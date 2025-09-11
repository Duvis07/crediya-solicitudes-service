FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

COPY *.jar solicitudes-service.jar
RUN chown appuser:appuser solicitudes-service.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"

# Switch to non-root user
USER appuser

EXPOSE 8081

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS  -jar solicitudes-service.jar" ]
