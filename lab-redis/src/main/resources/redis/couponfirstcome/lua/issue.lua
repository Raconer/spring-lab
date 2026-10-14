-- 선착순 쿠폰 발급. 확인과 발급을 한 번에 실행해서 동시 요청에도 수량을 넘기지 않는다
-- KEYS[1]: 쿠폰 수량 키, KEYS[2]: 발급받은 사용자 Set 키, ARGV[1]: 사용자 ID
-- 반환: 0 발급, 1 이미 발급, 2 수량 소진, 3 쿠폰 없음
local quantity = redis.call('GET', KEYS[1])
if not quantity then
    return 3
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1
end
if redis.call('SCARD', KEYS[2]) >= tonumber(quantity) then
    return 2
end
redis.call('SADD', KEYS[2], ARGV[1])
return 0
