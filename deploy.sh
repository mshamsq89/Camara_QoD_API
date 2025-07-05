#!/bin/bash
set -e

# --- Configuration ---
# NOTE: The image specified here MUST be public for this script to work.
REGISTRY="ghcr.io/mshamsq89"
IMAGE_NAME="qod-api"
IMAGE_TAG=$(git rev-parse --short HEAD)
K8S_NAMESPACE="qod"

echo "🚀 DEPLOYING IN 'NO-AUTH' TEST MODE 🚀"
echo "Deploying image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# --- Build & Push Steps ---
echo "✅ Step 1: Packaging the application..."
mvn clean package -DskipTests

echo "✅ Step 2: Building the Docker image..."
FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t "${FULL_IMAGE_NAME}" .
docker tag "${FULL_IMAGE_NAME}" "${REGISTRY}/${IMAGE_NAME}:latest"

echo "✅ Step 3: Pushing the public Docker image..."
# You still need to log in to push, even to a public repo
if [[ -z "$GITHUB_USER" || -z "$GITHUB_PAT" ]]; then
  echo "WARNING: GITHUB_USER and GITHUB_PAT are not set. Push may fail if not already logged in."
else
  echo "${GITHUB_PAT}" | docker login ghcr.io -u "${GITHUB_USER}" --password-stdin
fi
docker push "${FULL_IMAGE_NAME}"
docker push "${REGISTRY}/${IMAGE_NAME}:latest"

# --- Kubernetes Deployment Steps ---
echo "✅ Step 4: Deploying simplified architecture to Kubernetes..."

echo "--> Applying namespace and configurations..."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/03-qod-api-configmap.yaml

echo "--> Deploying Redis and Kafka..."
kubectl apply -f k8s/01-redis.yaml
kubectl apply -f k8s/02-kafka.yaml

echo "--> Waiting for dependencies to be ready..."
kubectl rollout status statefulset/redis -n "${K8S_NAMESPACE}" --timeout=5m
kubectl rollout status statefulset/zookeeper -n "${K8S_NAMESPACE}" --timeout=5m
kubectl rollout status statefulset/kafka -n "${K8S_NAMESPACE}" --timeout=5m

echo "--> Updating application deployments with image: ${FULL_IMAGE_NAME}"
sed -i.bak "s|image: .*|image: ${FULL_IMAGE_NAME}|g" k8s/04-qod-api-deployment.yaml
sed -i.bak "s|image: .*|image: ${FULL_IMAGE_NAME}|g" k8s/06-qod-processor-deployment.yaml
rm k8s/*.bak

echo "--> Deploying Main Application..."
kubectl apply -f k8s/04-qod-api-deployment.yaml
kubectl apply -f k8s/06-qod-processor-deployment.yaml
kubectl apply -f k8s/05-qod-api-service.yaml
kubectl apply -f k8s/07-network-policy.yaml
kubectl apply -f k8s/08-ingress.yaml

echo "--> Waiting for main application to be ready..."
kubectl rollout status deployment/qod-api-deployment -n "${K8S_NAMESPACE}" --timeout=5m
kubectl rollout status deployment/qod-processor-deployment -n "${K8S_NAMESPACE}" --timeout=5m

echo "✅ Deployment complete! API is now accessible without authentication."
