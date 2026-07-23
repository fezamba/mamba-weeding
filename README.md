# Mamba Wedding (in development)

This project is a comprehensive web application for wedding management, comprising a public-facing website for guests and an administrative dashboard for the couple.

The system features RSVP confirmation via access codes, guest management, a gift registry, payment gateway integration, and a hybrid storage architecture utilizing PostgreSQL and MongoDB.

Backend setup, architecture and API contracts are documented in [`docs/BACKEND.md`](docs/BACKEND.md).

---

## Estrutura do Repositório

```
mamba-wedding/
 ├── backend/          # Spring Boot Application
 ├── frontend/         # Angular Application
 ├── infra/            # Infrastructure Configurations
 ├── docs/             # Documentation
 └── README.md
```

---

## Tech Stack

### Frontend

- Angular
- TypeScript

### Backend

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Data MongoDB
- Spring Security + OAuth2 - Google
- Spring Validation
- Lombok

### Database

- PostgreSQL
- MongoDB
