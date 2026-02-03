## 📜 Deployment Guide

### Overview

The IQ Scaffold Lead Service is deployed using Helm charts and automated CI/CD pipelines. The service provides lead management, qualification, scoring, and conversion capabilities with multi-tenancy support.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (PostgreSQL, Redis, RabbitMQ)

### Environments

| Environment | Namespace                   | Purpose                      |
| ----------- | --------------------------- | ---------------------------- |
| Dev         | `iqscaffold-dev-env`        | Development and WIP branches |
| Test        | `iqscaffold-test-env`       | Feature branch testing       |
| Staging     | `iqscaffold-staging-env`    | Pre-production validation    |
| Production  | `iqscaffold-production-env` | Live production environment  |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

The service uses a comprehensive Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgressOnDev** - WIP branch auto-deployment
5. **RollbackWorkInProgressOnDev** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-lead-service ./ \
  --values ./values.yaml \
  --values ./values-dev.yaml \
  --set image.tag=wip \
  --set secrets.database.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set secrets.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --namespace iqscaffold-dev-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-lead-service ./ \
  --values ./values.yaml \
  --values ./values-production.yaml \
  --set image.tag=${DRONE_TAG} \
  --set secrets.database.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set secrets.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --namespace iqscaffold-production-env
```

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-lead-service

# Deploy to development
helm upgrade --install lead-service ./ \
  --values values-dev.yaml \
  --set secrets.database.password="your-db-password" \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install lead-service ./ \
  --values values-local.yaml \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install lead-service ./ \
  --values values-production.yaml \
  --set secrets.database.password="${DB_PASSWORD}" \
  --set secrets.redis.password="${REDIS_PASSWORD}" \
  --set secrets.rabbitmq.password="${RABBITMQ_PASSWORD}" \
  --namespace iqscaffold-production-env \
  --create-namespace
```

### Configuration

#### Required Secrets

| Secret            | Environment Variable       | Required | Description             |
| ----------------- | -------------------------- | -------- | ----------------------- |
| Database Password | `INFRA_POSTGRESQL_PASSWORD`  | ✅       | PostgreSQL password     |
| RabbitMQ Password | `INFRA_RABBITMQ_PASSWORD` | ⚠️       | Message broker password |
| Redis Password    | `REDIS_PASSWORD`           | ⚠️       | Cache password          |

#### External Services

The service connects to these external infrastructure components:

- **PostgreSQL**: Lead data storage (database: `iqscaffold_lead`, Redis DB: 3)
- **Redis**: Lead scoring cache and session management
- **RabbitMQ**: Event messaging for lead lifecycle events
- **User Service**: JWT validation and user context
- **Contact Service**: Lead conversion integration
- **Pipeline Service**: Lead pipeline management

#### Service Configuration

| Setting        | Dev      | Production       |
| -------------- | -------- | ---------------- |
| Replicas       | 1        | 3                |
| CPU Request    | 200m     | 500m             |
| Memory Request | 256Mi    | 512Mi            |
| CPU Limit      | 500m     | 1000m            |
| Memory Limit   | 512Mi    | 1Gi              |
| Autoscaling    | Disabled | 3-10 replicas    |
| Ingress        | Disabled | Enabled with TLS |
| Monitoring     | Enabled  | Enabled          |

#### Lead-Specific Configuration

| Setting                 | Dev | Production |
| ----------------------- | --- | ---------- |
| Auto Scoring            | ✅  | ✅         |
| Auto Qualification      | ❌  | ✅         |
| Min Qualification Score | 50  | 70         |
| Require Email           | ✅  | ✅         |
| Require Phone           | ❌  | ✅         |
| Require Company         | ❌  | ✅         |

### Monitoring & Health Checks

#### Health Endpoints

- **Liveness**: `/actuator/health/liveness` (port 8081)
- **Readiness**: `/actuator/health/readiness` (port 8081)
- **Metrics**: `/actuator/prometheus` (port 8081)

#### Monitoring Stack

Production deployments include:

- Prometheus ServiceMonitor
- Alerting rules for service health:
  - **LeadServiceDown**: Service unavailable for >1 minute
  - **LeadServiceHighMemory**: Memory usage >80% for >5 minutes
  - **LeadServiceHighLatency**: 95th percentile latency >2 seconds
  - **LeadServiceCircuitBreakerOpen**: Circuit breaker open for >1 minute
- Grafana dashboards for lead metrics

### Troubleshooting

#### Common Issues

1. **Database Connection Failures**

   ```bash
   kubectl logs deployment/iqscaffold-lead-service -n iqscaffold-dev-env
   ```

2. **Check Configuration**

   ```bash
   kubectl describe configmap iqscaffold-lead-service-config -n iqscaffold-dev-env
   ```

3. **Test Health Endpoints**

   ```bash
   kubectl port-forward deployment/iqscaffold-lead-service 8081:8081 -n iqscaffold-dev-env
   curl http://localhost:8081/actuator/health
   ```

4. **Lead Scoring Issues**

   ```bash
   # Check lead configuration
   kubectl get configmap iqscaffold-lead-service-config -o yaml | grep LEAD_
   ```

5. **Service Integration Issues**
   ```bash
   # Test service connectivity
   kubectl exec -it deployment/iqscaffold-lead-service -n iqscaffold-dev-env -- \
     curl http://iqscaffold-contact-service/actuator/health
   ```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-lead-service -n iqscaffold-production-env

# Or uninstall completely
helm uninstall iqscaffold-lead-service -n iqscaffold-production-env
```

### Security

- All sensitive values passed via `--set` flags
- TLS enabled in production
- Network policies restrict pod communication
- Non-root container execution (UID: 1001)
- Read-only root filesystem in production
- Minimal container capabilities (drop ALL)
