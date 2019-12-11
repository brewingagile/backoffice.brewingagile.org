# === BUILD
FROM openjdk:12 as builder
WORKDIR /work
ADD  . ./
USER root
RUN ./gradlew clean build

# === RUN
FROM openjdk:12-alpine as runner
COPY --from=builder /work/application/build/libs/application-all.jar /
CMD [ \
  "java", \
  "-XX:InitialRAMPercentage=50", \
  "-XX:MaxRAMPercentage=85", \
  "-jar", "/application-all.jar", \
  "--config-file=/config/config.properties", \
  "--secret-file=/secret/secret.properties" \
]