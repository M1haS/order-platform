# Contributing

## Требования

- Java 21
- Docker & Docker Compose
- Gradle 9+

## Локальный запуск

```bash
# Запустить инфраструктуру
docker-compose up -d

# Запустить сервисы
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :payment-service:bootRun
./gradlew :delivery-service:bootRun --args='--spring.profiles.active=local'
```

## Тесты

```bash
# Все тесты
./gradlew test

# Конкретный сервис
./gradlew :order-service:test

# С подробным выводом
./gradlew test --info
```

Интеграционные тесты поднимают PostgreSQL и Kafka через Testcontainers — Docker должен быть запущен.

## HTTP-запросы

Примеры запросов в `requests.http` — открываются в IntelliJ IDEA.

## Структура проекта

```
order-platform/
├── common-libs/       # Общие DTO и события
├── order-service/     # Управление заказами (:8080)
├── payment-service/   # Платежи (:8081)
├── delivery-service/  # Доставка (:8083)
└── docker-compose.yaml
```

## Миграции базы данных

Используется Liquibase. Файлы миграций в `src/main/resources/db/changelog/`.
Новые миграции добавляются в `changes/` и подключаются в `db.changelog-master.yaml`.
