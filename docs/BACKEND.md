# Mamba Wedding Backend — Referência técnica

## 1. Visão geral

Este documento descreve a implementação atual do backend Mamba Wedding: organização do código, persistência, autenticação, contratos HTTP, regras de negócio, configuração e testes automatizados.

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

### Migrações com Flyway

O Flyway é a autoridade responsável pela estrutura do PostgreSQL. O Hibernate usa `ddl-auto: validate`: ele confere se as entidades correspondem ao banco, mas não cria nem altera tabelas.

As migrações ficam em `backend/src/main/resources/db/migration` e seguem o formato:

```text
V<versão>__<descrição>.sql
```

Migrações existentes:

- `V1__create_initial_schema.sql`: cria convidados, presentes, transações, constraints e índices;
- `V2__link_gift_transactions_to_guests.sql`: vincula transações ao `guest_id`.

O perfil `dsv` usa `baseline-version: 0` e `baseline-on-migrate: true`. Um schema local não vazio e ainda sem histórico do Flyway recebe um registro de baseline na versão `0` antes da execução das migrations. O perfil `prod` lê `FLYWAY_BASELINE_ON_MIGRATE` e usa `false` como valor padrão.

O comando destrutivo `flyway clean` permanece desativado em todos os perfis.

A migração V2 associa registros antigos pelo nome completo. Ela interrompe a inicialização quando encontra convidados homônimos ou uma transação sem convidado correspondente. Essa interrupção protege os dados contra associações silenciosamente incorretas.

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

O login administrativo recebe um Google ID Token emitido para o cliente configurado. O backend verifica:

1. validade do token;
2. audience correspondente ao `GOOGLE_CLIENT_ID`;
3. e-mail presente em `ADMIN_EMAILS`.

Após a autorização, o backend emite um JWT interno com `ROLE_ADMIN` e o e-mail administrativo como `subject`.

### Política dos endpoints

| Regra de acesso | Endpoints |
|---|---|
| Público | `POST /api/auth/login` |
| Público | `POST /api/admin/auth/google` |
| Público | `GET /api/messages` |
| Público quando habilitado | Swagger e OpenAPI |
| JWT válido | Endpoints não públicos de RSVP, presentes e mensagens |
| `ROLE_ADMIN` | `/api/admin/**`, exceto o login Google |

Requisições sem autenticação para recursos protegidos recebem `401`. Acesso a `/api/admin/**` sem `ROLE_ADMIN` recebe `403`. Endpoints que alteram RSVP, reservas e mensagens obtêm o objeto `Guest` do principal autenticado criado para tokens `ROLE_GUEST`.

### CORS

O CORS é aplicado a todas as rotas. As origens são lidas de `CORS_ALLOWED_ORIGINS` como uma lista separada por vírgulas. A configuração rejeita lista vazia e origem curinga `*`.

- métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`;
- headers: `Authorization`, `Content-Type` e `Accept`;
- credenciais: habilitadas;
- cache de preflight: 3.600 segundos.

## 6. API

Por padrão, a aplicação executa em `http://localhost:8080`.

As rotas usam o prefixo `/api` sem segmento de versão. As listagens de presentes e mensagens retornam a coleção completa em um array JSON e não recebem parâmetros de paginação, filtro ou ordenação. A ordem das mensagens é definida pelo backend; a listagem de presentes não declara uma ordenação no contrato.

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

Retorna `200` com uma lista de presentes. Cada item contém `id`, `name`, `description`, `value`, `quotaValue`, `totalQuotas`, `availableQuotas`, `imageUrl`, `purchaseLink` e `soldOut`.

#### `GET /api/gifts/{id}`

Retorna `200` com os mesmos campos calculados da listagem para o presente identificado. Um identificador inexistente produz `404`.

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

Resposta de sucesso: `204 No Content`.

#### `POST /api/gifts/{id}/buy`

Compra as cotas previamente reservadas pelo convidado autenticado. Uma reserva expirada não pode ser comprada.

O gateway atual é simulado e não realiza cobrança real.

Resposta de sucesso: `204 No Content`.

### 6.4 Mensagens

#### `GET /api/messages`

Retorna `200` com a lista pública de mensagens em ordem decrescente de `sendDate`.

#### `POST /api/messages`

Requer convidado autenticado.

```json
{
  "text": "Felicidades ao casal!"
}
```

O autor é obtido do convidado autenticado. A mensagem é obrigatória e aceita até 1.000 caracteres.

Retorna `200` com a mensagem persistida, incluindo `id`, `author`, `text` e `sendDate`.

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

Retorna `201 Created` com `fullName`, `rsvpCode`, `side`, `email` e `phone`.

#### `DELETE /api/admin/guests/{id}/delete`

Exclui um convidado existente.

Resposta de sucesso: `204 No Content`.

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

Retorna `201 Created` com os dados persistidos e o valor calculado por cota.

#### `DELETE /api/admin/gifts/{id}/delete`

Exclui um presente existente e suas transações associadas.

Resposta de sucesso: `204 No Content`.

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
| `GuestRsvpControllerTest`       | identidade autenticada, `/me`, confirmação e recusa      |
| `GuestRsvpServiceTest`          | consulta por ID, atualização e geração do código         |
| `GuestControllerTest`           | autorização dos endpoints administrativos                |
| `GiftControllerTest`            | autorização administrativa de presentes                  |
| `GuestGiftControllerTest`       | identidade em reserva, compra e cancelamento             |
| `GiftServiceTest`               | cotas, duplicidade, expiração, compra e limpeza agendada |
| `GiftTest`                      | cálculo de disponibilidade e esgotamento                 |

O teste de contexto usa mocks para os quatro repositórios e desabilita apenas as autoconfigurações de persistência. Assim, valida controllers, services, segurança, scheduling e demais beans sem exigir PostgreSQL ou MongoDB em execução.

### Testes de integração

| Suíte | Responsabilidade |
|---|---|
| `MigrationIntegrationTest` | aplica e valida as migrations em schema vazio; repete a execução sem reaplicar versões; migra um schema legado e preserva o vínculo da reserva com o convidado |
| `PostgresFlowIntegrationTest` | executa login, JWT, consulta e confirmação de RSVP; associa reserva ao convidado autenticado; disputa concorrente da última cota; cancelamento de reserva expirada |
| `MongoMessageIntegrationTest` | persiste mensagens no MongoDB real e valida a ordenação da mais recente para a mais antiga |

O perfil `integration` existe apenas no classpath de testes, em `src/test/resources/application-integration.yml`. Ele desativa Swagger e logs SQL detalhados, usa segredos fictícios e recebe as conexões temporárias dinamicamente do Testcontainers.

## 13. Dados de desenvolvimento

No perfil `dsv`, seeders inserem convidados e presentes quando as respectivas tabelas estão vazias.

- os seeders não executam no perfil de teste;
- o perfil `dsv` é usado por padrão quando nenhum perfil ativo é informado;
- dados de seed são apenas para desenvolvimento.

## 14. Fluxo de consumo do RSVP

O JWT é a fonte de identidade do convidado em todas as operações posteriores ao login.

O fluxo HTTP é:

1. `POST /api/auth/login` recebe o código RSVP e retorna JWT, nome e status;
2. requisições protegidas enviam `Authorization: Bearer <jwt>`;
3. `GET /api/rsvp/me` retorna os dados do convidado identificado pelo token;
4. confirmação e recusa recebem somente os dados editáveis de contato e observações;
5. o backend obtém o `rsvpCode` e o identificador do convidado a partir do principal autenticado, não do corpo das operações posteriores.
