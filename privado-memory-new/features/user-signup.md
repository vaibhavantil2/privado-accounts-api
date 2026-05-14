# User Signup

## Overview
The User Signup feature creates a new user account, persists it, and triggers welcome communications. It is invoked when a client sends a POST request to the `/api/public/user/signup` endpoint with a valid signup payload. The feature stores the new user in the `USERS` table, returns a `UserProfileD` DTO, sends a welcome email via SendGrid, posts a Slack notification, and logs the signup event to an analytics endpoint.

## Behavior
- **Trigger** – A POST request to `/api/public/user/signup` invokes `AuthenticationService.signup`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:43`
- **Input validation** – The method checks that the request body (`SignupD`) is non‑null and that `email` and `password` are non‑null and non‑empty. If any check fails, a `ResponseStatusException(HttpStatus.BAD_REQUEST)` is thrown. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:44‑51`
- **Extract fields** – Reads `email`, `phone`, `firstName`, `lastName`, and `password` from the DTO. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:52‑58`
- **Create entity** – Instantiates a new `UserE`, sets the extracted fields, and leaves other columns (`dob`, `deviceid`, etc.) unset. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:59‑64`
- **Persist** – Calls `userr.save(us)` (the `UserRepository`) which writes the entity to the `USERS` table. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:65`
- **Logging** – Writes an INFO log entry with the new email and phone. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:66`
- **Event emission** – Calls `sendEvent(UUID.randomUUID().toString(), "SIGNUP", email + phone)`. This builds a JSON payload and POSTs it to `https://localhost/analytics/events` via Unirest. Errors are logged but do not abort the flow. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:67‑78`
- **Welcome email** – Calls `sendEmail(email, "Welcome", "Hi " + firstName + " " + lastName + " Some welcome message")`. The method builds a SendGrid `Mail` object and invokes `sg.api(request)`. IOExceptions are caught and logged. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:69‑84` and `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86‑101`
- **Slack notification** – Calls `sendSlackMessage("someid", "Hello, 世界! … New user Signup - " + email + ", Name - " + firstName + " " + lastName)`. The method posts the message to a hard‑coded webhook URL using the Slack SDK; any exception is logged. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:70‑71` and `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103‑115`
- **Response** – Maps the persisted `UserE` to `UserProfileD` via ModelMapper and returns it to the client. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:72‑73`
- **Failure paths** –  
  * Invalid payload → `ResponseStatusException(400)` (line 80).  
  * Email send failure → logged, flow continues (lines 96‑100).  
  * Slack post failure → logged, flow continues (lines 111‑114).  
  * Event‑logging failure → logged, flow continues (lines 73‑78).

## Triggers / Entry points
- **HTTP route** – `POST /api/public/user/signup` mapped to `AuthenticationService.signup`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:43`

## End-to-end flow (Mermaid)
```mermaid
sequenceDiagram
    participant Client
    participant AuthCtrl as AuthenticationService
    participant Repo as UserRepository
    participant Email as SendGrid (sg.api)
    participant Slack as Slack Webhook
    participant Analytics as Analytics Service (Unirest)

    Client->>AuthCtrl: POST /api/public/user/signup (SignupD JSON)
    AuthCtrl->>AuthCtrl: Validate payload (email & password non‑empty)
    AuthCtrl->>AuthCtrl: Build UserE from DTO
    AuthCtrl->>Repo: save(UserE)
    Repo-->>AuthCtrl: persisted UserE (with ID)
    AuthCtrl->>AuthCtrl: sendEvent(UUID, "SIGNUP", email+phone)
    AuthCtrl->>Analytics: POST /events (JSON payload)
    Analytics-->>AuthCtrl: HTTP 200 / error (logged)
    AuthCtrl->>AuthCtrl: sendEmail(to, subject, body)
    AuthCtrl->>Email: sg.api(request)
    Email-->>AuthCtrl: response / IOException (logged)
    AuthCtrl->>AuthCtrl: sendSlackMessage(id, message)
    AuthCtrl->>Slack: POST webhook URL
    Slack-->>AuthCtrl: result / Exception (logged)
    AuthCtrl->>AuthCtrl: map(UserE → UserProfileD)
    AuthCtrl-->>Client: 200 OK + UserProfileD JSON
```

## State / data touched
- **`USERS` table** – Inserted a new row via `UserRepository.save`. (`UserE` entity definition) `src/main/java/ai/privado/demo/accounts/service/entity/UserE.java:1‑31`
- **Response DTO** – `UserProfileD` (mapped from the saved `UserE`). `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:72‑73`

## External dependencies
- **SendGrid API** – Direct HTTP call via `SendGrid` SDK inside `AuthenticationService.sendEmail`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86‑101`
- **Slack webhook** – HTTP POST to a hard‑coded webhook URL using Slack SDK in `AuthenticationService.sendSlackMessage`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103‑115`
- **Analytics endpoint** – POST to `https://localhost/analytics/events` using Unirest in `AuthenticationService.sendEvent`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:73‑78`
- **ObjectMapper** – Serialises the event payload in `sendEvent`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:74‑75`

## Configuration / parameters
- **SendGrid API key** – Hard‑coded as `"Dummy-api-key"` in `sendEmail`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:88`
- **Slack webhook URL** – Hard‑coded as `"https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"` in `sendSlackMessage`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103`
- **Analytics base URL** – Hard‑coded as `"https://localhost/analytics"` in `sendEvent`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:73`
- **Email “from” address** – `"test@privado.ai"` used in `sendEmail`. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:86`

## Edge cases & failure modes (observed in code)
- **Payload validation** – Missing or empty `email`/`password` results in a 400 response. (`ResponseStatusException`) `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:80`
- **Email send failure** – `IOException` from SendGrid is caught; error is logged, but signup continues. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:96‑100`
- **Slack post failure** – Any `Exception` is caught; error is logged, but signup continues. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:111‑114`
- **Analytics logging failure** – `UnirestException` or `IOException` is caught; error is logged, but signup continues. `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:73‑78`
- **No duplicate‑email check** – The code does not verify that the email is unique before persisting; a duplicate would rely on DB constraints (not shown).

## Open questions
- **Unused stubs** – `SendGridStub` and `SlackStub` exist in the codebase but are never invoked by `AuthenticationService`. It is unclear whether other components use them or if they are remnants of a previous implementation. (`src/main/java/ai/privado/demo/accounts/thirdparty/SendGridStub.java:1`, `src/main/java/ai/privado/demo/accounts/thirdparty/SlackStub.java:1`)
- **`DataLoggerS` usage** – The `AuthenticationService` has a `DataLoggerS` field injected, yet the signup flow calls its own `sendEvent` method instead of `DataLoggerS.sendEvent`. The purpose of the injected `datalogger` is therefore unclear. (`src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:31‑34`)
- **Configuration externalisation** – Base URLs, API keys, and webhook URLs are hard‑coded with TODO comments indicating they should be moved to `application.properties`. The current mechanism for providing these values at runtime is not visible. (`src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:73`, `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:88`, `src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:103`)