FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S safeher && adduser -S safeher -G safeher
COPY target/eureka-server-*.jar app.jar
RUN chown safeher:safeher app.jar
USER safeher
EXPOSE 8761
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-jar", "app.jar"]
