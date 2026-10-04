FROM akrosinc/reveal-gdal-java:1.0.0

WORKDIR /

COPY reveal-server.jar /reveal-server.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.library.path=/opt/gdal/lib/jni:/opt/gdal/lib", \
    "-XX:+UseG1GC", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar"]

CMD ["/reveal-server.jar", "--spring.config.location=file:/application.properties"]