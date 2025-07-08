# --- Stage 1: Build Frontend ---
FROM node:22-alpine AS frontend-build
WORKDIR /frontend

# Argumento para URL da API
ARG VITE_API_URL=http://10.113.134.159:8083/api
ENV VITE_API_URL=$VITE_API_URL

COPY frontend/package*.json ./
RUN npm install
RUN npm install -g typescript
COPY frontend/ ./
RUN chmod -R +x node_modules/.bin
RUN npm run build

# --- Stage 2: Build Backend ---
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY target/*.jar app.jar

# --- Stage 3: Nginx para servir o frontend ---
FROM nginx:alpine AS frontend-prod
COPY --from=frontend-build /frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

# --- Stage 4: Backend runtime ---
FROM eclipse-temurin:21-jdk-alpine AS backend-prod
WORKDIR /app
COPY --from=backend-build /app/app.jar ./app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

# Instruções:
# - Para rodar o frontend: use a imagem 'frontend-prod' (Nginx na porta 80)
# - Para rodar o backend: use a imagem 'backend-prod' (Java na porta 8083)
# - Recomenda-se usar docker-compose para orquestrar ambos 