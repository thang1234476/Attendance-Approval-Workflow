# Dockerfile tối ưu
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Lấy file JAR mà GitHub vừa đẩy sang
COPY attendance-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
