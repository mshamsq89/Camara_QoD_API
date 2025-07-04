#!/bin/bash

set -e

echo "🔍 Checking and installing system requirements..."

# Function to compare versions
version_ge() {
  [ "$(printf '%s\n' "$2" "$1" | sort -V | head -n1)" = "$2" ]
}

install_package() {
  echo "📦 Installing $1..."
  sudo apt-get update -y
  sudo apt-get install -y "$1"
}

# Java
if command -v java &> /dev/null; then
  JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}')
  if version_ge "$JAVA_VERSION" "17"; then
    echo "✅ Java version $JAVA_VERSION detected"
  else
    echo "❌ Java 17+ required, found version $JAVA_VERSION"
    install_package openjdk-17-jdk
  fi
else
  echo "❌ Java not found"
  install_package openjdk-17-jdk
fi

# Maven
if command -v mvn &> /dev/null; then
  MAVEN_VERSION=$(mvn -v | awk '/Apache Maven/ {print $3}')
  if version_ge "$MAVEN_VERSION" "3.8"; then
    echo "✅ Maven version $MAVEN_VERSION detected"
  else
    echo "❌ Maven 3.8+ required, found version $MAVEN_VERSION"
    install_package maven
  fi
else
  echo "❌ Maven not found"
  install_package maven
fi

# Docker
if ! command -v docker &> /dev/null; then
  echo "❌ Docker not found"
  echo "📦 Installing Docker..."
  curl -fsSL https://get.docker.com -o get-docker.sh
  sudo sh get-docker.sh
  rm get-docker.sh
else
  echo "✅ Docker installed"
fi

# Docker Compose
if ! docker compose version &> /dev/null && ! docker-compose version &> /dev/null; then
  echo "❌ Docker Compose not found"
  echo "📦 Installing Docker Compose..."
  sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.6/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  sudo chmod +x /usr/local/bin/docker-compose
else
  echo "✅ Docker Compose installed"
fi

# Docker login check
if docker info | grep -q "Username:"; then
  echo "✅ Docker is logged in"
else
  echo "⚠️  Docker is not logged in. Run 'docker login' to enable push access."
fi
# kubectl
if ! command -v kubectl &> /dev/null; then
  echo "❌ kubectl not found"
  echo "📦 Installing kubectl..."
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
  rm kubectl
else
  installed"
fi

# Cluster access
if kubectl cluster-info &> /dev/null; then
  echo "✅ kubectl is connected to a cluster"
else
  echo "❌ kubectl is not connected to a cluster"
  exit 1
fi

# Ingress Controller check
if kubectl get pods -A | grep -E 'ingress-nginx|istio-ingressgateway' &> /dev/null; then
  echo "✅ Ingress Controller detected"
else
  echo "⚠️  No Ingress Controller detected NGINX or Istio"
fi

# cert-manager check (optional)
if kubectl get pods -A | grep cert-manager &> /dev/null; then
  echo "✅ cert-manager is installed"
else
  echo "ℹ️  cert-manager not found optional"
fi

#echo " All checks and installations completed"
