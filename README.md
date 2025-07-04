# CAMARA Quality on Demand (QoD) API - High-Performance Implementation

This project provides a production-grade, high-performance implementation of the CAMARA QoD API, architected to handle millions of simultaneous calls with low latency and high security.

## Architecture

- **Reactive Services**: Built with Spring WebFlux for non-blocking I/O.
- **Asynchronous Processing**: Uses Apache Kafka to decouple API requests from backend network processing.
- **Distributed Caching**: Leverages Redis for fast, scalable session state management.
- **Secure & Scalable**: Designed for Kubernetes with NetworkPolicies, non-root containers, and horizontal scalability.

## Prerequisites

- Java 17+ & Maven 3.8+
- Docker & Docker Compose
- `kubectl` connected to a Kubernetes cluster
- An Ingress Controller (e.g., NGINX or Istio) and optionally `cert-manager` in the cluster.
- Push access to a container registry.

## How to Build and Deploy

1.  **Configure**:
    *   Update `REGISTRY` in `deploy.sh`.
    *   Update the domain `qod.your-domain.com` in `k8s/08-ingress.yaml`.
    *   Ensure you have a TLS secret named `qod-api-tls` in the `qod` namespace or configure `cert-manager`.

2.  **Make the script executable**:
    ```bash
    chmod +x deploy.sh
    ```

3.  **Run the deployment script**:
    ```bash
    ./deploy.sh
    ```
    This script will build the app, package it into a Docker container, push it to your registry, and apply all Kubernetes manifests.

## Testing the Deployed API

To test, you need a valid JWT access token.

```bash
# Set environment variables
export TOKEN="<your_jwt_access_token>"
export API_URL="https://qod.your-domain.com" # Use your actual domain

# 1. Create a Session Request (asynchronous)
# The server will respond immediately with 202 Accepted
CREATE_RESPONSE=$(curl -s -X POST "${API_URL}/qod/v1/sessions" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "duration": 3600,
    "device": { "ipv4Address": { "publicAddress": "1.2.3.4" }},
    "applicationServer": { "ipv4Address": "8.8.8.8" },
    "qosProfile": "QOS_L"
  }')

echo "Create Response: $CREATE_RESPONSE"
export SESSION_ID=$(echo $CREATE_RESPONSE | jq -r .sessionId)
echo "Session ID: $SESSION_ID"

# 2. Poll for Session Status
# Wait a moment for the Kafka processor to update the status.
echo "Waiting 5 seconds for async processing..."
sleep 5

curl -X GET "${API_URL}/qod/v1/sessions/${SESSION_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq .

# The status should now be "AVAILABLE".

# 3. Delete the Session (asynchronous)
curl -X DELETE "${API_URL}/qod/v1/sessions/${SESSION_ID}" \
  -H "Authorization: Bearer ${TOKEN}"

echo "Deletion request sent."
