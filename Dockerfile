ARG RUNTIME_IMAGE=eclipse-temurin:25.0.4_7-jre-noble@sha256:8c6736fa623090b057a5bbd36d42f90c9de4c7d2d4b6c285921a4f85ce65a445

FROM ${RUNTIME_IMAGE} AS extractor
ARG JAR_FILE
ARG SOURCE_DATE_EPOCH
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted \
    && find extracted -exec touch --no-dereference --date="@${SOURCE_DATE_EPOCH}" {} +

FROM ${RUNTIME_IMAGE}
ARG VCS_REF
ARG SOURCE_DATE_EPOCH
LABEL org.opencontainers.image.source="https://github.com/avgarcia/vgrunning" \
      org.opencontainers.image.revision="${VCS_REF}"
ENV SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH}
WORKDIR /workspace
COPY --from=extractor --chown=10001:10001 extracted/dependencies/ ./
COPY --from=extractor --chown=10001:10001 extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=10001:10001 extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=10001:10001 extracted/application/ ./
USER 10001:10001
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
