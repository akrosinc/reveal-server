FROM akrosinc/reveal-gdal-java:1.0.0

WORKDIR /app

COPY build/libs/revealserver-0.0.1-SNAPSHOT.jar /app/reveal-server.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.library.path=/opt/gdal/lib/jni:/opt/gdal/lib", \
    "-XX:+UseG1GC", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", \
    "/app/reveal-server.jar"]

CMD ["--spring.profiles.active=local", \
     "--spring.config.additional-location=file:/config/application-local.properties"]