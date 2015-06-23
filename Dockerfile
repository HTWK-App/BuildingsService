FROM java:8-jre
MAINTAINER roymeissn@gmail.com

# Add project files and NewRelic Agent
ADD files /
ADD newrelic.zip /opt/docker/

WORKDIR /opt/docker

# Install NewRelic Agent
RUN unzip newrelic.zip
RUN cd newrelic && sed -i -e "s/My Application/Buildings MicroService/g" newrelic.yml
RUN chmod +x ./bin/building-microservice

# Production settings
EXPOSE 9000
ENTRYPOINT ["./bin/building-microservice", "-J-javaagent:/opt/docker/newrelic/newrelic.jar"]
CMD []
