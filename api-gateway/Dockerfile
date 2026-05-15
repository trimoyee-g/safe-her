FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S safeher && adduser -S safeher -G safeher
COPY target/api-gateway-*.jar app.jar
RUN chown safeher:safeher app.jar
USER safeher
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dreactor.netty.http.server.accessLogEnabled=true", \
  "-jar", "app.jar"]
