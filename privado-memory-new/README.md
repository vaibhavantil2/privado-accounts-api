# As-Is Knowledge Base

Descriptive documentation of the system as it works today — generated from source.

## Engineering Doc
- [erd.md](erd.md) — system overview, ingress catalog, egress catalog, ingress→egress connection map, deployment

## Features
- [User Signup](features/user-signup.md) — Creates a new user account, persists it, and triggers welcome communications.
- [User Login](features/user-login.md) — Validates credentials and creates a session token for authenticated users.
- [Session Management](features/session-management.md) — Handles creation, storage, and lookup of user sessions.
- [User Profile Retrieval](features/user-profile-retrieval.md) — Returns the profile information of a user based on a valid session identifier.
- [Event Logging / Analytics](features/event-logging-analytics.md) — Sends business events (signup, login, profile view) to an external analytics endpoint and to third‑party analytics services.
- [Email Notification](features/email-notification.md) — Sends transactional emails (e.g., welcome email) via SendGrid.
- [Slack Notification](features/slack-notification.md) — Posts messages to Slack channels for operational alerts such as new user sign‑ups.
- [Asynchronous Job Execution](features/asynchronous-job-execution.md) — Executes background tasks (event logging, email, Slack messages) using a fixed‑size thread pool.
- [Third‑Party Analytics Integration](features/third-party-analytics-integration.md) — Provides wrappers for Mixpanel, Segment, and Amplitude to track events.
- [S3 File Storage](features/s3-file-storage.md) — Uploads files to AWS S3 buckets.
- [Health Check / Ping](features/health-check-ping.md) — Simple endpoint to verify the service is up and report its version.