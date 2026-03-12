FROM eclipse-temurin:25-jdk-alpine

# 1. Crear usuario y grupo de forma explícita para Alpine (Seguridad AWS)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 2. Definir el nombre del archivo exacto de tu captura
ARG JAR_FILE=applications/app-service/build/libs/BTG_Pactual_Backend_Challenge_Yerid.jar

VOLUME /tmp

# 3. Copiar desde la ruta del proyecto hacia la raíz del contenedor
COPY ${JAR_FILE} /BTG_Pactual_Backend_Challenge_Yerid.jar

# 4. Configuración de JAVA_OPTS optimizada para AWS (Fargate/ECS)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 5. Usar el usuario seguro
USER appuser

# 6. Entrypoint con la ruta y nombre exactos que solicitaste
ENTRYPOINT [ "/bin/sh", "-c", "java $JAVA_OPTS -jar /BTG_Pactual_Backend_Challenge_Yerid.jar" ]