# Event Ledger

Event Ledger is a distributed transaction-processing system built with two independently runnable Spring Boot microservices:

- **Event Gateway** — public-facing API that validates and stores transaction events.
- **Account Service** — internal service that manages account balances and transaction history.

The services communicate synchronously over REST and use separate H2 databases.

## Architecture

```text
Client
  |
  v
Event Gateway :8080
  |
  | REST + X-Trace-Id
  v
Account Service :8081