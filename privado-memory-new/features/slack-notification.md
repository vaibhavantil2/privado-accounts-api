# Slack Notification

## Overview
The Slack Notification feature posts a message to a Slack channel whenever a new user signs up through the public API. The signup request is handled by `AuthenticationService.signup`, which validates the request, persists the new user, and then sends a Slack alert containing the user’s email and name. The alert is sent directly via Slack’s Web API (using a webhook URL) from the controller method.

## Behavior
- **Trigger** – A `POST /api/public/user/signup` request reaches `AuthenticationService.signup` (`src/main/java/ai/privado/demo/accounts/service/controller/AuthenticationService.java:47`).  
- **Input validation** – The method checks that the `SignupD` payload, its `email`, and `password` are non‑null and non‑empty (`AuthenticationService.java:49‑52`).  
- **Persist user** – A new `UserE` entity is populated with the supplied fields and saved through `UserRepository.save` (`AuthenticationService.java:56‑62`).  
- **Log creation** – An informational log records the new signup (`AuthenticationService.java:63`).  
- **Emit internal event** – `sendEvent` is called to POST a JSON payload to an analytics endpoint (`AuthenticationService.java:64`).  
- **Send welcome email** – `sendEmail` is invoked to dispatch an email via SendGrid (`AuthenticationService.java:65`).  
- **Send Slack alert** – `sendSlackMessage` is called with a hard‑coded webhook URL and a message that includes the email and full name (`AuthenticationService.java:66‑78`).  
- **Slack call** – Inside `sendSlackMessage`, a `Slack` client is obtained (`Slack.getInstance()`) and the webhook URL is used to post the message (`AuthenticationService.java:80‑84`).  
- **Success response** – The newly created user entity is mapped to `UserProfileD` and returned (`AuthenticationService.java:68‑69`).  
- **Validation failure** – If any required field is missing or empty, a `ResponseStatusException` with HTTP 400 is thrown (`AuthenticationService.java:71`).  
- **Slack API error handling** – Exceptions from the Slack call are caught and logged (`AuthenticationService.java:85‑86`).  

## Triggers / Entry points
- `POST /api/public/user/signup` – REST endpoint (`AuthenticationService.java:47`).  
- `AuthenticationService` class – Spring controller handling the signup request (`AuthenticationService.java:47‑71`).  
- `SlackStub` class – Defined as a bean and injected into `AuthenticationService` (`AuthenticationService.java:33‑35`), but **not invoked** by the current signup flow.  
- `SlackSendJobRun` – Runnable that can post a Slack message (`SlackSendJobRun.java:13‑31`), but is not referenced from the signup path.

## End-to-end flow (Mermaid)
```mermaid
sequenceDiagram
    participant Client as "HTTP Client"
    participant AuthCtrl as "AuthenticationService"
    participant DB as "UserRepository / DB"
    participant Analytics as "Analytics Service"
    participant EmailSrv as "SendGrid"
    participant SlackSrv as "Slack Webhook"

    Client->>AuthCtrl: POST /api/public/user/signup (SignupD)
    AuthCtrl->>AuthCtrl: Validate payload (email, password)   %% AuthenticationService.java:49‑52
    alt Valid
        AuthCtrl->>DB: save new UserE                         %% AuthenticationService.java:56‑62
        DB-->>AuthCtrl: persisted UserE
        AuthCtrl->>AuthCtrl: logger.info("New Signup…")      %% AuthenticationService.java:63
        AuthCtrl->>Analytics: POST /events (eventData)       %% AuthenticationService.java:64‑71
        Analytics-->>AuthCtrl: HTTP 200 / error
        AuthCtrl->>EmailSrv: send welcome email               %% AuthenticationService.java:65‑73
        EmailSrv-->>AuthCtrl: response
        AuthCtrl->>SlackSrv: POST webhook (message)           %% AuthenticationService.java:80‑84
        SlackSrv-->>AuthCtrl: result / error
        AuthCtrl->>Client: 200 OK (UserProfileD)             %% AuthenticationService.java:68‑69
    else Invalid
        AuthCtrl->>Client: 400 Bad Request (ResponseStatusException) %% AuthenticationService.java:71
    end
```

## State / data touched
- **`UserE` entity** – New instance created and persisted (`AuthenticationService.java:56‑62`).  
- **`UserRepository`** – `save` method writes the user record to the underlying database (`AuthenticationService.java:62`).  

## External dependencies
- **Slack Webhook URL** – Hard‑coded string used in `sendSlackMessage` (`AuthenticationService.java:78`).  
- **Slack Java SDK** – `Slack.getInstance()` and `client.send(...)` to invoke the webhook (`AuthenticationService.java:80‑84`).  
- **SlackStub (Slack SDK)** – Uses `Slack.getInstance().methods().chatPostMessage` with a bot token (`SlackStub.java:13‑22`). (Not used in the current flow.)  
- **SendGrid** – Constructed and called inside `sendEmail` (`AuthenticationService.java:73‑84`).  
- **Analytics endpoint** – HTTP POST to `https://localhost/analytics/events` via Unirest (`AuthenticationService.java:66‑71`).  

## Configuration / parameters
- **Slack webhook URL** – Literal `"https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"` in `AuthenticationService.sendSlackMessage` (`AuthenticationService.java:78`).  
- **Slack bot token** – Literal `"xoxb-your-token"` inside `SlackStub.sendMessage` and `SlackSendJobRun.run` (`SlackStub.java:20`, `SlackSendJobRun.java:20`).  
- **SendGrid API key** – Literal `"Dummy-api-key"` used when constructing `SendGrid` (`AuthenticationService.java:73`).  
- **Analytics base URL** – Literal `"https://localhost/analytics"` in `sendEvent` (`AuthenticationService.java:66`).  

## Edge cases & failure modes (observed in code)
- **Missing/empty email or password** – Triggers a `ResponseStatusException` with HTTP 400 (`AuthenticationService.java:71`).  
- **Slack webhook call failure** – Any exception from `client.send` is caught and logged (`AuthenticationService.java:85‑86`).  
- **SlackStub / SlackSendJobRun failures** – IOException or `SlackApiException` are caught and logged (`SlackStub.java:25‑27`, `SlackSendJobRun.java:24‑26`).  
- **Analytics POST non‑200 response** – Logs an error but does not abort the signup flow (`AuthenticationService.java:71‑73`).  
- **SendGrid email send IOException** – Logged as an error; signup continues (`AuthenticationService.java:77‑80`).  

## Open questions
- **Why is `SlackStub` injected but never used?** The `AuthenticationService` field `slackStub` is set via constructor (`AuthenticationService.java:33‑35`) yet the signup method calls its own `sendSlackMessage` instead of `slackStub.sendMessage`. The intended usage of `SlackStub` is unclear.  
- **Purpose of `SlackSendJobRun`** – This runnable encapsulates the same Slack posting logic as `SlackStub` but is not referenced from any controller or scheduler. It is unclear whether an asynchronous Slack dispatch is planned elsewhere.  
- **Configuration externalization** – Webhook URL, Slack token, SendGrid key, and analytics base URL are hard‑coded. The code contains a TODO comment about loading the analytics base URL from `application.properties` (`AuthenticationService.java:68`), but the current mechanism for externalizing the other values is not present.  
- **Retry or back‑off strategy** – The code logs errors from external calls but does not retry; it is unknown whether higher‑level infrastructure (e.g., a message queue) is expected to handle retries.