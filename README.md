
## API Cadastro de Usuário: Spring Boot, OAuth2 (Google), JWT e MongoDB

> **Spring Boot 4.0.9 · Java 27 · MongoDB 8 · Spring Security (JWT + OAuth2 Google)**

---

## 1. Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT (Browser / App)                         │
└────────┬──────────────────────────┬──────────────────────────┬──────────────┘
         │ POST /api/auth/register  │ POST /api/auth/login     │ GET /api/users/me
         │ POST /api/auth/login     │                          │ (Bearer JWT)
         ▼                          ▼                          ▼
  ┌──────────────────┐  ┌─────────────────┐  ┌──────────────────────────────────┐
  │  AuthController  │  │  OAuth2 Flow    │  │        UserController            │
  │  /api/auth/**    │  │  /oauth2/**     │  │        /api/users/**             │
  └────────┬─────────┘  └───────┬─────────┘  └──────────────┬───────────────────┘
           │                    │                           │
           ▼                    ▼                           │
┌──────────────────┐  ┌──────────────────────┐              │
│   AuthService    │  │ OAuth2SuccessHandler │              │
│  register/login  │  │  onAuthSuccess       │              │
└───┬──────┬───────┘  └───┬──────┬───────────┘              │
    │      │              │      │                          │
    │      ▼              │      ▼                          │
    │  ┌──────────┐       │  ┌──────────┐                   │
    │  │JwtService│       │  │JwtService│                   │
    │  │ generate │       │  │ generate │                   │
    │  └──────────┘       │  └──────────┘                   │
    ▼                     ▼                                 ▼
┌───────────────────────────────────────────────────────────────────┐
│                       UserRepository                              │
│          MongoRepository<User, String>                            │
└───────────────────────────────┬───────────────────────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │   MongoDB (users)     │
                    │   mongo:8 / :27017    │
                    └───────────────────────┘
```

---

## 2. Diagrama de Classes (UML)

### 2.1 Camada Model

```
┌───────────────────────────────────────────────────────────────┐
│                      <<Document>> User                        │
│ collection = "users"                                          │
├───────────────────────────────────────────────────────────────┤
│ - id : String                        «@Id»                    │
│ - name : String                                               │
│ - email : String                     «@Indexed(unique=true)»  │
│ - passwordHash : String              // null p/ contas Google │
│ - provider : AuthProvider                                     │
│ - providerId : String                // "sub" do Google       │
│ - roles : Set<String>                                         │
│ - createdAt : Instant                                         │
├───────────────────────────────────────────────────────────────┤
│ # User()                             // construtor protegido  │
│ + User(name, email, passwordHash,                             │
│        provider, providerId, roles)                           │
│ + getId() : String                                            │
│ + getName() : String                                          │
│ + getEmail() : String                                         │
│ + getPasswordHash() : String                                  │
│ + getProvider() : AuthProvider                                │
│ + getProviderId() : String                                    │
│ + getRoles() : Set<String>                                    │
│ + getCreatedAt() : Instant                                    │
└───────────────────────────────────────────────────────────────┘
                         │
                         │ usa ▼
                ┌────────────────────┐
                │ <<enum>>           │
                │ AuthProvider       │
                ├────────────────────┤
                │  LOCAL             │
                │  GOOGLE            │
                └────────────────────┘
```

### 2.2 Camada DTO

```
┌──────────────────────────────────┐    ┌──────────────────────────────────────┐
│ <<record>> RegisterRequest       │    │ <<record>> LoginRequest              │
├──────────────────────────────────┤    ├──────────────────────────────────────┤
│ + name : String     @NotBlank    │    │ + email : String   @NotBlank @Email  │
│ + email : String    @NotBlank    │    │ + password : String @NotBlank        │
│ + password : String @NotBlank    │    └──────────────────────────────────────┘
│                     @Size(min=8) │
└──────────────────────────────────┘

┌──────────────────────────────────┐    ┌──────────────────────────────────────┐
│ <<record>> UserResponse          │    │ <<record>> AuthTokenResponse         │
├──────────────────────────────────┤    ├──────────────────────────────────────┤
│ + id : String                    │    │ + accessToken : String               │
│ + name : String                  │    │ + tokenType : String                 │
│ + email : String                 │    │ + expiresIn : long                   │
│ + provider : AuthProvider        │    └──────────────────────────────────────┘
└──────────────────────────────────┘
```

### 2.3 Camada Repository

```
┌────────────────────────────────────────────────────────────────┐
│           <<interface>> MongoRepository<User, String>          │
│                    (Spring Data MongoDB)                       │
└──────────────────────────────┬─────────────────────────────────┘
                               △
                               │ extends
┌──────────────────────────────┴─────────────────────────────────┐
│               <<interface>> UserRepository                     │
├────────────────────────────────────────────────────────────────┤
│ + findByEmail(email: String) : Optional<User>                  │
│ + existsByEmail(email: String) : boolean                       │
│ + findByProviderAndProviderId(provider: AuthProvider,          │
│                               providerId: String)              │
│       : Optional<User>                                         │
└────────────────────────────────────────────────────────────────┘
```

### 2.4 Camada Service

```
┌───────────────────────────────────────────────────────┐
│                <<Service>> AuthService                │
├───────────────────────────────────────────────────────┤
│ - users : UserRepository                              │
│ - encoder : PasswordEncoder                           │
│ - jwtService : JwtService                             │
├───────────────────────────────────────────────────────┤
│ + register(req: RegisterRequest) : UserResponse       │
│ + login(req: LoginRequest) : AuthTokenResponse        │
└───────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────┐
│                <<Service>> JwtService                 │
├───────────────────────────────────────────────────────┤
│ - encoder : JwtEncoder                                │
│ - issuer : String          @Value("app.jwt.issuer")   │
│ - expirationMinutes : long @Value("app.jwt.exp...")   │
├───────────────────────────────────────────────────────┤
│ + generate(user: User) : AuthTokenResponse            │
└───────────────────────────────────────────────────────┘
```

### 2.5 Camada Controller

```
┌──────────────────────────────────────────────────────────────────┐
│              <<RestController>> AuthController                   │
│              @RequestMapping("/api/auth")                        │
├──────────────────────────────────────────────────────────────────┤
│ - authService : AuthService                                      │
├──────────────────────────────────────────────────────────────────┤
│ + register(@Valid req: RegisterRequest) : UserResponse           │
│       @PostMapping("/register") → 201 CREATED                    │
│ + login(@Valid req: LoginRequest) : AuthTokenResponse            │
│       @PostMapping("/login") → 200 OK                            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│              <<RestController>> UserController                   │
│              @RequestMapping("/api/users")                       │
├──────────────────────────────────────────────────────────────────┤
│ - users : UserRepository                                         │
├──────────────────────────────────────────────────────────────────┤
│ + me(@AuthenticationPrincipal jwt: Jwt) : UserResponse           │
│       @GetMapping("/me") → 200 OK                                │
└──────────────────────────────────────────────────────────────────┘
```

### 2.6 Camada Config & Security

```
┌────────────────────────────────────────────────────────┐
│             <<Configuration>> JwtConfig                │
├────────────────────────────────────────────────────────┤
│ + jwtSecretKey(secret: String) : SecretKey      @Bean  │
│ + jwtEncoder(key: SecretKey) : JwtEncoder       @Bean  │
│ + jwtDecoder(key: SecretKey, issuer) : JwtDecoder      │
│                                                 @Bean  │
└────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│      <<Configuration>> SecurityConfig                            │
│      @EnableWebSecurity                                          │
├──────────────────────────────────────────────────────────────────┤
│ + passwordEncoder() : PasswordEncoder (BCrypt)           @Bean   │
│ + filterChain(http, successHandler, clients)                     │
│       : SecurityFilterChain                              @Bean   │
│ - jwtAuthenticationConverter() : JwtAuthenticationConverter      │
└──────────────────────────────────────────────────────────────────┘

                  ┌──────────────────────────────────────────────┐
                  │   AuthenticationSuccessHandler               │
                  │          <<interface>>                       │
                  └───────────────────┬──────────────────────────┘
                                      △
                                      │ implements
┌─────────────────────────────────────┴──────────────────────────────────┐
│            <<Component>> OAuth2SuccessHandler                          │
├────────────────────────────────────────────────────────────────────────┤
│ - users : UserRepository                                               │
│ - jwtService : JwtService                                              │
├────────────────────────────────────────────────────────────────────────┤
│ + onAuthenticationSuccess(request, response, authentication) : void    │
│   → Extrai email/name/sub do OAuth2User                                │
│   → Cria ou busca User no MongoDB                                      │
│   → Gera JWT e retorna como JSON                                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Diagrama de Relacionamentos (Dependências)

Todas as dependências são injetadas via construtor (sem `@Autowired`).

```
AuthController ──────────────► AuthService
                                   │
                                   ├──► UserRepository
                                   ├──► PasswordEncoder (BCrypt)
                                   └──► JwtService
                                            │
                                            ├──► JwtEncoder (Nimbus)
                                            └──► @Value props

UserController ──────────────► UserRepository

OAuth2SuccessHandler ────────► UserRepository
                     ────────► JwtService

SecurityConfig ──────────────► OAuth2SuccessHandler
               ──────────────► ClientRegistrationRepository (Google)

JwtConfig ───────────────────► @Value("app.jwt.secret")
          ───────────────────► @Value("app.jwt.issuer")
```

---
## 4. Diagrama de Pacotes

```
com.register.api
├── ApiApplication                          ← @SpringBootApplication (entry point)
│
├── config/
│   ├── JwtConfig                           ← @Configuration: SecretKey, JwtEncoder, JwtDecoder
│   └── SecurityConfig                      ← @Configuration @EnableWebSecurity: FilterChain, BCrypt
│
├── controller/
│   ├── AuthController                      ← @RestController: /api/auth (register, login)
│   └── UserController                      ← @RestController: /api/users (me)
│
├── dto/
│   ├── request/
│   │   ├── LoginRequest                    ← record (email, password)
│   │   └── RegisterRequest                 ← record (name, email, password)
│   └── response/
│       ├── AuthTokenResponse               ← record (accessToken, tokenType, expiresIn)
│       └── UserResponse                    ← record (id, name, email, provider)
│
├── model/
│   ├── AuthProvider                        ← enum { LOCAL, GOOGLE }
│   └── User                                ← @Document("users") — entidade MongoDB
│
├── repository/
│   └── UserRepository                      ← interface extends MongoRepository<User, String>
│
├── security/
│   └── OAuth2SuccessHandler                ← @Component implements AuthenticationSuccessHandler
│
└── service/
    ├── AuthService                         ← @Service: registro local, login local
    └── JwtService                          ← @Service: geração de JWT (HS256)
```

---

## 5. Endpoints da API

| Método | Endpoint                        | Auth?  | Request Body       | Response Body       | Status       |
|--------|---------------------------------|--------|--------------------|---------------------|--------------|
| POST   | `/api/auth/register`            | Não    | `RegisterRequest`  | `UserResponse`      | 201 Created  |
| POST   | `/api/auth/login`               | Não    | `LoginRequest`     | `AuthTokenResponse` | 200 OK       |
| GET    | `/api/users/me`                 | JWT    | —                  | `UserResponse`      | 200 OK       |
| GET    | `/oauth2/authorization/google`  | Não    | —                  | Redirect → Google   | 302          |
| GET    | `/login/oauth2/code/google`     | Não    | —                  | `AuthTokenResponse` | 200 OK       |

---

## 6. Infraestrutura (Docker Compose)

```
┌──────────────────────────────┐         ┌──────────────────────────────┐
│   Spring Boot App            │         │   MongoDB 8                  │
│   (api-cadastro-usuario)     │────────►│   container: mongo-users     │
│   :8080                      │  :27017 │   database: userdb           │
│                              │         │   volume: mongo-users-data   │
└──────────────────────────────┘         └──────────────────────────────┘
```

---

## 7. Fluxo de Autenticação Dual (Resumo Visual)

```
                            ┌──────────────────┐
                            │     CLIENT       │
                            └────────┬─────────┘
                                     │
                        ┌────────────┴────────────┐
                        ▼                         ▼
              ┌───────────────────┐    ┌────────────────────┐
              │ Registro/Login    │    │   Google OAuth2    │
              │ (email + senha)   │    │   (PKCE flow)      │
              └────────┬──────────┘    └────────┬───────────┘
                       │                        │
                       ▼                        ▼
              ┌───────────────────┐    ┌────────────────────┐
              │ AuthService       │    │OAuth2SuccessHandler│
              │ BCrypt verify     │    │ cria/busca User    │
              └────────┬──────────┘    └────────┬───────────┘
                       │                        │
                       └────────┬───────────────┘
                                │
                                ▼
                       ┌────────────────┐
                       │   JwtService   │
                       │   HS256 sign   │
                       └────────┬───────┘
                                │
                                ▼
                       ┌────────────────┐
                       │  JWT Token     │
                       │  Bearer header │
                       └────────┬───────┘
                                │
                                ▼
                       ┌────────────────────┐
                       │ Rotas protegidas   │
                       │ /api/users/me      │
                       │ (JwtDecoder valida)│
                       └────────────────────┘
```

Ambos os caminhos convergem no `JwtService` — o token gerado é idêntico independente de a autenticação ser local ou via Google.
O `SecurityFilterChain` valida o Bearer JWT em todas as rotas protegidas usando o `JwtDecoder` configurado em `JwtConfig`.
