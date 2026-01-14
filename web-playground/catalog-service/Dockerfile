FROM amazoncorretto:24.0.2-alpine3.22

LABEL authors="Volodymyr Shpynta"
LABEL module="catalog-service"

RUN mkdir /app
COPY build/libs/catalog-service-1.0-SNAPSHOT.jar /app/
CMD exec java -jar /app/catalog-service-1.0-SNAPSHOT.jar
