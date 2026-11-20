FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY .mvn ./.mvn
COPY mvnw mvnw
COPY pom.xml .
COPY frontend ./frontend
COPY src ./src
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package \
    && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN groupadd --system app \
    && useradd --system --uid 10001 --gid app --create-home app \
    && mkdir /data \
    && chown app:app /data
COPY --from=build --chown=app:app /workspace/app.jar ./app.jar

EXPOSE 8080
VOLUME ["/data"]
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
