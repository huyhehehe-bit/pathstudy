# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/pathstudy.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
