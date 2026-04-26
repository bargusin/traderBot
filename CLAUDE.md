# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**traderBot** is a Java 17 Telegram bot for trading stocks via the Tinkoff Invest API. It's a multi-module Gradle project supporting sandbox, readonly, and production trading modes.

## Build & Test Commands

```bash
# Build all modules
./gradlew clean build

# Run all tests
./gradlew test

# Build with test coverage report (required for CI)
./gradlew clean build jacocoTestReport

# Run a single test class
./gradlew :core:test --tests "ru.rapidcoder.trader.core.service.EncryptionServiceTest"

# Run tests for a specific module
./gradlew :core:test
./gradlew :bot:test

# SonarCloud analysis (requires env vars)
./gradlew sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=amarakov -Dsonar.projectKey=bargusin_traderBot
```

Minimum code coverage enforced by JaCoCo: 10%.

## Architecture

Three Gradle modules with strict dependency direction:

```
bot  →  core  →  (external APIs)
strategy  (currently empty placeholder)
```

**Package root:** `ru.rapidcoder.trader`

### `core` module — business logic and data access

- **`database/DatabaseManager`** — singleton managing SQLite (HikariCP pool size=1, one writer), Hibernate SessionFactory, and Flyway migrations
- **`database/entity/`** — JPA entities: `User` (chatId, mode, accountId) and `UserSetting` (encrypted token per mode)
- **`database/repository/UserRepository`** — Hibernate-based data access
- **`service/TradingSessionManager`** — singleton with `ConcurrentHashMap` cache of active user sessions; lazily creates and caches `InvestApi` instances per user
- **`service/TradingService`** — wraps Tinkoff Invest API calls (orders, portfolio, accounts)
- **`service/EncryptionService`** — AES-256-GCM token encryption; key injected via environment variable, never hardcoded
- **`service/TradingMode`** — enum: `SANDBOX` (test), `READONLY` (no trading), `PRODUCTION` (real money); READONLY shares storage key with PRODUCTION

Database migrations live in `core/src/main/resources/db/migration/` (Flyway versioned SQL).

### `bot` module — Telegram interface

- **`Bot`** — extends `TelegramLongPollingBot`; routes messages and callback queries
- **`command/`** — one class per Telegram command, each implementing `Command` interface; registered in `CommandRegistry`; subpackages: `order/`, `settings/`, `account/`
- **`handler/MessageHandler`** — top-level dispatcher; **`handler/InputHandler`** — manages multi-step interactive flows
- **`service/UserStateService`** — singleton tracking per-user state across commands
- **`component/InterfaceFactory`** — builds Telegram inline keyboards

## Environment Configuration

The bot reads configuration from `.env` (not committed). Variable names are prefixed by `botEnv` value (`dev` or `prod`):

| Variable | Example |
|---|---|
| `botEnv` | `dev` |
| `devBotName` | `rapidcoderTraderBot` |
| `devTokenId` | Telegram bot token |
| `devEncryptedKey` | Base64 AES-256 key |
| `devStorageFile` | `/tmp/trader_storage.db` |

Tinkoff API tokens are stored encrypted in the database — the `devEncryptedKey` / `prodEncryptedKey` env var is the only plaintext secret that must be protected.

## Key Design Constraints

- **SQLite pool size is fixed at 1** — do not increase it; SQLite only supports one concurrent writer
- **All Tinkoff tokens stored encrypted** — `EncryptionService` must be used; never persist plaintext tokens
- **Singletons are not Spring beans** — `DatabaseManager` and `UserStateService` use static `getInstance()` pattern; don't introduce a DI framework without discussion
- **Long polling, not webhooks** — `TelegramLongPollingBot` is the transport; designed for single-instance deployment
