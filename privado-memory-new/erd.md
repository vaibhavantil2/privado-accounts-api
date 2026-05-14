# Engineering Doc

## System overview
The **Accounts Service** is a Spring Boot application that provides user‑account functionality for a web front‑end. It exposes REST endpoints for profile retrieval, login, signup, and a health‑check ping. The service persists user and session data in a relational database, logs business events to an internal analytics endpoint, and notifies external services (SendGrid for email and Slack for messaging). Asynchronous jobs are used for event logging and (potential) notification delivery. The application runs on Java 11 inside a container that is built and published via the Maven CI workflow.

## Tech stack & runtime
| Component | Technology / Library | Source |
|-----------|----------------------|--------|
| Language | Java 11 | `src/main/java/ai/privado/demo/accounts/AccountsApplication.java:1‑31` |
| Framework | Spring Boot (Web, Data JPA, Transaction Management) | `src/main/java/ai/privado/demo/accounts/AccountsApplication.java:1‑31` |
| ORM | Spring Data JPA (repositories `UserRepository`, `SessionsR`) | `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:31‑38` |
| JSON | Jackson `ObjectMapper` | `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55‑58` |
| Async execution | `ExecutorService` bean `apiCallerExecutor` (fixed‑size thread pool) | `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55‑60` |
| Datastores | Relational DB tables `users` (`UserE`) and `sessions` (`SessionE`) | `src/main/java/ai/privado/demo/accounts/service/entity/UserE.java:1‑31`, `src/main/java/ai/privado/demo/accounts/service/entity/SessionE.java:7‑15` |
| External HTTP services | Analytics endpoint (`https://localhost/analytics/events`), SendGrid API, Slack webhook | `src/main/java/ai/privado/demo/accounts/apistubs/DataLoggerS.java:24‑27`, `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86‑101`, `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103‑115` |
| HTTP client library | Unirest (analytics) | `src/main/java/ai/privado/demo/accounts/apistubs/DataLoggerS.java:24‑27` |
| Email SDK | SendGrid Java SDK | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86‑101` |
| Slack SDK | Slack Java SDK | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103‑115` |
| Optional third‑party SDKs (present but not invoked) | AWS S3 SDK (`S3stub`), Mixpanel & Segment SDKs (`AnalyticsStub`) | `src/main/java/ai/privado/demo/accounts/thirdparty/S3stub.java:14‑32`, `src/main/java/ai/privado/demo/accounts/thirdparty/AnalyticsStub.java:26‑35` |

## Ingress catalog
| ID | Name | Mechanism | Trigger | Source |
|----|------|-----------|---------|--------|
| IN-1 | Get User Profile | HTTP GET | `GET /api/user/{sessionid}` | `src/main/java/ai/privado/demo/accounts/service/controller/ProfileService.java:13‑14` |
| IN-2 | User Login | HTTP POST | `POST /api/public/user/authenticate` | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:43` |
| IN-3 | User Signup | HTTP POST | `POST /api/public/user/signup` | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:72‑73` |
| IN-4 | Health‑check Ping | HTTP GET | `GET /api/public/ping` | `src/main/java/ai/privado/demo/accounts/service/controller/Ping.java:5‑8` |
| IN-5 | Event logging job | Async `Runnable` | `EventJobRun` submitted to `apiCallerExecutor` (triggered from profile retrieval) | `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:7‑12` |
| IN-6 | Email sending job (unused) | Async `Runnable` | `SGSendMailJobRun` (instantiated but never submitted) | `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:23‑27` |
| IN-7 | Slack sending job (unused) | Async `Runnable` | `SlackSendJobRun` (instantiated but never submitted) | `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:9‑13` |
| IN-8 | S3 upload (unused) | Direct method call | `S3stub.uploadFile` (public API, not referenced elsewhere) | `src/main/java/ai/privado/demo/accounts/thirdparty/S3stub.java:21‑32` |
| IN-9 | Analytics SDK call (unused) | Direct method call | `AnalyticsStub.trackEvent` (public API, not referenced elsewhere) | `src/main/java/ai/privado/demo/accounts/thirdparty/AnalyticsStub.java:26` |

## Egress catalog
| ID | Destination | Type | Purpose | Source |
|----|------------|------|---------|--------|
| OUT-1 | `users` table (`UserE`) | Database | Persist new user records (signup) | `src/main/java/ai/privado/demo/accounts/service/entity/UserE.java:1‑31` |
| OUT-2 | `sessions` table (`SessionE`) | Database | Persist new session records (login) | `src/main/java/ai/privado/demo/accounts/service/entity/SessionE.java:7‑15` |
| OUT-3 | Analytics endpoint (`https://localhost/analytics/events`) | HTTP | Log business events (signup, login, profile view) | `src/main/java/ai/privado/demo/accounts/apistubs/DataLoggerS.java:24‑27`, `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:115‑122` |
| OUT-4 | SendGrid API (`mail/send`) | HTTP | Send welcome email on signup | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86‑101` |
| OUT-5 | Slack webhook (`https://hooks.slack.com/...`) | HTTP | Post Slack notification on signup | `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103‑115` |
| OUT-6 | AWS S3 (`PutObject`) | HTTP (SDK) | Store arbitrary files (currently unused) | `src/main/java/ai/privado/demo/accounts/thirdparty/S3stub.java:23‑26` |
| OUT-7 | Mixpanel / Segment analytics (via `AnalyticsStub`) | SDK / HTTP | Third‑party analytics (present but not invoked) | `src/main/java/ai/privado/demo/accounts/thirdparty/AnalyticsStub.java:28‑35` |

## Ingress → Egress connection map
| Ingress | Reaches (egress IDs) | Path through code |
|---------|----------------------|-------------------|
| IN-1 | OUT-1 (read), OUT-2 (read), OUT-3 (write) | `ProfileService.getProfile` reads `SessionE` (`sesr.findById`) → reads `UserE` (`userr.findById`) → builds `EventD` → creates `EventJobRun` → `apiExecutor.execute` → `EventJobRun.run` → `DataLoggerS.sendEvent` → analytics endpoint (`OUT-3`) |
| IN-2 | OUT-1 (read), OUT-2 (write), OUT-3 (write) | `AuthenticationService.authenticate` reads `UserE` (`userr.findByEmail`) → on success creates `SessionE` (`sesr.save`) → calls `sendEvent` → analytics endpoint (`OUT-3`) |
| IN-3 | OUT-1 (write), OUT-2 (write), OUT-3 (write), OUT-4 (write), OUT-5 (write) | `AuthenticationService.signup` creates `UserE` (`userr.save`) → calls `sendEvent` (analytics) → calls `sendEmail` (SendGrid) → calls `sendSlackMessage` (Slack) |
| IN-4 | *none* | Health‑check returns static string, no external interaction |
| IN-5 | OUT-3 (write) | `EventJobRun.run` invokes `DataLoggerS.sendEvent` → analytics endpoint |
| IN-6 | OUT-4 (write) | `SGSendMailJobRun.run` builds SendGrid `Mail` and calls `sg.api(request)` → SendGrid endpoint |
| IN-7 | OUT-5 (write) | `SlackSendJobRun.run` calls `client.chatPostMessage` → Slack webhook |
| IN-8 | OUT-6 (write) | `S3stub.uploadFile` builds `PutObjectRequest` and calls `s3.putObject` (AWS SDK) |
| IN-9 | OUT-7 (write) | `AnalyticsStub.trackEvent` builds Mixpanel/Segment payloads and invokes respective SDK clients |

*Read‑only accesses are marked as “read”. All other accesses are writes or outbound calls.*

## System diagram (Mermaid)
```mermaid
flowchart LR
    subgraph Ingress
        IN1[GET /api/user/{sessionid}]
        IN2[POST /api/public/user/authenticate]
        IN3[POST /api/public/user/signup]
        IN4[GET /api/public/ping]
        IN5[EventJobRun (async)]
        IN6[SGSendMailJobRun (async, unused)]
        IN7[SlackSendJobRun (async, unused)]
        IN8[S3stub.uploadFile (unused)]
        IN9[AnalyticsStub.trackEvent (unused)]
    end

    subgraph Internal
        PS[ProfileService]
        AS[AuthenticationService]
        DL[DataLoggerS]
        EX[apiCallerExecutor]
        SG[SendGridStub]   %% placeholder for SDK usage
        SL[SlackStub]      %% placeholder for SDK usage
        S3[S3stub]
        AN[AnalyticsStub]
    end

    subgraph Egress
        OUT1[users table]
        OUT2[sessions table]
        OUT3[Analytics endpoint]
        OUT4[SendGrid API]
        OUT5[Slack webhook]
        OUT6[AWS S3]
        OUT7[Mixpanel/Segment]
    end

    IN1 --> PS --> DL --> OUT3
    IN2 --> AS --> DL --> OUT3
    IN3 --> AS --> OUT1
    IN3 --> AS --> OUT2
    IN3 --> AS --> DL --> OUT3
    IN3 --> AS --> OUT4
    IN3 --> AS --> OUT5
    IN5 --> DL --> OUT3
    IN6 --> OUT4
    IN7 --> OUT5
    IN8 --> OUT6
    IN9 --> OUT7
```

## Data stores
| Store | Tables / Collections | Purpose |
|-------|----------------------|---------|
| Relational DB (configured via `AccountsDataSource`) | `users` (`UserE`) – persists user identity and profile data.<br>`sessions` (`SessionE`) – persists login session identifiers. | Core application state. |
| In‑memory objects | `ObjectMapper`, `ModelMapper`, `DataLoggerS` bean, thread‑pool executor – hold configuration and runtime helpers. | Runtime support, not persisted. |

Sources: `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:31‑38`, `src/main/java/ai/privado/demo/accounts/service/entity/UserE.java:1‑31`, `src/main/java/ai/privado/demo/accounts/service/entity/SessionE.java:7‑15`.

## Deployment & infrastructure
- **Containerisation**: The application is built with Maven (`pom.xml`) and packaged as a JAR that runs inside a Docker container (implied by the GitHub Actions Maven publish workflow).  
- **CI/CD**: GitHub Actions workflow `.github/workflows/maven-publish.yml` builds and publishes the artifact on release creation.  
- **Secrets handling**: API keys for SendGrid, Slack, and analytics are hard‑coded placeholders in source (e.g., `"Dummy-api-key"` and `"xoxb-your-token"`). Real secrets are expected to be injected via environment variables or secret stores at deployment time (not visible in source).  
- **Runtime configuration**: Database connection properties are read from Spring `Environment` (`GeneralConfig.env`) and injected into the `DataSource` bean. Base URLs for analytics are hard‑coded in `DataLoggerS` and `AuthenticationService`.  

Sources: `.github/workflows/maven-publish.yml`, `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:31‑60`.

## Cross‑cutting concerns
| Concern | Implementation |
|---------|----------------|
| **Authentication / Authorization** | Handled manually in `AuthenticationService.authenticate` (password check) and session lookup; no external auth provider. |
| **Observability** | Logging via SLF4J (`logger.info`, `logger.error`). Event logging to analytics endpoint (`DataLoggerS`, `AuthenticationService.sendEvent`). |
| **Configuration** | Spring `Environment` for DB properties; hard‑coded URLs for analytics, SendGrid, Slack. |
| **Async processing** | `ExecutorService apiCallerExecutor` (fixed thread pool) used to run `EventJobRun` (and potentially other jobs). |
| **Error handling** | `ResponseStatusException` for HTTP error responses; try/catch around external calls with logging. |
| **Feature flags** | None detected in source. |

Sources: `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:43‑122`, `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55‑60`.

## Open questions
1. **Unused async jobs** – `SGSendMailJobRun` and `SlackSendJobRun` are defined but never submitted to the executor. Clarify whether they are intended for future use or should be removed.  
2. **S3 and AnalyticsStub** – Both provide public methods (`uploadFile`, `trackEvent`) that are not referenced anywhere in the current code base. Determine if external services call them via reflection or if they are dead code.  
3. **Secret management** – API keys and webhook URLs are hard‑coded placeholders. Confirm the production secret injection strategy (e.g., Kubernetes secrets, Vault).  
4. **Scalability of the executor** – The thread pool size is fixed at 3. Assess whether this is sufficient under load, especially when profile retrieval triggers async logging for every request.  
5. **Analytics base URL** – The base URL is duplicated in `DataLoggerS` and `AuthenticationService`. Consider centralising this configuration.  

---  

*All statements are derived from the current source code (paths and line numbers cited above). The document reflects the system as it exists today, not a future design.*