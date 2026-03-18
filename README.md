# Order Platform

![Build](https://github.com/M1haS/order-platform/actions/workflows/build.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Tests](https://img.shields.io/badge/tests-31%20passing-brightgreen)

Микросервисная платформа для обработки заказов на Java + Spring Boot.

## Архитектура

```
┌─────────────────┐     HTTP      ┌──────────────────┐
│  order-service  │ ────────────► │ payment-service  │
│   :8080         │               │   :8081          │
└────────┬────────┘               └──────────────────┘
         │
         │  Kafka: orders.event
         ▼
┌─────────────────┐
│delivery-service │
│   :8083         │
└────────┬────────┘
         │  Kafka: delivery.events
         └──► order-service (обновление статуса)
```

### Сервисы

| Сервис | Порт | Описание |
|---|---|---|
| `order-service` | 8080 | Управление заказами, оркестрация |
| `payment-service` | 8081 | Обработка платежей |
| `delivery-service` | 8083 | Назначение курьеров |

### Жизненный цикл заказа

```
PENDING_PAYMENT → PAID → DELIVERY_ASSIGNED
              ↘
               PAYMENT_FAILED
```

---

## Быстрый старт

```bash
# 1. Инфраструктура
docker-compose up -d

# 2. Сервисы
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :payment-service:bootRun
./gradlew :delivery-service:bootRun --args='--spring.profiles.active=local'
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## API

### `POST /api/orders` — создать заказ

```json
{
  "customerId": 1,
  "address": "ул. Ленина 1",
  "items": [{ "itemId": 10, "quantity": 2 }]
}
```

### `GET /api/orders/{id}` — получить заказ

### `POST /api/orders/{id}/pay` — оплатить заказ

```json
{ "paymentMethod": "CARD" }
```

Методы: `CARD` (успех), `QR` (отказ).

Примеры запросов: [`requests.http`](requests.http)

---

## Тесты

```bash
./gradlew test
```

- 18 unit тестов — order-service
- 6 unit тестов — payment-service
- 7 unit тестов — delivery-service
- Integration тесты с Testcontainers (PostgreSQL + Kafka)

---

## Стек

- Java 21, Spring Boot 3
- Apache Kafka
- PostgreSQL + Liquibase
- Spring Data JPA, MapStruct, Lombok
- Docker Compose
- JUnit 5, Mockito, Testcontainers
- Swagger / OpenAPI

## Contributing

См. [CONTRIBUTING.md](CONTRIBUTING.md)
