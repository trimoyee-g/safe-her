FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S safeher && adduser -S safeher -G safeher
COPY target/place-service-*.jar app.jar
RUN chown safeher:safeher app.jar
USER safeher
EXPOSE 8083
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
