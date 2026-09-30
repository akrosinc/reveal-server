# Use the official GDAL 3.12.0 image
FROM ghcr.io/osgeo/gdal:ubuntu-small-3.12.0

# 1. Copy the pre-built JRE from the official Eclipse Temurin image
# This takes ~1-2 seconds and requires zero internet downloads during build
COPY --from=eclipse-temurin:11-jre-jammy /opt/java/openjdk /opt/java/openjdk

# 2. Add Java to the system PATH
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /
COPY revealserver*-SNAPSHOT.jar reveal-server.jar

# GDAL Paths for the OSGeo image
ENV LD_LIBRARY_PATH=/usr/lib
ENV GDAL_DATA=/usr/share/gdal
ENV PROJ_LIB=/usr/share/proj

EXPOSE 8080

ENTRYPOINT [ \
  "java", \
  "-Djava.library.path=/usr/lib", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar" \
]
CMD [ "reveal-server.jar", "--spring.config.location=file:/application.properties" ]