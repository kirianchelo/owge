FROM tomcat:8-jre8
MAINTAINER Kevin Guanche Darias

EXPOSE 8081
ARG warHolderDirectory
ARG OWGE_ADMIN_WAR_FILENAME
ARG OWGE_REST_WAR_FILENAME
ARG OWGE_UNIVERSE_ID

ENV TERM xterm

COPY config /tmp/config
COPY $warHolderDirectory /tmp/war_location_folder

RUN cat /tmp/config/tomcat/context.xml | sed s/€database/"sgalactica_java_$OWGE_UNIVERSE_ID"/g > /usr/local/tomcat/conf/context.xml
RUN echo VALUES ARE: $OWGE_ADMIN_WAR_FILENAME , $OWGE_REST_WAR_FILENAME;
RUN ls /tmp/war_location_folder;
RUN mv /tmp/config/tomcat/server.xml /usr/local/tomcat/conf/server.xml
RUN rm -r /usr/local/tomcat/webapps/ROOT
RUN mkdir -p /usr/local/tomcat/webapps/ROOT
RUN mv /tmp/war_location_folder/$OWGE_ADMIN_WAR_FILENAME /usr/local/tomcat/webapps/admin.war
RUN mv /tmp/war_location_folder/$OWGE_REST_WAR_FILENAME /usr/local/tomcat/webapps/game_api.war
#RUN apt update && apt install -y nano

