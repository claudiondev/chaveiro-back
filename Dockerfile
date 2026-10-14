# Build em JDK 21 (não 17): o openpdf-3.0.5.jar foi compilado com Java 21 (class file major
# version 65) e o javac do JDK 17 não consegue nem ler esse .class ("bad class file"), só
# compilar. O bytecode final continua Java 17 — o pom.xml mantém java.version=17, que o
# Spring Boot Parent usa como --release — por isso o runtime abaixo segue em JRE 17.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
