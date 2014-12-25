FROM ifunsoftware/c3-next:latest

COPY c3.jar /opt/c3.jar

RUN mkdir -p /opt/c3-app && unzip -q /opt/c3.jar -d /opt/c3-app && rm /opt/c3.jar

RUN rm -rf /opt/virgo/repository/usr/* && rm -rf /opt/virgo/pickup/*

RUN cp /opt/c3-app/server/lib/* /opt/virgo/repository/usr/

RUN cp /opt/c3-app/server/pickup/c3.plan /opt/virgo/pickup/

RUN rm -rf /opt/c3-app
