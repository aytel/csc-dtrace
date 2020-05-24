FROM gradle:latest
WORKDIR /app
COPY ./build.gradle /app/
RUN gradle getDeps
COPY . /app
RUN gradle build
CMD gradle run