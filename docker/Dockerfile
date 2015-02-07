FROM debian:7.4
MAINTAINER jvermillard@gmail.com

RUN apt-get update
RUN apt-get install -y openjdk-7-jre-headless wget

RUN mkdir /opt/leshan
RUN wget https://drone.io/github.com/jvermillard/leshan/files/leshan.jar -O /opt/leshan/leshan.jar
ENTRYPOINT cd /opt/leshan && java -jar ./leshan.jar
