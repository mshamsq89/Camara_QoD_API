Of course. Here is the final, comprehensive `README.md` that reflects the complete, working architecture you have built. This file serves as the perfect entry point and documentation for your project.

---

# CAMARA Quality on Demand (QoD) API - High-Performance Implementation

This project provides a production-grade, high-performance implementation of the CAMARA Quality on Demand (QoD) API, architected to handle millions of simultaneous calls with low latency and high security. It is designed for and deployed to Kubernetes.

The architecture uses a modern, reactive stack to achieve scalability and resilience.

### Architecture Overview

-   **Reactive Services**: The core API is built with **Spring WebFlux** for a non-blocking, event-loop-based I/O model, allowing it to handle a massive number of concurrent connections efficiently.
-   **Asynchronous Processing**: The system uses **Apache Kafka** to decouple the initial API request from the backend network processing. The API responds instantly (`201 Created` with a `REQUESTED` status), while a separate processor service handles the request asynchronously.
-   **Distributed Caching**: **Redis** is used as a fast, scalable, in-memory data grid to store and share QoD session state between all microservice instances.
-   **Separation of Concerns**: The application runs in two modes, controlled by Spring profiles:
    -   `api`: The public-facing REST API pods, which handle incoming requests and produce messages to Kafka.
    -   `processor`: The internal worker pods, which consume messages from Kafka, interact with backend systems, and update the state in Redis.
-   **Secure & Scalable**: Designed for Kubernetes with robust `NetworkPolicy` for security, `initContainers` for dependency management, and horizontal scalability for all components.



## Features

-   **CAMARA QoD API**: Implements `POST /sessions`, `GET /sessions/{id}`, `DELETE /sessions/{id}`.
-   **OpenAPI 3.0**: API is defined using OpenAPI specifications with interactive Swagger UI.
-   **OAuth 2.0 Security**: Endpoints are secured using JWT-based token authentication (can be disabled for testing).
-   **5G Core Simulation**: Includes a stubbed `NefService` to simulate the interaction with a 5G core network.
-   **Containerized**: A secure, multi-stage `Dockerfile` creates a lean production image running as a non-root user.
-   **Fully Automated Deployment**: A `deploy.sh` script automates the entire process: building the app, pushing the Docker image, and deploying all Kubernetes resources in the correct order.
-   **Test-Ready**: Includes a `--no-auth` flag for the deployment script to disable all security for easy local testing and development.

## Prerequisites

-   Java 17+ & Maven 3.8+
-   Docker & Docker Compose
-   `kubectl` CLI connected to a Kubernetes cluster
-   An Ingress Controller (e.g., NGINX) installed in the cluster.
-   A container registry (the script is configured for GHCR, but can be adapted).

## How to Build and Deploy

This project provides a single script to automate the entire deployment process.

### Step 1: Set Up Credentials (for Secure Mode)

If you plan to deploy with security enabled (the default), you must provide credentials for pushing to your private container registry.

**Make your GitHub Package Public (Easiest Option for Testing)**
1.  Go to your repository on GitHub -> "Packages".
2.  Find the `qod-api` package and go to its settings.
3.  Change the visibility to **Public**. This allows your Kubernetes cluster to pull the image without any secrets.

**Or, Use a Private Package with Credentials**
1.  Create a GitHub Personal Access Token (PAT) with `read:packages` scope.
2.  Export your credentials as environment variables in your terminal:
    ```bash
    export GITHUB_USER="your-github-username"
    export GITHUB_PAT="ghp_YourPersonalAccessToken"
    ```

### Step 2: Configure Your Domain

You need to map a local domain name to your Kubernetes server's IP address.

1.  **Edit your local hosts file**:
    -   On Linux/macOS: `sudo nano /etc/hosts`
    -   On Windows: `C:\Windows\System32\drivers\etc\hosts`
2.  **Add the following line**, replacing `192.168.22.25` with your server's actual IP address:
    ```
    192.168.22.25   qod.local
    ```
3.  **Update the Ingress manifest**: Open `k8s/08-ingress.yaml` and ensure the `host` is set to `qod.local`.

### Step 3: Run the Deployment Script

The script is flexible. You can deploy in a secure mode or a "no-auth" mode for easy testing.

**Option A: Deploy for Testing (Recommended for first run)**

This deploys the application with all security **disabled**. You can `curl` the endpoints directly without any tokens.

```bash
# First, clean up any previous deployments
kubectl delete namespace qod --ignore-not-found=true

# Run the script with the --no-auth flag
./deploy.sh --no-auth
```

**Option B: Deploy in Secure Mode**

This deploys the full stack with OAuth2.0 security enabled. Make sure you have completed Step 1.

```bash
# First, clean up any previous deployments
kubectl delete namespace qod --ignore-not-found=true

# Make sure your GITHUB_USER and GITHUB_PAT are exported
# then run the script without any flags
./deploy.sh
```

The script will build the app, push the Docker image, and deploy all Kubernetes resources, waiting for each component to become healthy before proceeding to the next.

## How to Test the API

Once the deployment is complete, you can interact with the live API.

**1. Set Environment Variables**
```bash
# Use the domain you configured in your /etc/hosts file
export API_URL="http://qod.local"
```

**2. Create a New Session Request**
```bash
curl -X POST "${API_URL}/qod/v1/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "duration": 3600,
    "device": { "ipv4Address": { "publicAddress": "1.2.3.4" }},
    "applicationServer": { "ipv4Address": "8.8.8.8" },
    "qosProfile": "QOS_L"
  }'
```
This will immediately return a response with `"qosStatus": "REQUESTED"`. Note the `sessionId`.

**3. Watch the Processor Logs (Optional)**
In a separate terminal, you can see the asynchronous processing happen in real-time.
```bash
kubectl logs -f -n qod -l app=qod-processor
```
You will see a log message indicating that it consumed the request from Kafka.

**4. Get the Final Session Status**
Wait about 5-10 seconds for the processor to do its work, then request the session again using the ID from the first response.
```bash
# Replace with the actual session ID from your POST response
export SESSION_ID="<your-session-id>"

curl "${API_URL}/qod/v1/sessions/${SESSION_ID}" | jq .
```
The response will now show the updated status: **`"qosStatus": "AVAILABLE"`**. This confirms the entire end-to-end asynchronous workflow is successful.
