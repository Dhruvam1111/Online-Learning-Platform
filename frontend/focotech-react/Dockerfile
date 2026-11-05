# ============================================================
# Stage 1 – BUILD
# Install dependencies and build the Vite production bundle.
# Node 22 LTS is used to match React 19 / Vite 8 requirements.
# ============================================================
FROM node:22-alpine AS builder

# Set working directory inside the build container
WORKDIR /app

# --- Dependency caching layer ---
# Copy package files first so Docker caches node_modules as a
# separate layer.  The layer is only invalidated when
# package.json or package-lock.json actually changes.
COPY package.json package-lock.json ./

# Install ALL dependencies (including devDependencies needed
# for the Vite build step).  --frozen-lockfile ensures the
# exact versions in package-lock.json are used.
RUN npm ci --frozen-lockfile

# --- Source copy & build ---
# Copy the rest of the source code then produce the
# production-optimised static bundle in /app/dist
COPY . .

# VITE_API_BASE_URL is intentionally left unset here.
# The api.js fallback  →  baseURL: '/api'  is used in production.
# Nginx (Stage 2) proxies all /api/* requests to the backend.
# This means the image is backend-URL-agnostic and the backend
# host:port can be changed at runtime via the BACKEND_URL env var
# passed to the container (used in nginx.conf).
RUN npm run build

# ============================================================
# Stage 2 – RUNTIME
# Serve the static dist/ output with Nginx.
# nginx:alpine is ~23 MB — far smaller than any Node image.
# ============================================================
FROM nginx:1.27-alpine

# Remove the default Nginx placeholder page
RUN rm -rf /usr/share/nginx/html/*

# Copy the production build from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our custom Nginx configuration as a template.
# The official nginx:alpine image automatically runs envsubst on
# any file in /etc/nginx/templates/ and writes the result to
# /etc/nginx/conf.d/ before Nginx starts, substituting any
# ${VAR} placeholders with runtime environment variables.
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Expose port 80 (Nginx default)
EXPOSE 80

# Nginx runs in the foreground so Docker can manage the process
CMD ["nginx", "-g", "daemon off;"]
