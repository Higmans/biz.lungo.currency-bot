FROM openjdk:22-jdk-slim
COPY release/currency-bot.jar /app.jar
CMD ["java", "-jar", "/app.jar"]