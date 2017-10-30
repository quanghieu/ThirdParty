FROM java:openjdk-8-jre
ENV SERVER_PREFIX /opt/ThirdParty

RUN mkdir -p $SERVER_PREFIX
ADD ThirdParty.jar $SERVER_PREFIX/
EXPOSE 5000

CMD /usr/bin/java -jar $SERVER_PREFIX/ThirdParty.jar

