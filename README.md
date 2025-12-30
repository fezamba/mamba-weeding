# Mamba Wedding – RSVP, Gift Registry and Admin Panel

Este projeto é uma aplicação web completa para gerenciamento de casamento, composta por um site público para convidados e um painel administrativo para os noivos.
O sistema permite confirmação de presença via código, gerenciamento de convidados, lista de presentes, integração com gateway de pagamento e armazenamento híbrido em MySQL e MongoDB.

---

## Estrutura do Repositório

```
mamba-wedding/
 ├── backend/          # Aplicação Spring Boot 
 ├── frontend/         # Aplicação Angular
 ├── infra/            # Docker Compose, Nginx e configurações de infraestrutura
 ├── docs/             # Documentação e diagramas
 └── README.md
```

---

## Tech Stack

### Frontend

* Angular 20
* TypeScript
* Angular Router
* Angular Material

### Backend

* Java 21
* Spring Boot 4
* Spring Web
* Spring Data JPA
* Spring Data MongoDB
* Spring Security + OAuth2 - Google
* Spring Validation
* Lombok

### Bancos de Dados

* MySQL 8 (dados transacionais)
* MongoDB 8 (recados e logs de eventos)

### Infraestrutura

* Docker / Docker Compose

---

## MVP – Funcionalidades Iniciais

### Público

* Página inicial com informações do evento
* Confirmação de presença usando código do convite (RSVP)

### Administração

* Login via Google OAuth2
* Listagem de convidados
* Status de RSVP (pendente, confirmado, recusado)

---

## Modelagem Inicial

### Entidade Guest (MySQL)

* id
* nomeCompleto
* codigoConvite
* statusConvite (PENDENTE, CONFIRMADO, RECUSADO)
* rsvpEm
* lado (noivo, noiva, amigos, família)
* email (opcional)
* telefone (opcional)
* observacoes (opcional)