# Implementation Plan: City–Item File Search API

**Branch**: `001-city-item-search` | **Date**: 2026-08-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-city-item-search/spec.md` plus plan-time stack and contract rules supplied with `/speckit-plan`.

**Note**: Wire contract is aligned with `spec.md` (synced 2026-08-12): `POST /api/items` + `POST /api/city`, `{params}` → `{output}` (4–500 UTF-8 bytes, comma-separated names with no spaces).

## Summary

Build a Spring Boot 3 / Java 23 REST service (`pl.tomaszko:s03e04`) that exposes two POST endpoints. Each accepts a natural-language `params` command, runs a Spring AI `ChatClient` tool-calling loop against OpenRouter (`nvidia/nemotron-3-ultra-550b-a55b:free`), and grounds answers in local CSVs via a stdio **filesmcp** MCP server pointed at `C:\priv\aidevs4-cwiczenia\workspace`. Responses are always `{"output":"<string>"}` where `output` is UTF-8 length 4–500 bytes: comma-separated exact names on success, or a descriptive error string (including overflow / out-of-scope / not-found cases).

## Technical Context

**Language/Version**: Java 23 (`JAVA_HOME=C:\tools\jdk-23.0.2`)

**Primary Dependencies**: Spring Boot `3.5.15`, Spring AI `1.1.8` (BOM), `spring-boot-starter-web`, `spring-ai-starter-model-openai` (OpenRouter-compatible), `spring-ai-starter-mcp-client`, Maven

**Storage**: Local CSV corpus via MCP filesystem tools (no database)

**Testing**: Spring Boot Test + MockMvc / WebTestClient; optional Testcontainers-free unit tests for output-byte validation and prompt assembly

**Target Platform**: Windows local long-running JVM process

**Project Type**: web-service (REST only, no GUI)

**Performance Goals**: No wall-clock latency success criterion (author decision; SC-006 removed)

**Constraints**: `output` UTF-8 byte length ∈ [4, 500]; exact CSV name values only; fail-fast if corpus/MCP unavailable; MCP used read-only (read/search tools only); `OPENROUTER_API_KEY` required via env; `HUB_API_KEY` optional and unused by `/api/*` (FR-022)

**Scale/Scope**: 2 endpoints, 3 CSV files, 1 stdio MCP child process, 2 dedicated system prompts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. REST-Only Surface | PASS | Only `/api/items` and `/api/city` HTTP APIs; no UI |
| II. Local File Search Scope | PASS | Answers grounded in workspace CSVs via files MCP |
| III. LLM-Assisted Retrieval | PASS | Spring AI + OpenRouter; failures → descriptive `output` errors |
| IV. Explicit JSON Contracts | PASS | Request `{params}`, response `{output}` documented in contracts/ |
| V. Bare-Minimum Simplicity | PASS | Single Maven module; ChatClient + MCP client; no extra layers |
| Tech constraints (config secrets, standalone process) | PASS | Env/config parametrization; long-running Boot app |

**Post-Phase 1 re-check**: PASS — contracts, data model, and quickstart stay REST/JSON/local-file/LLM-scoped without GUI or unjustified complexity.

## Project Structure

### Documentation (this feature)

```text
specs/001-city-item-search/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml
│   └── system-prompts.md
└── tasks.md                 # created later by /speckit-tasks
```

### Source Code (repository root)

```text
pom.xml
src/main/java/pl/tomaszko/s03e04/
├── S03e04Application.java
├── config/
│   ├── AppProperties.java              # @ConfigurationProperties
│   ├── AiConfig.java                   # ChatClient, OpenRouter, MCP wiring
│   └── StartupReadinessValidator.java  # corpus + MCP fail-fast at boot
├── web/
│   ├── SearchController.java           # /api/items, /api/city
│   ├── SearchRequest.java
│   └── SearchResponse.java
├── service/
│   ├── ItemsSearchService.java
│   ├── CitySearchService.java
│   ├── LlmSearchGateway.java           # ChatClient + tools orchestration
│   └── OutputConstraintValidator.java  # 4–500 UTF-8 bytes
├── prompt/
│   └── PromptTemplates.java            # load/render system prompts
└── logging/
    ├── ModelExchangeLogger.java        # system/user prompts, tools, response
    └── ToolExecutionLogger.java        # tool params + results
src/main/resources/
├── application.yml
├── logback-spring.xml
└── prompts/
    ├── items-system.st                 # string template
    └── city-system.st
src/test/java/pl/tomaszko/s03e04/
├── web/
├── service/
└── ...
```

**Structure Decision**: Single Maven Spring Boot module at repo root (`groupId=pl.tomaszko`, `artifactId=s03e04`) — matches bare-minimum constitution and exercise layout.

## Complexity Tracking

> No constitution violations requiring justification.
