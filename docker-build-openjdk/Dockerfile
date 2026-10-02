FROM ghcr.io/osgeo/gdal:ubuntu-full-3.12.0

# Java 11
COPY --from=eclipse-temurin:11-jre-jammy /opt/java/openjdk /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /

COPY reveal-server.jar /reveal-server.jar

# GDAL
ENV GDAL_DATA=/usr/share/gdal
ENV PROJ_LIB=/usr/share/proj

# IMPORTANT: actual GDAL JNI location
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu/jni:/usr/lib/x86_64-linux-gnu:/usr/lib:/usr/local/lib

EXPOSE 8080

ENTRYPOINT [
  "java",
  "-Djava.library.path=/usr/lib/x86_64-linux-gnu/jni:/usr/lib/x86_64-linux-gnu:/usr/lib:/usr/local/lib",
  "-XX:+UseG1GC",
  "-XX:MaxRAMPercentage=75.0",
  "-jar"
]

CMD [
  "/reveal-server.jar",
  "--spring.config.location=file:/application.properties"
]
