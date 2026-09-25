# ---- Build stage (Maven image has mvn built in; no wrapper needed) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -q -B -ntp clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/pathstudy.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
# Cờ JVM cho instance free (0.1 CPU / 512MB): khởi động nhanh hơn để Render kịp
# phát hiện port (tránh "Timed Out"), và giới hạn heap hợp RAM 512MB tránh OOM.
#  - UseSerialGC: GC nhẹ nhất cho heap nhỏ / CPU ít.
#  - TieredStopAtLevel=1: bỏ JIT tầng cao → warmup nhanh, startup ngắn.
#  - MaxRAMPercentage=65: heap ~330MB, chừa chỗ cho metaspace/thread.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:MaxRAMPercentage=65", "-Dspring.jmx.enabled=false", "-jar", "app.jar"]
