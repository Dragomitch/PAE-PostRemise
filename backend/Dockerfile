# Multi-stage build for the Spring Boot backend, as a layered image: the dependencies (which change
# rarely) come first and the application classes (which change on every commit) last, so Docker
# rebuilds, pushes and pulls only the last layer when only the code changed.
# Layout and commands: "Efficient container images" in the Spring Boot reference (jarmode=tools).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Splits the executable jar into dependencies/, spring-boot-loader/, snapshot-dependencies/ and
# application/ (the order of the layers: from the least to the most frequently changed)
FROM eclipse-temurin:21-jre AS extract
WORKDIR /extract
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
WORKDIR /app
# One COPY, one image layer; the layers merge into /app: app.jar (the application classes only)
# and lib/ (the dependency jars it references)
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
