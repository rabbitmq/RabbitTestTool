FROM maven:3.8-jdk-11 as builder

COPY . /workspace

WORKDIR /workspace

RUN mvn package

FROM openjdk:11.0-jre

WORKDIR /rabbittesttool

COPY --from=builder \
    /workspace/target/rabbittesttool-1.1-SNAPSHOT-jar-with-dependencies.jar \
    rabbittesttool.jar

COPY topologies topologies
COPY policies policies

ENTRYPOINT ["java", "-jar", "rabbittesttool.jar"]