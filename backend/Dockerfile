FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# The wrapper verifies the ZIP distribution. With no unzip it downloads a tarball.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/main/ src/main/
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system codelearn && useradd --system --gid codelearn codelearn
COPY --from=build --chown=codelearn:codelearn /workspace/target/*.jar app.jar
USER codelearn

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
