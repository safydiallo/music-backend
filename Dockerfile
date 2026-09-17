# --- Étape 1 : build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

# --- Étape 2 : exécution ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

