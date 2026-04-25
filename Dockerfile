# syntax=docker/dockerfile:1

# --- build stage ---
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY lib ./lib
RUN mvn dependency:resolve -q
COPY src ./src
RUN mvn package -DskipTests -q

# --- runtime stage ---
# Tomcat 8.5 = Servlet 3.1, compatible with WebObjects 5.4.3
FROM tomcat:8.5-jdk11
ARG GIT_REVISION=unknown
RUN rm -rf /usr/local/tomcat/webapps/*

# .woa filesystem bundle (WOROOT mode — see CLAUDE.md for details)
COPY src/main/resources/Info.plist        /opt/woapps/HelloWorld.woa/Contents/
COPY src/main/resources/Properties        /opt/woapps/HelloWorld.woa/Contents/Resources/
COPY src/main/resources/Main.wo/          /opt/woapps/HelloWorld.woa/Contents/Resources/Main.wo/
COPY src/main/resources/GuestbookPage.wo/ /opt/woapps/HelloWorld.woa/Contents/Resources/GuestbookPage.wo/
COPY src/main/resources/StatsPage.wo/     /opt/woapps/HelloWorld.woa/Contents/Resources/StatsPage.wo/
COPY --from=build /app/target/bundles/HelloWorld.jar \
                  /opt/woapps/HelloWorld.woa/Contents/Resources/Java/
RUN echo "$GIT_REVISION" > /opt/woapps/HelloWorld.woa/REVISION

RUN mkdir -p /data/guestbook

COPY --from=build /app/target/HelloWorld-1.0.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
