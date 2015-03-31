FROM dockerfile/java
MAINTAINER roymeissn@gmail.com
EXPOSE 9000
ADD files /
ADD newrelic.zip /opt/docker/
WORKDIR /opt/docker
RUN unzip newrelic.zip
ADD license /opt/docker/newrelic/
RUN cd newrelic && sed -i -e "s/<%= license_key %>/`cat license`/g" newrelic.yml && sed -i -e "s/My Application/Buildings MicroService/g" newrelic.yml
ENTRYPOINT ["./bin/building-microservice", "-J-javaagent:newrelic/newrelic.jar"]
CMD []
