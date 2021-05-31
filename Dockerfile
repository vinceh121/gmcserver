FROM maven:3.8.1-jdk-11-openj9 AS build-base
RUN \
	apt-get update && \
	apt-get install -y make

FROM build-base AS builder
WORKDIR /build
COPY gmcserver-server/ .
# dirty hack for maven git plugin
COPY .git/ ./.git
RUN PATH_CONFIG=./config.properties \
	PATH_CONFIG_VERTX=./vertx.json \
	PATH_CONFIG_MAIL=./mail.json \
	PATH_MAIL_TEMPLATES=./gmcserver-email/ \
	make -e

FROM node:16.2.0-buster AS builder-email
WORKDIR /build
COPY gmcserver-email/ .
RUN npm install -g pnpm && \
	make

FROM adoptopenjdk/openjdk11:jre-11.0.11_9-debian AS runtime-base
WORKDIR /build
COPY --from=builder /build/target/gmcserver*jar-with-dependencies.jar ./gmcserver.jar
COPY --from=builder /build/mail.json /build/vertx.json ./
COPY --from=builder /build/config.example.properties ./config.properties
COPY --from=builder-email /build/out/ ./gmcserver-email/
EXPOSE 8080

CMD ["java", "-jar", "gmcserver.jar"]
