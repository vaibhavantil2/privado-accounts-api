# Asynchronous Job Execution

## Overview
The Asynchronous Job Execution feature runs background tasks—event logging, email delivery, and Slack messaging—on a fixed‑size thread pool. Application code creates a `Runnable` (`EventJobRun`, `SGSendMailJobRun`, or `SlackSendJobRun`) and submits it to the `ExecutorService` bean defined in `GeneralConfig`. The executor runs the task on one of three worker threads, allowing the main request flow to continue without waiting for the external call to complete.

## Behavior
- **Trigger** – A `Runnable` instance is submitted to the `apiCallerExecutor` bean (`Executors.newFixedThreadPool(3)`) `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55-60`.  
- **Inputs**  
  - `EventJobRun` receives a `DataLoggerS` instance and an `EventD` DTO `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:7-12`.  
  - `SGSendMailJobRun` receives an `SGEMailD` DTO containing `emailid`, `subject`, and `msgBody` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:23-27`.  
  - `SlackSendJobRun` receives a Slack channel ID (`id`) and a message string (`message`) `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:9-13`.  
- **Execution**  
  - `EventJobRun.run()` calls `datalogger.sendEvent(event)` `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:13-15`.  
  - `SGSendMailJobRun.run()` builds a SendGrid `Mail` object and invokes `sg.api(request)` inside a `try` block `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:30-48`.  
  - `SlackSendJobRun.run()` obtains a Slack client via `Slack.getInstance().methods()` and calls `chatPostMessage` `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:20-34`.  
- **State / Data Access**  
  - `EventJobRun` reads the supplied `EventD` and writes nothing locally; it delegates to `DataLoggerS` (a stub bean) `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:13-15`.  
  - `SGSendMailJobRun` reads the `SGEMailD` fields and writes nothing to the DB; it sends the email via the external SendGrid service `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:30-48`.  
  - `SlackSendJobRun` reads the channel ID and message and sends them to the external Slack service `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:20-34`.  
- **Outputs / Side‑effects**  
  - Event logging performed by `DataLoggerS.sendEvent` (implementation not shown).  
  - Email sent through SendGrid; response status, body, and headers are printed to `System.out` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:41-45`.  
  - Slack message posted; result object is logged at INFO level `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:27-30`.  
- **Branching / Error handling**  
  - `SGSendMailJobRun` catches `IOException` from the SendGrid call and logs an error `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:46-48`.  
  - `SlackSendJobRun` catches both `IOException` and `SlackApiException`, logging the error `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:31-34`.  
  - `EventJobRun` contains no explicit error handling; any exception propagates to the executor’s thread‑pool default handler.

## Triggers / Entry points
- **Bean definition** – `apiCallerExecutor` creates the thread pool used for all async jobs `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55-60`.  
- **Runnable classes** – Instantiation of any of the three `Runnable` implementations is an entry point for asynchronous work:  
  - `EventJobRun` `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:7-12`  
  - `SGSendMailJobRun` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:23-27`  
  - `SlackSendJobRun` `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:9-13`  

(How these instances are created and submitted is outside the provided source.)

## End‑to‑end flow (Mermaid)

```mermaid
sequenceDiagram
    participant Caller as Caller (controller/service)
    participant Executor as ExecutorService (apiCallerExecutor)
    participant EventJob as EventJobRun
    participant EmailJob as SGSendMailJobRun
    participant SlackJob as SlackSendJobRun
    participant DataLogger as DataLoggerS
    participant SendGrid as SendGrid API
    participant SlackAPI as Slack API

    Caller->>Executor: submit(Runnable)
    Executor->>EventJob: run()   %% or EmailJob / SlackJob
    alt Event logging
        EventJob->>DataLogger: sendEvent(event)
    else Email sending
        EmailJob->>SendGrid: POST /mail/send (mail JSON)
        SendGrid-->>EmailJob: Response (status, body, headers)
        EmailJob->>EmailJob: print response
    else Slack posting
        SlackJob->>SlackAPI: chatPostMessage(token, channel, text)
        SlackAPI-->>SlackJob: Result object
        SlackJob->>SlackJob: logger.info(result)
    end
```

## State / data touched
- **`DataLoggerS` bean** – receives the `EventD` object and performs logging (implementation not shown) `src/main/java/ai/privado/demo/accounts/async/EventJobRun.java:13-15`.  
- **SendGrid request** – builds a `Mail` object from `SGEMailD` fields and sends it via `sg.api(request)` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:30-48`.  
- **Slack request** – calls `client.chatPostMessage` with token, channel ID, and message `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:20-34`.

## External dependencies
- **SendGrid Java SDK** – `SendGrid`, `Request`, `Response`, `Method` classes used to issue the HTTP POST `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:30-48`.  
- **Slack Java SDK** – `Slack`, `SlackApiException` used to post a message `src/main/java/ai.privado.demo.accounts/async/SlackSendJobRun.java:20-34`.  

## Configuration / parameters
- **Thread‑pool size** – Fixed at 3 threads via `Executors.newFixedThreadPool(3)` `src/main/java/ai/privado/demo/accounts/config/GeneralConfig.java:55-60`.  
- **SendGrid API key** – Hard‑coded placeholder `"Dummy-api-key"` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:35`.  
- **Slack token** – Hard‑coded placeholder `"xoxb-your-token"` `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:26`.  
- **Email “from” address** – Fixed as `test@privado.ai` `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:31`.

## Edge cases & failure modes (observed in code)
- **SendGrid I/O failure** – `IOException` during request construction or transmission is caught and logged as an error `src/main/java/ai/privado/demo/accounts/async/SGSendMailJobRun.java:46-48`.  
- **Slack API failure** – Both `IOException` and `SlackApiException` are caught; the error message and stack trace are logged `src/main/java/ai/privado/demo/accounts/async/SlackSendJobRun.java:31-34`.  
- **No explicit validation** – The run methods assume the DTO fields are non‑null; no null‑checks or other validation are present.  
- **No retry logic** – Failures are logged but not retried; the executor thread simply completes.

## Open questions
- **Job submission** – The code shows the `ExecutorService` bean and the `Runnable` implementations, but it does not show where or how the runnables are instantiated and submitted (e.g., which controller/service calls `apiCallerExecutor.submit(...)`).  
- **Graceful shutdown** – There is no visible shutdown hook for the `apiCallerExecutor`; it is unclear how in‑flight jobs are handled on application stop.  
- **`DataLoggerS` implementation** – The stub class is instantiated as a singleton bean, but its `sendEvent` method body is not provided, so the exact side‑effects of event logging are unknown.  
- **Error handling strategy** – Apart from logging, there is no observable compensation, retry, or alerting mechanism for failed email or Slack deliveries.  
- **Rate‑limit handling** – The code does not inspect HTTP status codes or Slack response metadata to detect throttling; behavior under rate limits is undefined.  
- **Idempotency** – If the same `Runnable` is submitted multiple times, the code does not enforce idempotency; the effect of duplicate executions is unknown.