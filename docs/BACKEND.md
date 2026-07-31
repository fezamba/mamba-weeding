# Mamba Wedding Backend — Referência técnica

## 1. Visão geral

Este documento descreve a implementação atual do backend Mamba Wedding: organização do código, persistência, autenticação, contratos HTTP, regras de negócio, configuração e testes automatizados.

Áreas funcionais documentadas:

- autenticação de convidados por código RSVP;
- convites e confirmações de presença independentes por evento;
- consulta administrativa de presenças, com filtros, paginação e resumo;
- autenticação administrativa com Google;
- cadastro e exclusão administrativa de convidados;
- cadastro e exclusão administrativa de presentes;
- catálogo de presentes dividido em cotas;
- reserva, cancelamento e compra simulada de cotas;
- expiração automática de reservas;
- mural de mensagens;
- limitação de tentativas no login RSVP;
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
- Flyway
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
- Testcontainers
- Maven Surefire e Failsafe

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
│   ├── JpaRepositoryConfig.java
│   ├── MongoRepositoryConfig.java
│   ├── SecurityConfig.java
│   └── *DataSeeder.java     dados do perfil de desenvolvimento
├── gifts/
│   ├── api/                 endpoints e DTOs
│   ├── application/         regras de presentes e reservas
│   ├── domain/              entidades e estados
│   └── infrastructure/      repositórios JPA
├── events/
│   ├── api/                 endpoints de eventos e confirmações
│   ├── application/         regras de RSVP por evento
│   ├── domain/              eventos, convites e estados de presença
│   └── infrastructure/      repositórios JPA
├── guests/
│   ├── api/                 endpoints e DTOs
│   ├── application/         cadastro, exclusão e código de acesso
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

Recursos e configurações:

```text
backend/src/main/resources/
├── db/migration/            migrações versionadas do PostgreSQL
├── application.yml          propriedades comuns
├── application-dsv.yml      desenvolvimento local
└── application-prod.yml     produção
```

Fluxo de dependências:

```text
Controller → Service → Repository → Banco
                ↓
          PaymentGateway
```

Controllers validam o contrato HTTP e identificam o usuário autenticado. Services concentram as regras de negócio. Repositórios encapsulam a persistência. Entidades representam o domínio persistido.

O escaneamento de persistência é explícito e separado por tecnologia. `JpaRepositoryConfig` registra somente os repositórios de `events.infrastructure`, `gifts.infrastructure` e `guests.infrastructure`. `MongoRepositoryConfig` registra somente `messages.infrastructure`. As duas configurações ficam inativas no perfil `test`, no qual os repositórios são substituídos por mocks.

## 4. Persistência

### PostgreSQL

Armazena convidados, eventos, convites, presentes e transações de presentes.

#### `guests`

Campos principais:

- `id`: identificador interno;
- `fullName`: nome completo;
- `rsvpCode`: código único usado no login;
- `side`: `BRIDE` ou `GROOM`;
- `email`;
- `phone`.

O convidado representa a identidade compartilhada pelos eventos. O código de acesso e os dados de contato são únicos, enquanto cada confirmação de presença pertence a um convite de evento.

#### `events`

Campos principais:

- `id`;
- `slug`: identificador textual único;
- `type`: `WEDDING` ou `BRIDAL_SHOWER`;
- `title`;
- `description`;
- `eventDateTime`;
- `venueName`;
- `address`;
- `mapUrl`;
- `dressCode`.

A migration cadastra os eventos `casamento` e `cha-de-panelas`. Os campos descritivos, de data, local, mapa e traje aceitam valor nulo.

#### `event_invitations`

Relaciona um convidado a um evento. Campos principais:

- `id`;
- `event_id`;
- `guest_id`;
- `rsvpStatus`: `PENDING`, `CONFIRMED` ou `REJECTED`;
- `respondedAt`: data da resposta;
- `notes`: observação específica daquela resposta.

A combinação entre evento e convidado é única. As chaves estrangeiras usam exclusão em cascata, portanto a remoção do evento ou do convidado também remove os convites relacionados.

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

### Migrações com Flyway

O Flyway é a autoridade responsável pela estrutura do PostgreSQL. O Hibernate usa `ddl-auto: validate`: ele confere se as entidades correspondem ao banco, mas não cria nem altera tabelas.

As migrações ficam em `backend/src/main/resources/db/migration` e seguem o formato:

```text
V<versão>__<descrição>.sql
```

Migrações existentes:

- `V1__create_initial_schema.sql`: cria convidados, presentes, transações, constraints e índices;
- `V2__link_gift_transactions_to_guests.sql`: vincula transações ao `guest_id`;
- `V3__create_events_and_event_rsvps.sql`: cria eventos e convites, transfere o RSVP existente para o casamento e cria o convite pendente do chá de panelas.

O perfil `dsv` usa `baseline-version: 0` e `baseline-on-migrate: true`. Um schema local não vazio e ainda sem histórico do Flyway recebe um registro de baseline na versão `0` antes da execução das migrations. O perfil `prod` lê `FLYWAY_BASELINE_ON_MIGRATE` e usa `false` como valor padrão.

O comando destrutivo `flyway clean` permanece desativado em todos os perfis.

A migração V2 associa registros antigos pelo nome completo. Ela interrompe a inicialização quando encontra convidados homônimos ou uma transação sem convidado correspondente. Essa interrupção protege os dados contra associações silenciosamente incorretas.

A migração V3 preserva `rsvp_status`, `rsvp_by` e `notes` de cada convidado no convite do casamento. Depois da cópia, cria um convite `PENDING` para o chá de panelas e remove essas três colunas de `guests`.

O Flyway armazena o checksum de cada migration aplicada em `flyway_schema_history`. Uma alteração no conteúdo de uma migration já registrada faz a validação falhar. Alterações posteriores de schema são representadas por novos arquivos versionados.

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
POST /api/v1/auth/login
    ↓
JWT com ROLE_GUEST
    ↓
Endpoints autenticados
```

Depois do login, o backend identifica o convidado pelo JWT. Endpoints de RSVP e presentes não aceitam outro código ou nome para selecionar o convidado. Isso impede que um convidado altere dados ou reservas de outro.

A aplicação exclui `UserDetailsServiceAutoConfiguration`. Não é criado usuário em memória, não é emitida senha automática no log e a autenticação HTTP da aplicação é realizada pelo filtro JWT e pelo login Google administrativo.

O JWT possui:

- `subject`: código RSVP ou e-mail administrativo;
- `role`: `ROLE_GUEST` ou `ROLE_ADMIN`;
- `issuer`: configurado por ambiente;
- expiração padrão de duas horas.

### Administradores

O login administrativo recebe um Google ID Token emitido para o cliente configurado. O backend verifica:

1. validade do token;
2. audience correspondente ao `GOOGLE_CLIENT_ID`;
3. e-mail presente em `ADMIN_EMAILS`.

Após a autorização, o backend emite um JWT interno com `ROLE_ADMIN` e o e-mail administrativo como `subject`.

### Política dos endpoints

| Regra de acesso | Endpoints |
|---|---|
| Público | `POST /api/v1/auth/login` |
| Público | `POST /api/v1/admin/auth/google` |
| Público | `GET /api/v1/messages` |
| Público quando habilitado | Swagger e OpenAPI |
| `ROLE_GUEST` | `/api/v1/events/**`, criação de mensagem e operações de reserva/compra |
| JWT válido | Consulta de presentes |
| `ROLE_ADMIN` | `/api/v1/admin/**`, exceto o login Google |

Requisições sem autenticação para recursos protegidos recebem `401`. Acesso a `/api/v1/admin/**` sem `ROLE_ADMIN` recebe `403`. Endpoints que alteram RSVP, reservas e mensagens obtêm o objeto `Guest` do principal autenticado criado para tokens `ROLE_GUEST`.

### CORS

O CORS é aplicado a todas as rotas. As origens são lidas de `CORS_ALLOWED_ORIGINS` como uma lista separada por vírgulas. A configuração rejeita lista vazia e origem curinga `*`.

- métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`;
- headers: `Authorization`, `Content-Type` e `Accept`;
- credenciais: habilitadas;
- cache de preflight: 3.600 segundos.

## 6. API

Por padrão, a aplicação executa em `http://localhost:8080`.

As rotas usam o prefixo versionado `/api/v1`. As listagens de presentes, mensagens e confirmações administrativas retornam um envelope paginado estável. A página começa em zero, o tamanho padrão é 20 e o tamanho máximo aceito é 100.

Formato das respostas paginadas:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Para endpoints autenticados:

```http
Authorization: Bearer <jwt>
```

### 6.1 Login do convidado

#### `POST /api/v1/auth/login`

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
  "fullName": "Convidado Teste"
}
```

O endpoint possui rate limit de 10 tentativas por minuto por combinação de IP e código.

### 6.2 Eventos e RSVP

Os mesmos convidados participam dos dois eventos, mas cada evento possui estado, data de resposta e observação próprios. Dados pessoais de RSVP somente são retornados depois da autenticação.

#### `GET /api/v1/events/my-invitations`

Retorna os convites do convidado autenticado, ordenados pelo ID do evento. Cada item contém:

```json
{
  "eventId": 1,
  "slug": "casamento",
  "type": "WEDDING",
  "title": "Casamento",
  "description": null,
  "eventDateTime": null,
  "venueName": null,
  "address": null,
  "mapUrl": null,
  "dressCode": null,
  "rsvpStatus": "PENDING",
  "respondedAt": null
}
```

#### `GET /api/v1/events/{eventId}/rsvp/me`

Resposta `200`:

```json
{
  "eventId": 1,
  "eventSlug": "casamento",
  "eventTitle": "Casamento",
  "fullName": "Convidado Teste",
  "rsvpStatus": "PENDING",
  "respondedAt": null,
  "email": "guest@example.com",
  "phone": "21999999999",
  "notes": null
}
```

#### `POST /api/v1/events/{eventId}/rsvp/confirm`

#### `POST /api/v1/events/{eventId}/rsvp/decline`

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

E-mail e telefone pertencem ao convidado e, quando informados na resposta, são atualizados para todas as operações posteriores. Estado, data e observações pertencem somente ao convite indicado por `eventId`. Um convidado sem convite para o evento recebe `404`.

### 6.3 Presentes

#### `GET /api/v1/gifts`

Parâmetros opcionais:

- `page`: índice da página, a partir de zero;
- `size`: quantidade de itens, entre 1 e 100;
- `name`: trecho do nome, sem diferenciação entre maiúsculas e minúsculas.

Os presentes são ordenados por `id` crescente. Cada item contém `id`, `name`, `description`, `value`, `quotaValue`, `totalQuotas`, `availableQuotas`, `imageUrl`, `purchaseLink` e `soldOut`.

#### `GET /api/v1/gifts/{id}`

Retorna `200` com os mesmos campos calculados da listagem para o presente identificado. Um identificador inexistente produz `404`.

#### `POST /api/v1/gifts/{id}/reserve`

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

#### `DELETE /api/v1/gifts/{id}/reserve`

Cancela a reserva ativa do convidado autenticado. Um convidado não pode cancelar a reserva de outro.

Resposta de sucesso: `204 No Content`.

#### `POST /api/v1/gifts/{id}/buy`

Compra as cotas previamente reservadas pelo convidado autenticado. Uma reserva expirada não pode ser comprada.

O gateway atual é simulado e não realiza cobrança real.

Resposta de sucesso: `204 No Content`.

### 6.4 Mensagens

#### `GET /api/v1/messages`

Aceita `page` e `size` com as mesmas regras da listagem de presentes e o filtro opcional `author`, que busca um trecho do autor sem diferenciar maiúsculas e minúsculas. As mensagens são ordenadas por `sendDate` e `id`, ambos decrescentes.

#### `POST /api/v1/messages`

Requer convidado autenticado.

```json
{
  "text": "Felicidades ao casal!"
}
```

O autor é obtido do convidado autenticado. A mensagem é obrigatória e aceita até 1.000 caracteres.

Retorna `200` com a mensagem persistida, incluindo `id`, `author`, `text` e `sendDate`.

### 6.5 Administração de convidados

#### `POST /api/v1/admin/guests/register`

```json
{
  "fullName": "Nome do Convidado",
  "side": "BRIDE",
  "email": "guest@example.com",
  "phone": "21999999999"
}
```

O código RSVP é gerado com três letras normalizadas do nome e quatro dígitos aleatórios. A aplicação tenta evitar colisões antes de salvar, e o banco também possui restrição única.

O cadastro cria um convite `PENDING` para cada evento existente.

Retorna `201 Created` com `fullName`, `rsvpCode`, `side`, `email` e `phone`.

#### `DELETE /api/v1/admin/guests/{id}/delete`

Exclui um convidado existente.

Resposta de sucesso: `204 No Content`.

### 6.6 Administração de confirmações

#### `GET /api/v1/admin/events/{eventId}/rsvps`

Lista os convites do evento em um envelope paginado. Parâmetros opcionais:

- `page`: índice da página, a partir de zero;
- `size`: quantidade de itens, entre 1 e 100;
- `name`: trecho do nome, sem diferenciação entre maiúsculas e minúsculas;
- `status`: `PENDING`, `CONFIRMED` ou `REJECTED`;
- `side`: `BRIDE` ou `GROOM`.

Os filtros podem ser combinados. A ordenação é feita pelo nome do convidado, sem diferenciação entre maiúsculas e minúsculas, e depois pelo ID do convidado.

Cada item contém `guestId`, `fullName`, `side`, `email`, `phone`, `rsvpStatus`, `respondedAt` e `notes`.

#### `GET /api/v1/admin/events/{eventId}/rsvps/summary`

Retorna as contagens de presença do evento:

```json
{
  "eventId": 1,
  "eventTitle": "Casamento",
  "total": 3,
  "pending": 1,
  "confirmed": 1,
  "rejected": 1
}
```

As duas rotas retornam `404` quando o evento não existe.

### 6.7 Administração de presentes

#### `POST /api/v1/admin/gifts/register`

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

Retorna `201 Created` com os dados persistidos e o valor calculado por cota.

#### `DELETE /api/v1/admin/gifts/{id}/delete`

Exclui um presente existente e suas transações associadas.

Resposta de sucesso: `204 No Content`.

### 6.8 Login administrativo

#### `POST /api/v1/admin/auth/google`

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

Resposta `200`:

```json
{
  "token": "jwt"
}
```

## 7. Erros HTTP

Erros tratados pelo backend seguem o formato:

```json
{
  "timestamp": "2026-07-21T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Descrição do erro",
  "path": "/api/v1/example"
}
```

Mapeamentos principais:

| Status | Situação                                   |
| ------ | ------------------------------------------ |
| `400`  | validação ou argumento inválido            |
| `401`  | autenticação ausente ou token inválido     |
| `403`  | usuário autenticado sem permissão          |
| `404`  | evento, convite, convidado, presente ou recurso inexistente |
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

`PaymentGateway` é a interface consumida por `GiftService.buy`. A implementação registrada no contexto é `MockPaymentGateway`.

Ao processar uma compra, o mock registra no log o convidado, o presente, a quantidade de cotas e o valor calculado. Em seguida, aguarda 1,5 segundo e retorna sem chamar um serviço externo. Após o retorno, a transação passa de `RESERVED` para `PURCHASED`, recebe `purchasedAt` e perde o prazo de reserva.

Não existem integração financeira externa, cobrança, webhook ou identificador de pagamento na implementação atual.

## 10. Rate limiter

O `PublicEndpointRateLimiter` utiliza janelas fixas armazenadas em um `ConcurrentHashMap` no processo da aplicação. No login RSVP, a chave combina a ação `auth-login`, o IP do cliente e o código RSVP normalizado. O limite configurado pelo controller é de dez requisições por minuto para cada chave.

O IP é resolvido nesta ordem: primeiro valor de `X-Forwarded-For`, `X-Real-IP` e endereço remoto da requisição. O estado não é compartilhado entre processos e é descartado quando a aplicação reinicia. As chaves permanecem no mapa e são substituídas quando recebem uma nova requisição após o término da janela.

## 11. Configuração e execução

### Perfis

Quando nenhum perfil é informado, o backend ativa `dsv` como perfil padrão. `SPRING_PROFILES_ACTIVE` substitui essa seleção:

| Perfil | Finalidade |
|---|---|
| `dsv` | desenvolvimento local, bancos do Docker Compose, SQL detalhado e Swagger ativo |
| `test` | testes automatizados, com persistência substituída por mocks no teste de contexto |
| `integration` | testes com PostgreSQL e MongoDB descartáveis gerenciados pelo Testcontainers |
| `prod` | conexões externas, logs reduzidos, CORS obrigatório e Swagger desativado por padrão |

`application.yml` contém as propriedades comuns e define o perfil padrão. `application-dsv.yml` e `application-prod.yml` contêm as propriedades específicas dos respectivos ambientes. O arquivo `application-integration.yml` pertence ao classpath de testes.

### Variáveis de ambiente

`backend/.env.example` contém o modelo das variáveis usadas no ambiente local. `backend/.env` é ignorado pelo Git e é carregado pela dependência `springboot4-dotenv`.

```dotenv
SPRING_PROFILES_ACTIVE=dsv

POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=

MONGO_ROOT_USERNAME=
MONGO_ROOT_PASSWORD=
MONGO_DATABASE=

JWT_SECRET=
JWT_ISSUER=
JWT_EXPIRATION_HOURS=2

GOOGLE_CLIENT_ID=
ADMIN_EMAILS=

CORS_ALLOWED_ORIGINS=http://localhost:4200
SERVER_PORT=8080
```

- `ADMIN_EMAILS`: lista de e-mails administrativos separada por vírgulas;
- `CORS_ALLOWED_ORIGINS`: lista de origens explícitas separada por vírgulas; o valor `*` é rejeitado;
- `JWT_SECRET`: chave usada pelo algoritmo HMAC256 para assinar e verificar os JWTs;
- `JWT_ISSUER`: emissor exigido durante a validação dos JWTs;
- `JWT_EXPIRATION_HOURS`: duração dos JWTs, com padrão de duas horas;
- `SERVER_PORT`: porta HTTP, com padrão `8080`.

No perfil `prod`, a persistência usa:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://host:5432/database
DATABASE_USERNAME=
DATABASE_PASSWORD=
DATABASE_MAX_POOL_SIZE=10
DATABASE_MIN_IDLE=2
MONGODB_URI=mongodb://user:password@host:27017/database
CORS_ALLOWED_ORIGINS=https://example.com
SWAGGER_ENABLED=false
ROOT_LOG_LEVEL=INFO
FLYWAY_BASELINE_ON_MIGRATE=false
```

### Serviços locais com Docker Compose

O arquivo `backend/docker-compose.yml` define PostgreSQL 16 e MongoDB 7. A inicialização ocorre com:

```bash
docker compose up -d
```

Serviços locais:

- PostgreSQL: porta `5432`;
- MongoDB: porta `27017`.

Os volumes `postgres_data` e `mongo_data` preservam os dados entre reinicializações.

### Inicialização da aplicação

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

Na inicialização, o Flyway valida o histórico e aplica migrations pendentes. Em seguida, o Hibernate valida o mapeamento das entidades com `ddl-auto: validate`. No perfil `dsv`, os seeders consultam as tabelas e inserem dados somente quando elas estão vazias.

### Swagger

No perfil `dsv`, com a aplicação em execução:

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

No perfil `prod`, `SWAGGER_ENABLED` controla simultaneamente a geração OpenAPI e a interface Swagger, com valor padrão `false`.

## 12. Testes

Executar somente os testes unitários e de camada web:

```bash
cd backend
./mvnw test
```

Executar a verificação completa, incluindo testes de integração:

```bash
cd backend
./mvnw verify
```

Os testes de integração exigem acesso direto do processo Maven ao Docker; `docker info` precisa executar sem `sudo`. O Testcontainers inicia PostgreSQL 16 e MongoDB 7 em portas aleatórias e remove os containers ao final. As URLs temporárias são injetadas por `DynamicPropertySource`, sem utilizar os bancos configurados no `.env`.

O Maven Surefire executa os testes rápidos durante a fase `test`. O Maven Failsafe executa as classes `*IntegrationTest` nas fases `integration-test` e `verify`. O Mockito é carregado como `-javaagent` nos dois executores, evitando a dependência do mecanismo de auto-attach da JVM.

O workflow `.github/workflows/backend-ci.yml` executa `./mvnw clean verify` em Java 21 para eventos de `push` e `pull_request`. O job usa um runner Linux com Docker disponível para o Testcontainers e cache local das dependências Maven.

### Cobertura existente por área

| Suíte                           | Responsabilidade                                         |
| ------------------------------- | -------------------------------------------------------- |
| `MambaWeddingApplicationTests`  | carregamento completo do contexto sem bancos externos    |
| `AdminAuthControllerTest`       | validação do contrato do login Google                    |
| `TokenServiceTest`              | geração, claims, issuer e token inválido                 |
| `SecurityFilterTest`            | autenticação de convidado/admin e tokens impróprios      |
| `PublicEndpointRateLimiterTest` | limite, bloqueio e IP encaminhado                        |
| `SecurityConfigTest`            | origens permitidas e rejeição de CORS curinga            |
| `AuthControllerTest`            | login, resposta, código inválido e rate limit            |
| `EventRsvpControllerTest`       | convites, RSVP por evento e autorização administrativa   |
| `EventRsvpServiceTest`          | consulta, resposta, listagem e resumo por evento          |
| `GuestRsvpServiceTest`          | cadastro, exclusão, código e criação dos convites         |
| `GuestControllerTest`           | autorização dos endpoints administrativos                |
| `GiftControllerTest`            | administração, paginação e filtro de presentes           |
| `GuestGiftControllerTest`       | identidade em reserva, compra e cancelamento             |
| `MessageControllerTest`         | paginação, filtro por autor e validação dos parâmetros   |
| `GiftServiceTest`               | cotas, duplicidade, expiração, compra e limpeza agendada |
| `GiftTest`                      | cálculo de disponibilidade e esgotamento                 |

O teste de contexto usa mocks para os repositórios e desabilita apenas as autoconfigurações de persistência. Assim, valida controllers, services, segurança, scheduling e demais beans sem exigir PostgreSQL ou MongoDB em execução.

### Testes de integração

| Suíte | Responsabilidade |
|---|---|
| `MigrationIntegrationTest` | aplica e valida as migrations em schema vazio; repete a execução sem reaplicar versões; migra um schema legado; preserva o vínculo da reserva e transfere o RSVP existente para o casamento |
| `PostgresFlowIntegrationTest` | executa login, JWT, contratos `401`/`403`, convites e RSVP independentes; valida listagem, filtros e resumo administrativos; pagina e filtra presentes; associa reserva ao convidado autenticado; disputa concorrente da última cota; cancela reserva expirada |
| `MongoMessageIntegrationTest` | persiste mensagens no MongoDB real e valida paginação, filtro por autor e ordenação da mais recente para a mais antiga |

O perfil `integration` existe apenas no classpath de testes, em `src/test/resources/application-integration.yml`. Ele desativa Swagger e logs SQL detalhados, usa segredos fictícios e recebe as conexões temporárias dinamicamente do Testcontainers.

## 13. Dados de desenvolvimento

No perfil `dsv`, seeders inserem convidados e presentes quando as respectivas tabelas estão vazias. Para cada convidado inserido, o seeder cria um convite `PENDING` para todos os eventos cadastrados.

- os seeders não executam no perfil de teste;
- o perfil `dsv` é usado por padrão quando nenhum perfil ativo é informado;
- dados de seed são apenas para desenvolvimento.

## 14. Fluxo de consumo do RSVP

O JWT é a fonte de identidade do convidado em todas as operações posteriores ao login.

O fluxo HTTP é:

1. `POST /api/v1/auth/login` recebe o código RSVP e retorna JWT e nome;
2. requisições protegidas enviam `Authorization: Bearer <jwt>`;
3. `GET /api/v1/events/my-invitations` retorna os eventos e seus estados independentes;
4. `GET /api/v1/events/{eventId}/rsvp/me` retorna o RSVP daquele convite;
5. confirmação e recusa recebem somente os dados editáveis de contato e observações;
6. o backend obtém o `rsvpCode` e o identificador do convidado a partir do principal autenticado, não do corpo das operações posteriores;
7. a alteração de um convite não altera o estado dos demais eventos.
