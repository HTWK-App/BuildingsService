FROM dockerfile/java
MAINTAINER roymeissn@gmail.com
EXPOSE 9000
ADD files /
ADD newrelic.zip /
RUN unzip newrelic.zip
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/building-microservice -J-javaagent:/newrelic/newrelic.jar"]
CMD []
