// lab-redis의 couponfirstcome(쿠폰 선착순 발급)에 부하를 준다
// 옵션은 nGrinder 테스트 설정의 Parameter(상세 설정 보기 안)에 key.value를 쉼표로 이어 적는다
//   예: method.lua / method.lock,quantity.20 / method.lua,sameUser.true
//   nGrinder는 Parameter에 영문·숫자·. , | _만 50자까지 허용해서 = 대신 .으로 key와 value를 나눈다
//   method: naive, lua, incr, lock (기본 naive)
//   quantity: 쿠폰 수량 (기본 10)
//   sameUser: true면 모든 요청이 user-1로 발급을 요청한다 (기본 false: 요청마다 다른 사용자)
//   비운 key는 맨 위 DEFAULTS 값을 쓴다. 모르는 key나 method면 테스트를 시작하지 않는다
// 쿠폰 ID는 실행마다 새로 만든다 (load-(시작 시각)). 이전 결과와 섞이지 않는다
// 대상 주소는 TARGET이다. agent는 spring-boot-lab 네트워크에서 서비스 이름으로 찾는다
// 쿠폰 등록과 검증은 프로세스 안에서 한 번씩 한다. 그래서 agent 1개, 프로세스 1개로 실행한다 (가상 사용자 수 = 스레드 수)
import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.assertTrue

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.concurrent.atomic.AtomicLong
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.AfterProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.message.BasicHeader
import org.junit.Test
import org.junit.runner.RunWith
import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPRequestControl
import org.ngrinder.http.HTTPResponse

@RunWith(GrinderRunner)
class TestRunner {

    // Parameter에 없는 key의 기본값
    static final Map<String, String> DEFAULTS = [method: "naive", quantity: "10", sameUser: "false"]
    static final List<String> METHODS = ["naive", "lua", "incr", "lock"]

    public static final String TARGET = "http://lab-redis:8080"

    public static GTest test
    public static HTTPRequest request
    public static List<Header> headers = [new BasicHeader("Content-Type", "application/json")]

    public static String method
    public static long quantity
    public static boolean sameUser
    public static String couponId

    // 201 응답 수. 검증에서 실제 발급 수와 비교한다
    public static AtomicLong created = new AtomicLong()

    @BeforeProcess
    public static void beforeProcess() {
        HTTPRequestControl.setConnectionTimeout(300000)
        test = new GTest(1, "redis.couponfirstcome")
        request = new HTTPRequest()

        Map<String, String> options = DEFAULTS + parse(System.getProperty("param", ""))
        method = options.method
        quantity = options.quantity as long
        sameUser = options.sameUser == "true"
        assertTrue("모르는 method: ${method} (${METHODS} 중 하나)", method in METHODS)
        couponId = "load-${System.currentTimeMillis()}"

        HTTPResponse response = request.PUT("${TARGET}/redis/coupons/${couponId}",
                JsonOutput.toJson([quantity: quantity]).getBytes("UTF-8"), headers)
        assertTrue("쿠폰 등록 실패: ${response.statusCode}", response.statusCode in [200, 201, 204])
        grinder.logger.info("쿠폰 등록: couponId={}, quantity={}, method={}, sameUser={}", couponId, quantity, method, sameUser)
    }

    @BeforeThread
    public void beforeThread() {
        test.record(this, "test")
        grinder.statistics.delayReports = true
    }

    // 발급 요청 하나. 201(발급)과 409(중복·소진)는 정상 응답으로 본다
    @Test
    public void test() {
        HTTPResponse response = request.POST("${TARGET}/redis/coupons/${couponId}/issues?method=${method}",
                JsonOutput.toJson([userId: userId()]).getBytes("UTF-8"), headers)
        if (response.statusCode == 201) {
            created.incrementAndGet()
        }
        assertTrue("예상하지 못한 응답: ${response.statusCode}", response.statusCode in [201, 409])
    }

    // 부하 후 쿠폰 상태를 조회해서 규칙을 확인하고 로그로 남긴다 (테스트 상세의 로그에서 본다)
    @AfterProcess
    public static void afterProcess() {
        HTTPResponse response = request.GET("${TARGET}/redis/coupons/${couponId}", [], headers)
        def coupon = new JsonSlurper().parseText(response.bodyText)
        long issued = coupon.issuedCount as long
        // 실행 횟수 대신 실행 시간으로 돌리면 grinder.runs가 0이다. 이때는 요청이 수량보다 많다고 본다
        int runs = grinder.properties.getInt("grinder.runs", 0)
        long users = sameUser ? 1 : (runs > 0 ? grinder.properties.getInt("grinder.threads", 1) * runs : Long.MAX_VALUE)
        long expected = Math.min(quantity, users)

        check("발급 수가 수량을 넘지 않는다", "<= ${quantity}", issued, issued <= quantity)
        check("201 응답 수와 실제 발급 수가 같다", "${issued}", created.get(), created.get() == issued)
        check("받을 수 있는 만큼 모두 발급된다", "${expected}", issued, issued == expected)
    }

    private static void check(String name, String expected, long actual, boolean passed) {
        grinder.logger.info("[검증 {}] {} (기대 {}, 실제 {})", passed ? "통과" : "실패", name, expected, actual)
    }

    private String userId() {
        if (sameUser) {
            return "user-1"
        }
        return "user-${grinder.agentNumber}-${grinder.processNumber}-${grinder.threadNumber}-${grinder.runNumber}"
    }

    // "method.lua,quantity.20" → [method: "lua", quantity: "20"]
    private static Map<String, String> parse(String raw) {
        Map<String, String> options = [:]
        raw.split(",").findAll { it }.each { String pair ->
            List<String> keyValue = pair.split("\\.", 2) as List
            assertTrue("Parameter 형식 오류: ${pair} (key.value)", keyValue.size() == 2)
            assertTrue("모르는 key: ${keyValue[0]} (${DEFAULTS.keySet()} 중 하나)", DEFAULTS.containsKey(keyValue[0]))
            options[keyValue[0]] = keyValue[1]
        }
        return options
    }
}
