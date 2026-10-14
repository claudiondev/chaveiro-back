# openpdf-3.0.5.jar foi compilado com Java 21 (class file major version 65) — tanto build quanto
# runtime precisam ser JDK/JRE 21, não só o build. Com runtime em JRE 17, o jar sobe, o Spring
# começa a inicializar e só quebra na hora de instanciar o FechamentoPdfService:
# UnsupportedClassVersionError em org.openpdf.text.pdf.draw.DrawInterface ("class file version
# 65.0 ... este runtime só reconhece até 61.0"). O pom.xml mantém java.version=17 (nosso próprio
# bytecode via --release continua 17), mas isso não rebaixa bytecode de dependência já compilada.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
