# Angular UI Dockerfile
# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Build the Angular application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built Angular app
COPY --from=build /app/dist/spring-ai-agent-ui /usr/share/nginx/html

# Create non-root user
RUN addgroup -g 1000 nginx-user && \
    adduser -D -u 1000 -G nginx-user nginx-user && \
    chown -R nginx-user:nginx-user /usr/share/nginx/html && \
    chown -R nginx-user:nginx-user /var/cache/nginx && \
    chown -R nginx-user:nginx-user /var/log/nginx && \
    chown -R nginx-user:nginx-user /etc/nginx/conf.d

# Switch to non-root user
USER nginx-user

EXPOSE 4200

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4200 || exit 1

CMD ["nginx", "-g", "daemon off;"]