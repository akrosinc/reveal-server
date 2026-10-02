# Official GDAL 3.12.0 image with Java bindings
FROM ghcr.io/osgeo/gdal:ubuntu-full-3.12.0

# Java 11 for Reveal Server
COPY --from=eclipse-temurin:11-jre-jammy /opt/java/openjdk /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# GDAL
ENV GDAL_DATA=/usr/share/gdal
ENV PROJ_LIB=/usr/share/proj

# IMPORTANT:
# GDAL native library + Java JNI library locations
ENV LD_LIBRARY_PATH=/usr/lib/jni:/usr/lib/x86_64-linux-gnu:/usr/lib:/usr/local/lib

WORKDIR /

COPY reveal-server.jar /reveal-server.jar

EXPOSE 8080

ENTRYPOINT [ \
  "java", \
  "-Djava.library.path=/usr/lib/jni:/usr/lib/x86_64-linux-gnu:/usr/lib:/usr/local/lib", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar" \
]

CMD [ \
  "/reveal-server.jar", \
  "--spring.config.location=file:/application.properties" \
]
