# ETAPA 1: Compilação (Build)
# Aqui usamos uma imagem que já tem o Maven e o JDK 17 instalados
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa as dependências (otimiza o cache do Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte e gera o .jar
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Execução (Runtime)
# Agora usamos apenas o JRE (mais leve) para rodar a aplicação
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia apenas o arquivo .jar que foi gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que o Spring Boot usa (definida no seu application.properties como 8080)
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]