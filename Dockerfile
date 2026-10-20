# Build stage
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
EXPOSE 8080
# No debugger by default. To enable remote debugging, set e.g.
# JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENTRYPOINT ["java","-jar","/app/app.jar"]
