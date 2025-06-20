# Multi-stage build for Java microservice
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

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
  CMD curl -f http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
