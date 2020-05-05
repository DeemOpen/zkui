FROM maven:3-jdk-8 as build
WORKDIR /app

COPY pom.xml .

# Run maven build & remove artifacts to cache downloaded dependencies until pom.xml changes
RUN mvn clean package -Dmaven.main.skip -Dmaven.test.skip && rm -r target

# Copy all files with extensions (i.e. skip Dockerfile, Makefile)
COPY *.* ./

# Build for real
RUN mvn clean package -Dmaven.test.skip

FROM java:8

LABEL maintainer="Miguel Garcia Puyol <miguelpuyol@gmail.com>"

WORKDIR /var/app

COPY --from=build /app/target/zkui-*-jar-with-dependencies.jar /var/app/zkui.jar
ADD config.cfg /var/app/config.cfg
ADD bootstrap.sh /var/app/bootstrap.sh

ENTRYPOINT ["/var/app/bootstrap.sh"]

EXPOSE 9090
