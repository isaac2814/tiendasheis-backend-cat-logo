# syntax=docker/dockerfile:1
# ============================================================================
# Imagen de producción del Backend Catálogo TiendaSheis
# Multi-etapa: build con Maven + JDK 25 y runtime con JRE 25 ligero (no root).
# ============================================================================

# ---- Etapa 1: compilación (Maven + JDK 25) ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# 1) Cache de dependencias: se copia solo el POM primero para reutilizar la capa.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Código fuente y empaquetado (los tests corren en CI, no en la imagen).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Etapa 2: runtime (JRE 25 ligero, usuario no root) ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Límite de memoria relativo al contenedor y puerto por defecto del contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0" \
    PORT=8080

# Usuario sin privilegios.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /build/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
