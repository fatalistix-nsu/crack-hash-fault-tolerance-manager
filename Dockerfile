FROM gradle:latest AS common

ENV HOME=/home/gradle
ENV GRADLE_USER_HOME=$HOME/cache_home
ENV APP=$HOME/app

FROM common AS cache

RUN mkdir -p $HOME
RUN mkdir -p $GRADLE_USER_HOME
RUN mkdir -p $APP

COPY build.gradle.* gradle.properties $APP
COPY gradle $APP/gradle

WORKDIR $APP

RUN gradle clean build -i --stacktrace

FROM common AS build

ENV SRC=/usr/src/app/

COPY --from=cache $GRADLE_USER_HOME $HOME/.gradle
COPY . $SRC
WORKDIR $SRC
COPY --chown=gradle:gradle . $HOME/src
WORKDIR $HOME/src

RUN gradle buildFatJar --no-daemon

FROM amazoncorretto:21 AS runtime
EXPOSE 6969
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/crack-hash-manager.jar
ENTRYPOINT ["java","-jar","/app/crack-hash-manager.jar"]
