FROM openjdk:11

COPY reveal-server.jar /

EXPOSE 8080

CMD ["/usr/local/openjdk-11/bin/java","-XX:-HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=/tmp/java_pid.hprof", "-XX:MaxRAMPercentage=80.0", "-jar", "/reveal-server.jar", "--spring.config.location=file:/application.properties" ]
