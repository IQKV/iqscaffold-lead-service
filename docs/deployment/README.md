## 📜 Deployment Guide

### Overview

The IQ Scaffold Lead Service is deployed using Helm charts and automated CI/CD pipelines. The service provides lead management, qualification, scoring, and conversion capabilities with multi-tenancy support.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (PostgreSQL, Redis, RabbitMQ)

### Environments

| Environment | Namespace                | Purpose                      |
| ----------- | ------------------------ | ---------------------------- |
| Test        | `iqkvdev-sit-env`       | Feature branch testing       |
| Staging     | `iqkvdev-uat-env`    | Pre-production validation    |
| Production  | `iqkvdev-prd-env` | Live production environment  |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

<details>
<summary>📋 Pipeline Stages</summary>

The service uses Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgress** - WIP branch auto-deployment
5. **RollbackWorkInProgress** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

</details>

<details>
<summary>🔐 Required Drone Secrets</summary>

| Secret Name                       | Purpose                              | Used In                                    |
| --------------------------------- | ------------------------------------ | ------------------------------------------ |
| `NEXUS_DEPLOYER_USERNAME`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `NEXUS_DEPLOYER_PASSWORD`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `SONAR_HOST`                      | SonarQube server URL                 | Static code analysis                       |
| `SONAR_TOKEN`                     | SonarQube authentication token       | Static code analysis                       |
| `SLACK_WEBHOOK`                   | Slack notifications webhook URL      | Build status notifications                 |
| `GITHUB_API_ACCESS_TOKEN`         | GitHub API access for releases       | Release creation, changelog generation     |
| `SVC_CONTAINER_REGISTRY_USERNAME` | Container registry authentication    | Docker image publishing                    |
| `SVC_CONTAINER_REGISTRY_PASSWORD` | Container registry authentication    | Docker image publishing                    |
| `HELM_CHARTS_REPOSITORY`          | Helm charts repository URL           | Kubernetes deployments                     |
| `INFRA_POSTGRESQL_PASSWORD`       | PostgreSQL database password         | Lead data storage                          |
| `INFRA_REDIS_PASSWORD`            | Redis cache password                 | Lead scoring cache, session management     |
| `INFRA_RABBITMQ_PASSWORD`         | RabbitMQ message broker password     | Lead lifecycle event messaging             |
| `JWT_SECRET_KEY`                  | JWT symmetric validation key (HS256) | Request authentication validation          |

</details>

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

<details>
<summary>Helm Commands</summary>

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-lead-service ./ \
  --values ./values.yaml \
  --values ./values-test.yaml \
  --set image.tag=wip \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.lead.security.jwt.secretKey=${JWT_SECRET_KEY} \
  --namespace iqkvdev-sit-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-lead-service ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --set image.tag=${DRONE_TAG} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.lead.security.jwt.secretKey=${JWT_SECRET_KEY} \
  --namespace iqkvdev-prd-env
```

</details>

#### Drone CI Secrets Configuration

The following secrets must be configured in Drone CI for automated deployments:

```bash
# Configure Drone secrets (run once per repository)
drone secret add --repository IQKV/iqscaffold-lead-service --name INFRA_POSTGRESQL_PASSWORD --data "your-postgresql-password"
drone secret add --repository IQKV/iqscaffold-lead-service --name INFRA_REDIS_PASSWORD --data "your-redis-password"
drone secret add --repository IQKV/iqscaffold-lead-service --name INFRA_RABBITMQ_PASSWORD --data "your-rabbitmq-password"
drone secret add --repository IQKV/iqscaffold-lead-service --name JWT_SECRET_KEY --data "your-secure-symmetric-key"
```

#### Environment Variable Mapping

| Drone Secret                | Helm Parameter                       | Application Environment Variable | Description                      |
| --------------------------- | ------------------------------------ | -------------------------------- | -------------------------------- |
| `INFRA_POSTGRESQL_PASSWORD` | `infraServices.postgresql.password`  | `SPRING_DATASOURCE_PASSWORD`     | PostgreSQL database password     |
| `INFRA_REDIS_PASSWORD`      | `infraServices.redis.password`       | `SPRING_REDIS_PASSWORD`          | Redis cache password             |
| `INFRA_RABBITMQ_PASSWORD`   | `infraServices.rabbitmq.password`    | `SPRING_RABBITMQ_PASSWORD`       | RabbitMQ message broker password |
| `JWT_SECRET_KEY`            | `config.lead.security.jwt.secretKey` | `JWT_SECRET_KEY`                 | JWT symmetric validation secret  |

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-lead-service

# Deploy to development
helm upgrade --install lead-service ./ \
  --values values-sit.yaml \
  --set infraServices.postgresql.password="your-postgresql-password" \
  --set infraServices.redis.password="your-redis-password" \
  --set infraServices.rabbitmq.password="your-rabbitmq-password" \
  --set config.lead.security.jwt.secretKey="your-secure-symmetric-key" \
  --namespace iqkvdev-sit-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install lead-service ./ \
  --values values-sit.yaml \
  --set infraServices.postgresql.password="your-postgresql-password" \
  --set infraServices.redis.password="your-redis-password" \
  --set infraServices.rabbitmq.password="your-rabbitmq-password" \
  --set config.lead.security.jwt.secretKey="your-secure-symmetric-key" \
  --namespace iqkvdev-sit-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install lead-service ./ \
  --values values-prd.yaml \
  --set infraServices.postgresql.password="${POSTGRESQL_PASSWORD}" \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set infraServices.rabbitmq.password="${RABBITMQ_PASSWORD}" \
  --set config.lead.security.jwt.secretKey="${JWT_SECRET_KEY}" \
  --namespace iqkvdev-prd-env \
  --create-namespace
```

### Configuration

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
    kubectl logs deployment/iqscaffold-lead-service -n iqkvdev-sit-env
    ```

2. **Redis Connection Issues**

    ```bash
    # Check Redis connectivity
    kubectl exec -it deployment/iqscaffold-lead-service -n iqkvdev-sit-env -- \
      redis-cli -h iqscaffold-redis -p 6379 ping

    # Verify Redis password configuration
    kubectl get secret iqscaffold-lead-service-secrets -o yaml | grep redis
    ```

3. **Check Configuration**

    ```bash
    kubectl describe configmap iqscaffold-lead-service-config -n iqkvdev-sit-env
    ```

4. **Test Health Endpoints**

    ```bash
    kubectl port-forward deployment/iqscaffold-lead-service 8081:8081 -n iqkvdev-sit-env
    curl http://localhost:8081/actuator/health
    ```

5. **Lead Scoring Issues**

    ```bash
    # Check lead configuration
    kubectl get configmap iqscaffold-lead-service-config -o yaml | grep LEAD_
    ```

6. **Service Integration Issues**
    ```bash
    # Test service connectivity
    kubectl exec -it deployment/iqscaffold-lead-service -n iqkvdev-sit-env -- \
      curl http://iqscaffold-contact-service/actuator/health
    ```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-lead-service -n iqkvdev-prd-env

# Or uninstall completely
helm uninstall iqscaffold-lead-service -n iqkvdev-prd-env
```

### Security

- All sensitive values passed via `--set` flags
- TLS enabled in production
- Network policies restrict pod communication
- Non-root container execution (UID: 1001)
- Read-only root filesystem in production
- Minimal container capabilities (drop ALL)
