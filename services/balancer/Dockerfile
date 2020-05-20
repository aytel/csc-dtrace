FROM gradle:latest
#FROM openjdk:15-jdk-slim
WORKDIR /app
COPY ./build.gradle /app/
RUN gradle getDeps
COPY . /app
RUN gradle build
ARG PROP_FILE
COPY ${PROP_FILE} /app/balancer.properties
CMD gradle run
#RUN ./gradlew build
#CMD ./gradlew run