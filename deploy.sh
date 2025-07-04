#!/bin/bash
set -e

# --- Configuration ---
REGISTRY="ghcr.io/mshamsq89" # <-- IMPORTANT: Replace with your registry
IMAGE_NAME="qod-api"
IMAGE_TAG=$(git rev-parse --short HEAD)
K8S_NAMESPACE="qod"

echo "Deploying image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# --- Build Step ---
echo "✅ Step 1: Packaging the reactive application..."
mvn clean package -DskipTests

# --- Docker Step ---
echo "✅ Step 2: Building the secure Docker image..."
FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t "${FULL_IMAGE_NAME}" .
docker tag "${FULL_IMAGE_NAME}" "${REGISTRY}/${IMAGE_NAME}:latest"

# --- Push Step ---
echo "✅ Step 3: Pushing the Docker image..."
docker push "${FULL_IMAGE_NAME}"
docker push "${REGISTRY}/${IMAGE_NAME}:latest"

# --- Kubernetes Deployment Step ---
echo "✅ Step 4: Deploying high-performance architecture to Kubernetes..."

# Update image tags in both deployment files
sed -i.bak "s|image: .*|image: ${FULL_IMAGE_NAME}|g" k8s/04-qod-api-deployment.yaml
sed -i.bak "s|image: .*|image: ${FULL_IMAGE_NAME}|g" k8s/06-qod-processor-deployment.yaml
rm k8s/*.bak

# Apply all Kubernetes manifests from the k8s directory
kubectl apply -f k8s/

echo "🚀 Deployment complete!"
echo "--------------------------"
echo "To check the status of your deployment, run:"
echo "  kubectl get pods -n ${K8S_NAMESPACE}"
