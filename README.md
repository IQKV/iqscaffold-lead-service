# 🎯 IQ Scaffold Lead Service

> Comprehensive CRM lead management microservice providing multi-tenant lead capture, qualification, scoring, and conversion capabilities with automated workflows and intelligent lead routing.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Overview](#overview)
- [What It Demonstrates](#what-it-demonstrates)
- [Architecture Patterns](#architecture-patterns)
- [Technical Highlights](#technical-highlights)
- [Use Cases Implemented](#use-cases-implemented)
- [API Examples](#api-examples)
- [Learning Points](#learning-points)
- [Adapting for Your Domain](#adapting-for-your-domain)
- [Integration with Other Services](#integration-with-other-services)
- [Deployment Guide](docs/deployment/README.md)

## Business Purpose

CRM Lead management service that handles:

- **Lead Capture** - Multi-channel lead ingestion from websites, forms, APIs, and integrations
- **Lead Qualification** - Automated and manual lead scoring with configurable qualification criteria
- **Lead Assignment** - Intelligent lead routing to sales representatives based on territory, workload, and expertise
- **Lead Conversion** - Seamless conversion of qualified leads to contacts with pipeline tracking
- **Activity Tracking** - Complete audit trail of all lead interactions and touchpoints
- **Source Attribution** - Comprehensive tracking of lead sources for ROI analysis and campaign optimization
- **Multi-Tenancy** - Complete tenant isolation ensuring data segregation across organizations

## Overview

This is the lead management hub for the IQ Scaffold CRM platform. It centralizes lead lifecycle management, enabling sales teams to capture, qualify, and convert prospects efficiently while maintaining comprehensive tracking and analytics.

## What It Demonstrates

### 🎯 Lead Management & Qualification

- Comprehensive lead lifecycle management (NEW → CONTACTED → QUALIFIED → CONVERTED)
- Configurable lead scoring system (0-100) with automatic qualification thresholds
- Multi-source lead attribution (Website, Referral, Cold Call, Social Media, etc.)
- Intelligent lead assignment with workload balancing
- Lead conversion tracking with contact service integration
- Activity timeline with complete interaction history

### 📊 Lead Scoring & Analytics

- Configurable scoring algorithms based on lead attributes
- Automatic qualification based on score thresholds (default: 60)
- Lead source quality tracking and ROI analysis
- Engagement scoring with activity weighting
- Performance metrics and conversion analytics

### 🔄 Workflow Automation

- Event-driven lead processing with RabbitMQ integration
- Automated lead assignment based on rules and availability
- Status change notifications and alerts
- Lead nurturing workflows with scheduled follow-ups
- Integration with email marketing and communication tools

## Architecture Patterns

- **Lead Management**: Create, read, update, and delete leads with comprehensive tracking
- **Lead Qualification**: Automatic and manual lead qualification with scoring
- **Lead Scoring**: Configurable lead scoring system (0-100)
- **Lead Assignment**: Assign leads to sales representatives
- **Lead Conversion**: Convert qualified leads to contacts
- **Lead Sources**: Track lead sources (Website, Referral, Cold Call, etc.)
- **Lead Notes**: Add and manage notes for each lead
- **Activity Tracking**: Log all lead interactions and activities
- **Multi-tenancy**: Schema-per-tenant isolation
- **Internationalization**: Support for English, Spanish, and French
- **Security**: JWT-based authentication and authorization
- **Caching**: Redis and Hibernate second-level caching
- **Observability**: Metrics, tracing, and health checks
- **API Documentation**: OpenAPI/Swagger integration

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis + Hibernate/Ehcache
- **Messaging**: RabbitMQ
- **Security**: Spring Security with OAuth2 JWT
- **Documentation**: SpringDoc OpenAPI
- **Observability**: Micrometer, OpenTelemetry
- **Testing**: JUnit 5, Testcontainers

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Maven 3.9+

### Local Development

1. **Start infrastructure services**:

   <details>
   <summary>Click to expand bash commands</summary>

   ```bash
   docker compose up -d postgres-lead redis-lead rabbitmq-lead
   ```

   </details>

2. **Run the application**:

   <details>
   <summary>Click to expand bash commands</summary>

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   </details>

3. **Access the application**:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

### Docker Development

<details>
<summary>Click to expand bash commands</summary>

```bash
# Build and run all services
docker compose up --build

# Run in detached mode
docker compose up -d
```

</details>

## API Endpoints

<details>
<summary>Click to expand API endpoints</summary>

### Leads

- `GET /api/v1/leads` - List leads with pagination and filters
- `POST /api/v1/leads` - Create new lead
- `GET /api/v1/leads/{id}` - Get lead by ID
- `PUT /api/v1/leads/{id}` - Update lead
- `DELETE /api/v1/leads/{id}` - Delete lead
- `GET /api/v1/leads/search?q={term}` - Search leads
- `GET /api/v1/leads?status={status}` - Filter by status
- `GET /api/v1/leads?source={source}` - Filter by source
- `GET /api/v1/leads?assignedTo={user}` - Filter by assignment

### Lead Actions

- `PATCH /api/v1/leads/{id}/qualify` - Qualify lead
- `PATCH /api/v1/leads/{id}/disqualify` - Disqualify lead
- `PATCH /api/v1/leads/{id}/assign` - Assign lead to user
- `PATCH /api/v1/leads/{id}/score` - Update lead score
- `POST /api/v1/leads/{id}/convert` - Convert lead to contact

### Lead Notes

- `GET /api/v1/leads/{id}/notes` - Get lead notes
- `POST /api/v1/leads/{id}/notes` - Add note to lead
- `PUT /api/v1/leads/{id}/notes/{noteId}` - Update note
- `DELETE /api/v1/leads/{id}/notes/{noteId}` - Delete note
- `PATCH /api/v1/leads/{id}/notes/{noteId}/pin` - Pin/unpin note

### Lead Activities

- `GET /api/v1/leads/{id}/activities` - Get lead activity timeline
- `POST /api/v1/leads/{id}/activities` - Log activity

### Lead Sources

- `GET /api/v1/leads/sources` - List available lead sources

</details>

## Configuration

### Environment Variables

Key environment variables for configuration:

<details>
<summary>Click to expand environment variables</summary>

```bash
# Database
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5432/iqscaffold_lead_local
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_lead
IQSCAFFOLD_DATABASE_PASSWORD=iqscaffold_password

# Redis
IQSCAFFOLD_CACHE_REDIS_HOST=localhost
IQSCAFFOLD_CACHE_REDIS_PORT=6379
IQSCAFFOLD_CACHE_REDIS_DATABASE=3

# RabbitMQ
IQSCAFFOLD_MESSAGING_RABBITMQ_HOST=localhost
IQSCAFFOLD_MESSAGING_RABBITMQ_PORT=5672

# Security
USER_SERVICE_URL=http://iqscaffold-user-service:8080
JWT_ISSUER=iqscaffold-user-service

# Lead Features
LEAD_ENABLE_AUTO_SCORING=true
LEAD_ENABLE_AUTO_QUALIFICATION=false
```

</details>

### Profiles

- `local` - Local development with debug logging
- `staging` - Staging environment configuration
- `production` - Production environment with JSON logging

## Lead Lifecycle

```
1. Lead Capture
   └─> Lead created with status: NEW
       Source tracked (Website, Referral, etc.)

2. Lead Qualification
   └─> Manual or automatic qualification
       Score updated (0-100)
       Status: NEW → CONTACTED → QUALIFIED

3. Lead Assignment
   └─> Assign to sales representative
       Notifications sent

4. Lead Conversion
   └─> Convert qualified lead to contact
       Status: CONVERTED
       Contact created in contact-service
       Lead archived with conversion data

5. Alternative: Lead Lost
   └─> Mark as LOST or UNQUALIFIED
       Reason tracked in notes
```

## Lead Status Flow

```
NEW → CONTACTED → QUALIFIED → CONVERTED
  ↓       ↓           ↓
LOST    LOST    UNQUALIFIED
```

## Lead Scoring

- **Default Score**: 0
- **Max Score**: 100
- **Qualification Threshold**: 60 (configurable)
- **Auto-scoring**: Configurable based on lead attributes

### Scoring Factors (Example)

- Email provided: +10
- Phone provided: +10
- Company provided: +15
- Job title provided: +10
- Source quality: +5 to +20
- Engagement activities: +5 per activity

## Multi-tenancy

The service uses schema-per-tenant isolation:

1. **Tenant Identification**: Via `X-Tenant-ID` header
2. **Schema Management**: Automatic schema creation and migration
3. **Data Isolation**: Complete separation between tenants

## Database Schema

### System Schema (public)

- `tenant_info` - Tenant metadata and schema mapping

### Tenant Schemas

- `leads` - Lead information with scoring and qualification
- `lead_sources` - Available lead sources
- `lead_notes` - Notes attached to leads
- `lead_activities` - Activity timeline for leads

## Events Published

The service publishes events to RabbitMQ:

```
lead.created
  - When new lead is created
  - Payload: { leadId, email, source, status }

lead.qualified
  - When lead is qualified
  - Payload: { leadId, score, qualifiedBy }

lead.assigned
  - When lead is assigned
  - Payload: { leadId, assignedTo, assignedBy }

lead.converted
  - When lead is converted to contact
  - Payload: { leadId, contactId, convertedAt }

lead.status.changed
  - When lead status changes
  - Payload: { leadId, oldStatus, newStatus }
```

## Development

### Running Tests

<details>
<summary>Click to expand bash commands</summary>

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean verify jacoco:report
```

</details>

### Code Quality

The project includes:

- Checkstyle for code style
- JaCoCo for test coverage (70% minimum)
- ArchUnit for architecture testing

### Adding New Features

1. Create feature branch from `main`
2. Implement feature with tests
3. Update documentation
4. Submit pull request

## Monitoring

### Health Checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`

### Metrics

- Prometheus: `/actuator/prometheus`
- Application metrics: `/actuator/metrics`

### Tracing

- OpenTelemetry integration
- Distributed tracing support

### Grafana Dashboard

A Grafana dashboard is available at `docs/monitoring/grafana-dashboard.json` providing real-time visibility into:

- Service Health: uptime, request rate, error rate, p95 latency, active sessions
- HTTP Metrics: request rate by status code (2xx/4xx/5xx), response time percentiles (p50/p95/p99)
- JVM Memory: heap/non-heap usage, GC pause time, thread count
- Database: HikariCP connection pool usage, connection acquisition time
- Business Metrics: user registration rate, login attempts (success/failed), email verification rate

The dashboard uses Prometheus as the data source and auto-refreshes every 30 seconds. Import it into your Grafana instance to monitor service performance and health.

## Integration with Other Services

### User Service

- JWT authentication
- User information for assignments

### Contact Service

- Lead conversion creates contacts
- Event-driven communication

### Pipeline Service (Future)

- Lead pipeline management
- Stage tracking

## Troubleshooting

### Common Issues

1. **Database Connection**: Verify PostgreSQL is running and credentials are correct
2. **Redis Connection**: Check Redis service and port configuration
3. **JWT Validation**: Ensure user service is accessible and JWT configuration is correct
4. **Lead Conversion**: Verify contact service is running and accessible

### Logs

<details>
<summary>Click to expand bash commands</summary>

```bash
# View application logs
docker-compose logs lead-service

# Follow logs
docker-compose logs -f lead-service
```

</details>

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
