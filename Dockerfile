FROM eclipse-temurin:17

ARG app
WORKDIR /opt/${app}

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.24.0/opentelemetry-javaagent.jar /opt/${app}/opentelemetry-javaagent.jar
RUN chmod +r /opt/${app}/opentelemetry-javaagent.jar

# Enable metric exporting
ENV OTEL_METRICS_EXPORTER="otlp"

RUN addgroup -gid 1001 jdp && \
    adduser --uid 1001 --gid 1001 --disabled-password --gecos "" jdp

USER 1001

COPY build/libs/${app}-1.0.0.jar service.jar

ENV OTEL_JAR=/opt/${app}/opentelemetry-javaagent.jar

CMD if test "$OTEL_EXPORTER_OTLP_ENDPOINT" = "" ; then \
      java $JAVA_OPTIONS -jar service.jar ; \
    else \
      java $JAVA_OPTIONS -javaagent:$OTEL_JAR -jar service.jar ; \
    fi;