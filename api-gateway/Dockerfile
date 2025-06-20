# Multi-stage build for Java microservice
FROM openjdk:21-jdk-slim as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Maven Wrapper がある場合はそれを使用、なければMavenを直接使用
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

FROM openjdk:21-jre-slim

# 非rootユーザーでアプリケーションを実行
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# ビルドステージからJARファイルをコピー
COPY --from=builder /app/target/*.jar app.jar

# アプリケーション用ディレクトリの所有者を変更
RUN chown -R appuser:appgroup /app

USER appuser

# JVMオプション設定
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# ヘルスチェック
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
