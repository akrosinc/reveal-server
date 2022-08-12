FROM openjdk:11

COPY revealserver*-SNAPSHOT.jar reveal-server.jar

WORKDIR /

EXPOSE 8080


#ENTRYPOINT [ "/usr/local/openjdk-11/bin/java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:+UnlockExperimentalVMOptions", "-XX:MaxRAMFraction=1", "-jar" ]
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "/reveal-server.jar", "--spring.config.location=file:/application.properties" ]
