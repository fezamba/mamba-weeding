# Mamba Wedding — Documentação

## 1. Visão geral

O Mamba Wedding é uma aplicação para gerenciamento de casamento. Este documento descreve a organização do backend, as responsabilidades de cada módulo, os contratos HTTP e as regras de negócio.

Áreas funcionais documentadas:

- autenticação de convidados por código RSVP;
- confirmação ou recusa de presença;
- autenticação administrativa com Google;
- cadastro e exclusão administrativa de convidados;
- cadastro e exclusão administrativa de presentes;
- catálogo de presentes dividido em cotas;
- reserva, cancelamento e compra simulada de cotas;
- expiração automática de reservas;
- mural de mensagens;
- limitação de tentativas em endpoints públicos;
- documentação OpenAPI/Swagger;
- tratamento padronizado de erros.

## 2. Tecnologias

### Backend

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Data MongoDB
- Spring Security
- Bean Validation
- PostgreSQL 16
- MongoDB 7
- JWT por meio de `java-jwt`
- Google ID Token para autenticação administrativa
- Lombok
- Springdoc OpenAPI
- Maven Wrapper

### Testes

- JUnit 5
- Mockito
- Spring Boot Test
- Spring MVC Test/MockMvc
- Spring Security Test

## 3. Arquitetura

O código está organizado por área funcional e, dentro de cada área, por responsabilidade:

```text
backend/src/main/java/com/br/mamba_wedding/
├── admin/
│   └── api/                 autenticação administrativa
├── common/
│   ├── api/                 contratos comuns de resposta
│   └── exception/           exceções e tratamento global
├── config/
│   ├── security/            JWT, filtro e rate limiter
│   ├── SecurityConfig.java
│   └── *DataSeeder.java     dados do perfil de desenvolvimento
├── gifts/
│   ├── api/                 endpoints e DTOs
│   ├── application/         regras de presentes e reservas
│   ├── domain/              entidades e estados
│   └── infrastructure/      repositórios JPA
├── guests/
│   ├── api/                 endpoints e DTOs
│   ├── application/         regras de RSVP
│   ├── domain/              entidade e enums
│   └── infrastructure/      repositório JPA
├── messages/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/      repositório MongoDB
└── payment/
    └── application/         abstração e gateway simulado
```

Fluxo de dependências:

```text
Controller → Service → Repository → Banco
                ↓
          PaymentGateway
```

Controllers validam o contrato HTTP e identificam o usuário autenticado. Services concentram as regras de negócio. Repositórios encapsulam a persistência. Entidades representam o domínio persistido.

## 4. Persistência

### PostgreSQL

Armazena convidados, presentes e transações de presentes.

#### `guests`

Campos principais:

- `id`: identificador interno;
- `fullName`: nome completo;
- `rsvpCode`: código único usado no login;
- `rsvpStatus`: `PENDING`, `CONFIRMED` ou `REJECTED`;
- `rsvpBy`: data da última resposta;
- `side`: `BRIDE` ou `GROOM`;
- `email`;
- `phone`;
- `notes`.

#### `gifts`

Campos principais:

- `id`;
- `version`: versão usada pelo optimistic locking;
- `name`;
- `description`;
- `value`;
- `totalQuotas`;
- `imageUrl`;
- `purchaseLink`.

As cotas disponíveis não são armazenadas diretamente. Elas são calculadas subtraindo as transações `RESERVED` e `PURCHASED` do total. Transações `CANCELED` não ocupam cotas.

#### `gift_transactions`

Campos principais:

- `id`;
- `gift_id`: presente relacionado;
- `guest_id`: convidado proprietário da operação;
- `numberQuotas`;
- `status`: `RESERVED`, `PURCHASED` ou `CANCELED`;
- `reservedAt`;
- `reservedUntil`;
- `purchasedAt`.

Reservas são associadas ao ID do convidado, e não ao nome. Isso impede colisões entre convidados homônimos e permite validar a propriedade da reserva.

### MongoDB

Armazena o mural na coleção `messages`:

- `id`;
- `author`;
- `text`;
- `sendDate`.

As mensagens são listadas da mais recente para a mais antiga.

## 5. Autenticação e autorização

### Convidados

O código RSVP funciona como credencial de entrada:

```text
Código RSVP
    ↓
POST /api/auth/login
    ↓
JWT com ROLE_GUEST
    ↓
Endpoints autenticados
```

Depois do login, o backend identifica o convidado pelo JWT. Endpoints de RSVP e presentes não aceitam outro código ou nome para selecionar o convidado. Isso impede que um convidado altere dados ou reservas de outro.

O JWT possui:

- `subject`: código RSVP ou e-mail administrativo;
- `role`: `ROLE_GUEST` ou `ROLE_ADMIN`;
- `issuer`: configurado por ambiente;
- expiração padrão de duas horas.

### Administradores

O frontend deverá obter um Google ID Token e enviá-lo ao backend. O backend verifica:

1. validade do token;
2. audience correspondente ao `GOOGLE_CLIENT_ID`;
3. e-mail presente em `ADMIN_EMAILS`.

Se autorizado, o backend emite seu próprio JWT com `ROLE_ADMIN`.

### Política dos endpoints

| Acesso                | Endpoints                             |
| --------------------- | ------------------------------------- |
| Público               | `POST /api/auth/login`                |
| Público               | `POST /api/admin/auth/google`         |
| Público               | `GET /api/messages`                   |
| Público               | Swagger e OpenAPI                     |
| Convidado autenticado | RSVP, presentes e criação de mensagem |
| Administrador         | `/api/admin/**`                       |

Requisições sem autenticação para recursos protegidos recebem `401`. Usuários autenticados sem a permissão necessária recebem `403`.

## 6. API

Por padrão, a aplicação executa em `http://localhost:8080`.

Para endpoints autenticados:

```http
Authorization: Bearer <jwt>
```

### 6.1 Login do convidado

#### `POST /api/auth/login`

Requisição:

```json
{
  "rsvpCode": "CONV1234"
}
```

Resposta `200`:

```json
{
  "token": "jwt",
  "fullName": "Convidado Teste",
  "rsvpStatus": "PENDING"
}
```

O endpoint possui rate limit de 10 tentativas por minuto por combinação de IP e código.

### 6.2 RSVP

Dados pessoais de RSVP somente são retornados depois da autenticação.

#### `GET /api/rsvp/me`

Resposta `200`:

```json
{
  "fullName": "Convidado Teste",
  "rsvpStatus": "PENDING",
  "email": "guest@example.com",
  "phone": "21999999999",
  "notes": null
}
```

#### `POST /api/rsvp/confirm`

#### `POST /api/rsvp/decline`

Ambos recebem:

```json
{
  "phone": "21999999999",
  "email": "guest@example.com",
  "notes": "Sem restrições alimentares"
}
```

Validações:

- telefone obrigatório com exatamente 11 dígitos;
- e-mail obrigatório e válido, com até 120 caracteres;
- observações opcionais, com até 255 caracteres.

Resposta de sucesso: `204 No Content`.

### 6.3 Presentes

#### `GET /api/gifts`

Lista presentes com valor total, valor por cota, cotas totais, cotas disponíveis e indicador de esgotado.

#### `GET /api/gifts/{id}`

Retorna os detalhes de um presente.

#### `POST /api/gifts/{id}/reserve`

```json
{
  "quotas": 2
}
```

Regras:

- mínimo de uma cota;
- não pode ultrapassar as cotas disponíveis;
- um convidado não pode manter duas reservas ativas para o mesmo presente;
- a reserva pertence ao convidado do JWT;
- a reserva expira seis horas após sua criação;
- alterações concorrentes do mesmo presente são serializadas com lock pessimista.

Resposta de sucesso: `204 No Content`.

#### `DELETE /api/gifts/{id}/reserve`

Cancela a reserva ativa do convidado autenticado. Um convidado não pode cancelar a reserva de outro.

#### `POST /api/gifts/{id}/buy`

Compra as cotas previamente reservadas pelo convidado autenticado. Uma reserva expirada não pode ser comprada.

O gateway atual é simulado e não realiza cobrança real.

### 6.4 Mensagens

#### `GET /api/messages`

Lista pública de mensagens em ordem decrescente de envio.

#### `POST /api/messages`

Requer convidado autenticado.

```json
{
  "text": "Felicidades ao casal!"
}
```

O autor é obtido do convidado autenticado. A mensagem é obrigatória e aceita até 1.000 caracteres.

### 6.5 Administração de convidados

#### `POST /api/admin/guests/register`

```json
{
  "fullName": "Nome do Convidado",
  "side": "BRIDE",
  "email": "guest@example.com",
  "phone": "21999999999"
}
```

O código RSVP é gerado com três letras normalizadas do nome e quatro dígitos aleatórios. A aplicação tenta evitar colisões antes de salvar, e o banco também possui restrição única.

#### `DELETE /api/admin/guests/{id}/delete`

Exclui um convidado existente.

### 6.6 Administração de presentes

#### `POST /api/admin/gifts/register`

```json
{
  "name": "Geladeira",
  "description": "Geladeira frost free",
  "value": 2500.0,
  "totalQuotas": 5,
  "imageUrl": "https://example.com/image.jpg",
  "purchaseLink": "https://example.com/product"
}
```

Valor e total de cotas devem ser positivos.

#### `DELETE /api/admin/gifts/{id}/delete`

Exclui um presente existente e suas transações associadas.

### 6.7 Login administrativo

#### `POST /api/admin/auth/google`

```json
{
  "googleToken": "google-id-token"
}
```

Respostas relevantes:

- `200`: e-mail autorizado e JWT interno emitido;
- `400`: corpo ou token vazio;
- `401`: token Google inválido ou expirado;
- `403`: token válido, mas e-mail não autorizado.

## 7. Erros HTTP

Erros tratados pelo backend seguem o formato:

```json
{
  "timestamp": "2026-07-21T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Descrição do erro",
  "path": "/api/example"
}
```

Mapeamentos principais:

| Status | Situação                                   |
| ------ | ------------------------------------------ |
| `400`  | validação ou argumento inválido            |
| `401`  | autenticação ausente ou token inválido     |
| `403`  | usuário autenticado sem permissão          |
| `404`  | convidado, presente ou recurso inexistente |
| `405`  | método HTTP não suportado                  |
| `409`  | conflito de estado ou concorrência         |
| `429`  | limite de tentativas excedido              |
| `500`  | erro inesperado                            |

## 8. Reservas, expiração e concorrência

Uma reserva é criada com status `RESERVED`, `reservedAt` atual e `reservedUntil` seis horas no futuro.

O método agendado executa a cada minuto e transforma reservas vencidas em `CANCELED`. O scheduling está habilitado na classe principal da aplicação.

Antes da compra, o prazo também é validado diretamente. Dessa maneira, uma reserva vencida não pode ser paga mesmo que o job ainda não tenha realizado sua próxima execução.

Operações que alteram cotas buscam o presente com `PESSIMISTIC_WRITE`. Requisições concorrentes para o mesmo presente são serializadas pela transação do banco, impedindo que ambas consumam as mesmas cotas disponíveis.

As consultas de catálogo carregam as transações necessárias por `EntityGraph`. Isso permite calcular cotas com `spring.jpa.open-in-view=false` sem depender de uma sessão JPA aberta no controller.

## 9. Pagamentos

`PaymentGateway` define a abstração de pagamento. A implementação atual, `MockPaymentGateway`, apenas registra informações no log e simula latência.

O gateway simulado não oferece:

- cobrança real;
- idempotência de pagamento;
- webhook;
- identificador externo da cobrança;
- retentativas;
- estorno;
- estado intermediário de pagamento.

Esses pontos devem ser definidos antes da integração com Mercado Pago, Stripe ou outro provedor.

## 10. Rate limiter

O `PublicEndpointRateLimiter` utiliza janelas fixas em memória. A chave combina ação, IP e assunto, como o código RSVP.

Essa implementação é adequada para desenvolvimento e uma única instância, mas possui limitações para produção:

- não compartilha estado entre instâncias;
- perde o estado ao reiniciar;
- depende da configuração confiável dos headers de proxy;
- não possui limpeza dedicada de chaves antigas.

Para múltiplas instâncias, recomenda-se Redis ou controle equivalente no gateway/reverse proxy.

## 11. Configuração local

### Variáveis de ambiente

Crie `backend/.env`. O arquivo é ignorado pelo Git e seus valores não devem ser documentados ou versionados.

```dotenv
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
MONGO_ROOT_USERNAME=
MONGO_ROOT_PASSWORD=
MONGO_DATABASE=
JWT_SECRET=
JWT_ISSUER=
GOOGLE_CLIENT_ID=
ADMIN_EMAILS=
```

`ADMIN_EMAILS` aceita uma lista separada por vírgulas.

### Bancos com Docker

No diretório `backend`:

```bash
docker compose up -d
```

Serviços locais:

- PostgreSQL: porta `5432`;
- MongoDB: porta `27017`.

Os volumes `postgres_data` e `mongo_data` preservam os dados entre reinicializações.

### Executar a aplicação

Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Swagger

Com a aplicação em execução:

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 12. Testes

Executar toda a suíte:

```bash
cd backend
./mvnw test
```

O Mockito é carregado como `-javaagent` pelo Maven Surefire. Isso evita a dependência do mecanismo de auto-attach da JVM.

### Cobertura existente por área

| Suíte                           | Responsabilidade                                         |
| ------------------------------- | -------------------------------------------------------- |
| `MambaWeddingApplicationTests`  | carregamento completo do contexto sem bancos externos    |
| `AdminAuthControllerTest`       | validação do contrato do login Google                    |
| `TokenServiceTest`              | geração, claims, issuer e token inválido                 |
| `SecurityFilterTest`            | autenticação de convidado/admin e tokens impróprios      |
| `PublicEndpointRateLimiterTest` | limite, bloqueio e IP encaminhado                        |
| `AuthControllerTest`            | login, resposta, código inválido e rate limit            |
| `GuestRsvpControllerTest`       | identidade autenticada, `/me`, confirmação e recusa      |
| `GuestRsvpServiceTest`          | consulta por ID, atualização e geração do código         |
| `GuestControllerTest`           | autorização dos endpoints administrativos                |
| `GiftControllerTest`            | autorização administrativa de presentes                  |
| `GuestGiftControllerTest`       | identidade em reserva, compra e cancelamento             |
| `GiftServiceTest`               | cotas, duplicidade, expiração, compra e limpeza agendada |
| `GiftTest`                      | cálculo de disponibilidade e esgotamento                 |

O teste de contexto usa mocks para os quatro repositórios e desabilita apenas as autoconfigurações de persistência. Assim, valida controllers, services, segurança, scheduling e demais beans sem exigir PostgreSQL ou MongoDB em execução.

## 13. Dados de desenvolvimento

No perfil `dsv`, seeders inserem convidados e presentes quando as respectivas tabelas estão vazias.

Observações:

- os seeders não executam no perfil de teste;
- o perfil `dsv` é definido como ativo no `application.yml`;
- dados de seed são apenas para desenvolvimento.

## 14. Integração do cliente com RSVP

O JWT é a fonte de identidade do convidado em todas as operações posteriores ao login.

Um cliente da API deve:

1. autenticar pelo código RSVP;
2. armazenar o JWT com segurança;
3. enviar `Authorization: Bearer <jwt>`;
4. não enviar `rsvpCode` em confirmação ou recusa;
5. usar `/api/rsvp/me` para obter os dados completos;
6. consumir `fullName` e `rsvpStatus` da resposta do login quando necessário.
