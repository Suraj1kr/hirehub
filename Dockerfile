FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
RUN groupadd --system hirehub && useradd --system --gid hirehub hirehub
WORKDIR /app
COPY --from=build /build/target/hirehub-1.0.0.jar app.jar
USER hirehub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
