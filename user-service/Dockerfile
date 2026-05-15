FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Add non-root user
RUN addgroup -S safeher && adduser -S safeher -G safeher

COPY target/user-service-*.jar app.jar

RUN chown safeher:safeher app.jar

USER safeher

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
