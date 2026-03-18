# Configuration Guide

## Kafka Configuration

Kafka bootstrap servers are configured externally via `application-local.yaml` or environment variables.

### Local Development

To run with local Kafka (via Docker Compose):

```bash
# Start infrastructure
docker-compose up -d

# Run services with local profile
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :delivery-service:bootRun --args='--spring.profiles.active=local'
```

### Production

Set environment variable before running:

```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-broker-1:9092,kafka-broker-2:9092
java -jar order-service.jar
```

Or use `-D` flag:

```bash
java -Dspring.kafka.bootstrap-servers=kafka:9092 -jar order-service.jar
```

## Configuration Files

- `application.yaml` - base configuration (no Kafka servers specified)
- `application-local.yaml` - local development configuration with `localhost:9092`
- `.env` - environment variables for Docker Compose

## Available Profiles

- `local` - uses localhost Kafka and PostgreSQL
- (add more profiles as needed for staging/production)
