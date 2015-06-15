FROM java:8-jre
MAINTAINER roymeissn@gmail.com

# Add project files and NewRelic Agent
ADD files /
ADD newrelic.zip /opt/docker/

WORKDIR /opt/docker

# Install NewRelic Agent
RUN unzip newrelic.zip
ADD license /opt/docker/newrelic/
RUN cd newrelic && sed -i -e "s/My Application/Buildings MicroService/g" newrelic.yml

# Production settings
EXPOSE 9000
ENTRYPOINT ["./bin/building-microservice", "-J-javaagent:newrelic/newrelic.jar"]
CMD []
