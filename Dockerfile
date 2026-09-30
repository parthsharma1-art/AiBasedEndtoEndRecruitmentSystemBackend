FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

CMD ["java", "-javaagent:target/newrelic/newrelic.jar", "-jar", "target/AiBasedEndtoEndSystem-0.0.1-SNAPSHOT.jar"]