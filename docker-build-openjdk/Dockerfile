FROM openjdk:11

RUN mkdir /work

COPY revealserver*-SNAPSHOT.jar /work

RUN ls /work

EXPOSE 8080


#ENTRYPOINT [ "/usr/local/openjdk-11/bin/java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:+UnlockExperimentalVMOptions", "-XX:MaxRAMFraction=1", "-jar" ]
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "/work/reveal-server.jar", "--spring.config.location=file:/application.properties" ]
