FROM amazoncorretto:24.0.2-alpine3.22

LABEL authors="Volodymyr Shpynta"
LABEL module="third-party-service"

RUN mkdir /app
COPY build/libs/third-party-service-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/third-party-service-1.0-SNAPSHOT-all.jar

