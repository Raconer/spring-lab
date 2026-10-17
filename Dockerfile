# 모든 모듈이 같이 쓰는 이미지 빌드 파일이다. 모듈은 build args의 MODULE로 고른다
# 빌드 컨텍스트는 프로젝트 루트다 (core도 같이 빌드해야 해서)
FROM eclipse-temurin:25-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:25-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
