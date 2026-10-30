FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
# fuentes y librerias graficas: las usa la generacion de PDF y de codigos QR / barras
RUN apk add --no-cache fontconfig freetype ttf-dejavu
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV DOCUMENTS_DIR=/app/documents
RUN mkdir -p /app/documents
EXPOSE 8084
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
