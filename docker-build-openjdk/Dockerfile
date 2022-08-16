FROM openjdk:11

WORKDIR /

COPY reveal-server.jar reveal-server.jar

EXPOSE 8080

#ENTRYPOINT [ "/usr/local/openjdk-11/bin/java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:+UnlockExperimentalVMOptions", "-XX:MaxRAMFraction=1", "-jar" ]
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "/reveal-server.jar", "--spring.config.location=file:/application.properties" ]
