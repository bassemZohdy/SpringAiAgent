# Deployment Guide

This guide outlines recommended deployment options for Spring AI Agent, including Docker Compose for local/edge environments and a reference Kubernetes configuration for larger deployments.

## Prerequisites

* Java 21 and Maven (for building backend artifacts)
* Node.js 18+ (for building the Angular UI)
* Docker Engine 24+ and Docker Compose v2
* kubectl and a Kubernetes cluster (optional)

## 1. Docker Compose

The repository ships with a multi-container Compose file that builds and runs the Spring Boot API and Angular UI together.

```bash
# Build and start the stack
docker-compose up --build -d

# Follow logs
docker-compose logs -f spring-ai-agent

# Stop and remove containers
docker-compose down
```

### Services

| Service | Description | Ports |
| --- | --- | --- |
| `spring-ai-agent` | Spring Boot API with SSE streaming and provider integrations | `8080` |
| `ui` | Angular front-end served via Node | `4200` |

The Compose file enables container health checks, mounts a Maven cache volume, and forwards `OPENAI_API_KEY` and related environment variables. Adjust `OPENAI_BASE_URL`, `AI_MODEL`, and other settings via environment variables or an `.env` file.

### Updating Images

1. Build the backend JAR:
   ```bash
   ./mvnw -pl spring-ai-agent clean package
   ```
2. Build the UI:
   ```bash
   cd ui
   npm install
   npm run build
   cd ..
   ```
3. Rebuild containers:
   ```bash
   docker-compose build --no-cache
   docker-compose up -d
   ```

## 2. Standalone Containers

If you prefer publishing images to a registry:

```bash
# Backend
cd spring-ai-agent
./mvnw clean package
docker build -t your-registry/spring-ai-agent:latest .

# Front-end
cd ../ui
npm install
npm run build
docker build -t your-registry/spring-ai-agent-ui:latest .
```

Push the images and deploy them with your container orchestrator of choice.

## 3. Kubernetes Reference

Below is a minimal reference manifest for deploying both services to Kubernetes. Customize resource requests, autoscaling, and secrets as needed.

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: spring-ai
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-ai-agent
  namespace: spring-ai
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-ai-agent
  template:
    metadata:
      labels:
        app: spring-ai-agent
    spec:
      containers:
        - name: api
          image: your-registry/spring-ai-agent:latest
          ports:
            - containerPort: 8080
          env:
            - name: OPENAI_API_KEY
              valueFrom:
                secretKeyRef:
                  name: spring-ai-secrets
                  key: openaiApiKey
            - name: OPENAI_BASE_URL
              value: https://api.openai.com/v1
            - name: AI_MODEL
              value: gpt-3.5-turbo
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: spring-ai-agent
  namespace: spring-ai
spec:
  selector:
    app: spring-ai-agent
  ports:
    - name: http
      port: 80
      targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-ai-ui
  namespace: spring-ai
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-ai-ui
  template:
    metadata:
      labels:
        app: spring-ai-ui
    spec:
      containers:
        - name: ui
          image: your-registry/spring-ai-agent-ui:latest
          ports:
            - containerPort: 80
          env:
            - name: API_BASE_URL
              value: http://spring-ai-agent.spring-ai.svc.cluster.local
          readinessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: spring-ai-ui
  namespace: spring-ai
spec:
  type: LoadBalancer
  selector:
    app: spring-ai-ui
  ports:
    - name: http
      port: 80
      targetPort: 80
```

### Secrets & ConfigMaps

Create the `spring-ai-secrets` secret with your provider credentials:

```bash
kubectl create secret generic spring-ai-secrets \
  --namespace spring-ai \
  --from-literal=openaiApiKey=sk-your-key
```

Optionally create a ConfigMap for additional overrides (`OPENAI_BASE_URL`, `AI_MAX_HISTORY_TOKENS`, etc.) and mount it as environment variables.

## 4. Verification

After deployment:

1. Check API health: `curl http://<api-host>/actuator/health`
2. Hit the chat endpoint with a simple prompt.
3. Load the UI service and submit a message to confirm streaming works.

## 5. Observability & Scaling Tips

* Enable Spring Boot Actuator metrics and scrape them with Prometheus/Grafana for latency tracking.
* Horizontal scale the API deployment and front-end as needed; configure sticky sessions only if you introduce stateful storage.
* Consider deploying a Redis or database-backed thread store for production persistence.
* Use ingress controllers (NGINX, Traefik) or API gateways for TLS termination and rate limiting.
