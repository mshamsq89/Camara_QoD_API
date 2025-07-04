#!/bin/bash

set -e

echo "🔍 Checking system requirements..."

# Function to compare versions
version_ge() {
  [ "$(printf '%s\n' "$2" "$1" | sort -V | head -n1)" = "$2" ]
}

# Java
if command -v java &> /dev/null; then
  JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}')
  if version_ge "$JAVA_VERSION" "17"; then
    echo "✅ Java version $JAVA_VERSION detected"
  else
    echo "❌ Java 17+ required, found version $JAVA_VERSION"
    exit 1
  fi
else
  echo "❌ Java not found"
  exit 1
fi

# Maven
if command -v mvn &> /dev/null; then
  MAVEN_VERSION=$(mvn -v | awk '/Apache Maven/ {print $3}')
  if version_ge "$MAVEN_VERSION" "3.8"; then
    echo "✅ Maven version $MAVEN_VERSION detected"
  else
    echo "❌ Maven 3.8+ required, found version $MAVEN_VERSION"
    exit 1
  fi
else
  echo "❌ Maven not found"
  exit 1
fi
# Docker
if command -v docker &> /dev/null; then
  echo "✅ Docker installed"
else
  echo "❌ Docker not found"
  exit 1
fi

# Docker Compose
if docker compose version &> /dev/null || docker-compose version &> /dev/null; then
  echo "✅ Docker Compose installed"
else
  echo "❌ Docker Compose not found"
  exit 1
fi

# Docker login check
if docker info | grep -q "Username:"; then
  echo "✅ Docker is logged in"
else
  echo "⚠️  Docker is not logged in. Push access may fail."
fi

# kubectl
if command -v kubectl &> /dev/null; then
  if kubectl cluster-info &> /dev/null; then
    echo "✅ kubectl is connected to a cluster"
  else
    echo "❌ kubectl is not connected to a cluster"
    exit 1
  fi
else
  echo "❌ kubectl not found"
  exit 1
fi

# Ingress Controller check
if kubectl get pods -A | grep -E 'ingress-nginx|istio-ingressgateway' &> /dev/null; then
  echo "✅ Ingress Controller detected"
else
  echo "⚠️  No Ingress Controller detected (NGINX or Istio)"
fi

# cert-manager check (optional)
if kubectl get pods -A | grep cert-manager &> /dev/null; then
  echo "✅ cert-manager is installed"
else
  echo "ℹ️  cert-manager not found (optional)"
fi

echo "🎉 All checks completed."

