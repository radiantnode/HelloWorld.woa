# syntax=docker/dockerfile:1

# --- build stage ---
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
# Resolve deps before copying source so this layer is cached independently
RUN mvn dependency:resolve -q
COPY src ./src
RUN mvn package -DskipTests -q

# --- runtime stage ---
# Tomcat 8.5 = Servlet 3.1, compatible with WebObjects 5.4.3
FROM tomcat:8.5-jdk11
RUN rm -rf /usr/local/tomcat/webapps/*

# Build the .woa filesystem bundle (WOROOT mode).
# WOServletAdaptor scans WOClasspath for a path containing ".woa/" to determine
# mainBundlePath. Placing HelloWorld.jar at Contents/Resources/Java/ gives us that
# path segment, and WO resolves templates from Contents/Resources/Main.wo/.
RUN mkdir -p /opt/woapps/HelloWorld.woa/Contents/Resources/Java \
             /opt/woapps/HelloWorld.woa/Contents/Resources/Main.wo \
             /opt/woapps/HelloWorld.woa/Contents/Resources/GuestbookPage.wo \
             /opt/woapps/HelloWorld.woa/Contents/Resources/StatsPage.wo \
             /data/guestbook

COPY src/main/resources/Info.plist \
     /opt/woapps/HelloWorld.woa/Contents/Info.plist
COPY src/main/resources/Properties \
     /opt/woapps/HelloWorld.woa/Contents/Resources/Properties
COPY src/main/resources/Main.wo/Main.html \
     /opt/woapps/HelloWorld.woa/Contents/Resources/Main.wo/Main.html
COPY src/main/resources/Main.wo/Main.wod \
     /opt/woapps/HelloWorld.woa/Contents/Resources/Main.wo/Main.wod
COPY --from=build /app/target/bundles/HelloWorld.jar \
     /opt/woapps/HelloWorld.woa/Contents/Resources/Java/HelloWorld.jar

COPY src/main/resources/GuestbookPage.wo/GuestbookPage.html \
     /opt/woapps/HelloWorld.woa/Contents/Resources/GuestbookPage.wo/GuestbookPage.html
COPY src/main/resources/GuestbookPage.wo/GuestbookPage.wod \
     /opt/woapps/HelloWorld.woa/Contents/Resources/GuestbookPage.wo/GuestbookPage.wod

COPY src/main/resources/StatsPage.wo/StatsPage.html \
     /opt/woapps/HelloWorld.woa/Contents/Resources/StatsPage.wo/StatsPage.html
COPY src/main/resources/StatsPage.wo/StatsPage.wod \
     /opt/woapps/HelloWorld.woa/Contents/Resources/StatsPage.wo/StatsPage.wod

COPY --from=build /app/target/HelloWorld-1.0.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
