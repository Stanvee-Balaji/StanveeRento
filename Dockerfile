# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven descriptor and sources
COPY pom.xml .
COPY src ./src

# Build the JAR (skip tests for faster builds)
RUN mvn clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
# ⚠️ Use your exact JAR name here:
COPY --from=build /app/target/StanveeRento-0.0.1-SNAPSHOT.jar app.jar

# Render will use PORT env var, default 8080 in Spring config
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
