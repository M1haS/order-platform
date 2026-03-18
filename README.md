# Order Platform

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
│                 │
│  Kafka:         │
│  delivery.events│
└────────┬────────┘
         │
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

### Инфраструктура

- **PostgreSQL** — хранение данных
- **Kafka** — асинхронное взаимодействие между сервисами
- **Топики:** `orders.event`, `delivery.events`

---

## Быстрый старт

### 1. Запуск инфраструктуры

```bash
docker-compose up -d
```

### 2. Запуск сервисов

```bash
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :payment-service:bootRun
./gradlew :delivery-service:bootRun --args='--spring.profiles.active=local'
```

### 3. Swagger UI

- Order Service: http://localhost:8080/swagger-ui.html
- Payment Service: http://localhost:8081/swagger-ui.html

---

## API

### Order Service `POST /api/orders`

Создать заказ:

```json
{
  "customerId": 1,
  "address": "ул. Ленина 1",
  "items": [
    { "itemId": 10, "quantity": 2 }
  ]
}
```

### `GET /api/orders/{id}`

Получить заказ по ID.

### `POST /api/orders/{id}/pay`

Оплатить заказ:

```json
{
  "paymentMethod": "CARD"
}
```

Методы оплаты: `CARD` (успех), `QR` (отказ).

### Payment Service `POST /api/payments`

Внутренний эндпоинт, вызывается из `order-service`.

---

## Тесты

```bash
./gradlew test
```

Отчёты после запуска:
- `order-service/build/reports/tests/test/index.html`
- `payment-service/build/reports/tests/test/index.html`
- `delivery-service/build/reports/tests/test/index.html`

---

## Стек

- Java 21
- Spring Boot 3.x
- Spring Kafka
- Spring Data JPA
- MapStruct
- Lombok
- PostgreSQL
- H2 (тесты)
- JUnit 5 + Mockito
