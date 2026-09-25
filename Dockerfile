# syntax=docker/dockerfile:1

FROM node:24-alpine AS frontend-build
WORKDIR /src/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine AS frontend
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /src/frontend/dist /usr/share/nginx/html
EXPOSE 8080

FROM gradle:9.7.1-jdk21-alpine AS backend-build
WORKDIR /src/backend
COPY backend/ ./
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS backend
WORKDIR /app
COPY --from=backend-build /src/backend/build/libs/ /tmp/build/
RUN set -eux; \
    app_jar="$(find /tmp/build -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"; \
    test -n "$app_jar"; \
    cp "$app_jar" /app/app.jar; \
    rm -rf /tmp/build
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM gradle:9.7.1-jdk21-alpine AS monitoring-build
WORKDIR /src/monitoring-service
COPY monitoring-service/ ./
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS monitoring-service
WORKDIR /app
COPY --from=monitoring-build /src/monitoring-service/build/libs/ /tmp/build/
RUN set -eux; \
    app_jar="$(find /tmp/build -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"; \
    test -n "$app_jar"; \
    cp "$app_jar" /app/app.jar; \
    rm -rf /tmp/build
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
