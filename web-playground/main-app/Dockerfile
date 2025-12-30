FROM amazoncorretto:24.0.2-alpine3.22

LABEL authors="Volodymyr Shpynta"
LABEL module="main-app"

RUN mkdir /app
COPY build/libs/main-app-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/main-app-1.0-SNAPSHOT-all.jar

