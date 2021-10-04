FROM gcr.io/distroless/java:11

ADD application.properties /application.properties

COPY build/libs/reveal-server*-SNAPSHOT.jar reveal-server.jar

WORKDIR /

EXPOSE 8080

ENTRYPOINT [ "/usr/bin/java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:+UnlockExperimentalVMOptions", "-XX:MaxRAMFraction=1", "-jar" ]
CMD [ "/reveal-server.jar", "--spring.config.location=file:/application.properties" ]
