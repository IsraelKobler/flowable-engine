# base image
FROM maven:3.6.2-jdk-8 as builder

# set assets directory
RUN mkdir /usr/src/assets
RUN mkdir /usr/src/app
WORKDIR /usr/src/app

RUN java -version
RUN mvn -version

# copy source code
COPY ./ /usr/src/app

RUN mvn -Pdistro clean install -DskipTests
# build flowable-ui-idm && # copy flowable-ui-idm .war
RUN cd modules/flowable-ui-idm && mvn -T 1C clean install -DskipTests -Pdocker-deps && cp flowable-ui-idm-app/target/*.original ../../../assets/

# build flowable-ui-modeler && # copy flowable-ui-modeler .war
RUN cd modules/flowable-ui-modeler && mvn -T 1C clean install -DskipTests -Pdocker-deps && cp flowable-ui-modeler-app/target/*.original ../../../assets/

# build flowable-ui-task && # copy flowable-ui-task .war
RUN cd modules/flowable-ui-task && mvn -T 1C clean install -DskipTests -Pdocker-deps && cp flowable-ui-task-app/target/*.original ../../../assets/

# build flowable-ui-admin && # copy flowable-ui-admin .war
RUN cd modules/flowable-ui-admin && mvn -T 1C clean install -DskipTests -Pdocker-deps && cp flowable-ui-admin-app/target/*.original ../../../assets/


FROM adoptopenjdk/openjdk11:alpine-jre
ADD docker/all-in-one/wait-for-something.sh .

RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*
RUN addgroup tomcat && adduser -s /bin/false -G tomcat -h /opt/tomcat -D tomcat

RUN wget http://archive.apache.org/dist/tomcat/tomcat-9/v9.0.14/bin/apache-tomcat-9.0.14.tar.gz -O /tmp/tomcat.tar.gz
RUN cd /tmp && tar xvfz tomcat.tar.gz && cp -Rv /tmp/apache-tomcat-9.0.14/* /opt/tomcat/ && rm -Rf /tmp/apache-tomcat-9.0.14

COPY docker/all-in-one/context.xml /opt/tomcat/conf/context.xml

COPY --from=builder /usr/src/assets/flowable-idm.war.original /opt/tomcat/webapps/flowable-idm.war
COPY --from=builder /usr/src/assets/flowable-modeler.war.original /opt/tomcat/webapps/flowable-modeler.war
COPY --from=builder /usr/src/assets/flowable-task.war.original /opt/tomcat/webapps/flowable-task.war
COPY --from=builder /usr/src/assets/flowable-admin.war.original /opt/tomcat/webapps/flowable-admin.war

RUN cd /opt/tomcat && chgrp -R tomcat /opt/tomcat && chmod -R g+r conf && chmod g+x conf && chown -R tomcat webapps/ work/ temp/ logs/ \
    && chown tomcat /wait-for-something.sh && chmod +x /wait-for-something.sh

ENV CATALINA_HOME /opt/tomcat
ENV PATH $PATH:$CATALINA_HOME/bin
ENV JAVA_OPTS="-Xms512M -Xmx2G -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

WORKDIR /opt/tomcat

USER tomcat

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
