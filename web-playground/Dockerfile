FROM amazoncorretto:24.0.2-alpine3.22

LABEL authors="Volodymyr Shpynta"

RUN mkdir /app
COPY build/libs/web-playground-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/web-playground-1.0-SNAPSHOT-all.jar
