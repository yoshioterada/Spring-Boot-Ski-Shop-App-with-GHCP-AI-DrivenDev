FROM openjdk:21-jre-slim

LABEL maintainer="Azure SkiShop Team"
LABEL version="1.0.0"
LABEL description="Azure SkiShop Frontend Service"

# 作業ディレクトリを設定
WORKDIR /app

# アプリケーションJARファイルをコピー
COPY target/frontend-service-*.jar app.jar

# ポート8080を公開
EXPOSE 8080

# ヘルスチェック
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# アプリケーション実行
ENTRYPOINT ["java", "-jar", "app.jar"]

# 本番環境用のJVMオプション
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Spring Profile を本番に設定
ENV SPRING_PROFILES_ACTIVE=production
