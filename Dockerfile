FROM maven:3-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /workspace/target/*.jar /app/app.jar
EXPOSE 8080

CMD ["sh", "-c", "java ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar /app/app.jar"]
