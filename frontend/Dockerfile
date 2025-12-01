# ================================
# Etapa 1: build de React (Vite)
# ================================
FROM node:20 AS build

WORKDIR /app

# Copiamos solo package.json y package-lock para instalar deps
COPY package*.json ./
RUN npm install

# Copiamos el resto del código
COPY . .

# Backend que va a usar el frontend dentro de la red de Docker
# (por defecto backend-dev:8081 en la red ci-network)
ARG VITE_API_BASE=http://backend-dev:8081
ENV VITE_API_BASE=${VITE_API_BASE}

# Compilamos el frontend (genera la carpeta dist/)
RUN npm run build

# ================================
# Etapa 2: Nginx sirviendo estáticos
# ================================
FROM nginx:stable-alpine

# Borramos el default y metemos nuestra config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiamos los archivos compilados
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
