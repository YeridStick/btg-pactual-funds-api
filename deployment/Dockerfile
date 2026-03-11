FROM eclipse-temurin:25-jdk-alpine
VOLUME /tmp
COPY *.jar BTG_Pactual_Backend_Challenge_Yerid.jar
ENV JAVA_OPTS=" -Xshareclasses:name=cacheapp,cacheDir=/cache,nonfatal -XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
# Replace with a non-root user to avoid running the container with excessive privileges
USER appuser
ENTRYPOINT [ "/bin/sh", "-c", "/opt/java/openjdk/bin/java $JAVA_OPTS  -jar /BTG_Pactual_Backend_Challenge_Yerid.jar" ]
