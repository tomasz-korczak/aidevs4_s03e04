---
description: "Task list for City–Item File Search API implementation"
---

# Tasks: City–Item File Search API

**Input**: Design documents from `/specs/001-city-item-search/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Not requested in spec — no test tasks included

**Organization**: Tasks grouped by user story (US1–US4) for independent delivery

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: User story label (`[US1]`…`[US4]`)
- Include exact file paths in descriptions

## Path Conventions

- Single Maven module at repo root: `src/main/java/pl/tomaszko/s03e04/`, `src/main/resources/`, `src/test/java/pl/tomaszko/s03e04/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven/Spring Boot project skeleton and toolchain

- [ ] T001 Create Maven project layout with `pom.xml` (`groupId=pl.tomaszko`, `artifactId=s03e04`, Java 23, Spring Boot `3.5.15`, Spring AI BOM `1.1.8`) at repository root
- [ ] T002 Add dependencies in `pom.xml`: `spring-boot-starter-web`, `spring-ai-starter-model-openai`, `spring-ai-starter-mcp-client`, `spring-boot-starter-validation`, `spring-boot-starter-test` (test scope)
- [ ] T003 [P] Create application entrypoint `src/main/java/pl/tomaszko/s03e04/S03e04Application.java`
- [ ] T004 [P] Create package directories under `src/main/java/pl/tomaszko/s03e04/` for `config/`, `web/`, `service/`, `prompt/`, `logging/` per plan.md
- [ ] T005 [P] Add baseline `src/main/resources/application.yml` with `server.port`, app placeholders, and OpenRouter base-url stub

**Checkpoint**: Project builds with `mvn -q -DskipTests package` (empty app OK)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared config, logging, MCP+LLM wiring, DTOs, output validation — MUST complete before story endpoints

**⚠️ CRITICAL**: No user story endpoint work until this phase is complete

- [ ] T006 Implement `@ConfigurationProperties` in `src/main/java/pl/tomaszko/s03e04/config/AppProperties.java` for MCP jar path, data root, LLM model, prompt paths, HTTP base, optional `HUB_API_KEY` (unused by search)
- [ ] T007 Bind properties and env mapping in `src/main/resources/application.yml` (`OPENROUTER_API_KEY`, `app.mcp.files.*`, `app.llm.model=nvidia/nemotron-3-ultra-550b-a55b:free`, `spring.ai.openai.*`)
- [ ] T008 [P] Configure console + file logging in `src/main/resources/logback-spring.xml`
- [ ] T009 [P] Implement `src/main/java/pl/tomaszko/s03e04/logging/ToolExecutionLogger.java` to log tool parameters and results
- [ ] T010 [P] Implement `src/main/java/pl/tomaszko/s03e04/logging/ModelExchangeLogger.java` to log system prompt, tool definitions, user prompt, and model response
- [ ] T011 [P] Create DTOs `src/main/java/pl/tomaszko/s03e04/web/SearchRequest.java` and `src/main/java/pl/tomaszko/s03e04/web/SearchResponse.java` matching contracts/openapi.yaml
- [ ] T012 Implement `src/main/java/pl/tomaszko/s03e04/service/OutputConstraintValidator.java` enforcing UTF-8 byte length 4–500 and rejecting/replacing invalid lengths with descriptive overflow/size errors
- [ ] T013 [P] Add prompt templates `src/main/resources/prompts/city-system.st` and `src/main/resources/prompts/items-system.st` from contracts/system-prompts.md (comma, no spaces; MCP read-only tips; byte window)
- [ ] T014 Implement `src/main/java/pl/tomaszko/s03e04/prompt/PromptTemplates.java` to load/render string templates with `{data_root}`, `{byte_min}`, `{byte_max}`, shared rules
- [ ] T015 Configure stdio MCP client in `src/main/java/pl/tomaszko/s03e04/config/AiConfig.java` (java -jar filesmcp, `FS_ROOTS`=data root) and expose only `fs_read`/`fs_search` tool callbacks to ChatClient
- [ ] T016 Wire OpenRouter ChatClient + tool callbacks + exchange logging in `src/main/java/pl/tomaszko/s03e04/config/AiConfig.java`
- [ ] T017 Implement `src/main/java/pl/tomaszko/s03e04/service/LlmSearchGateway.java` to run ChatClient with a selected system prompt + user `params`, apply OutputConstraintValidator, map infra failures to HTTP 500-ready errors
- [ ] T024 Implement startup corpus check (readable `cities.csv`, `items.csv`, `connections.csv` under data root) in `src/main/java/pl/tomaszko/s03e04/config/StartupReadinessValidator.java` (or equivalent `@Component` used at boot)
- [ ] T025 Fail application context/startup if MCP stdio client cannot initialize or corpus check fails in `src/main/java/pl/tomaszko/s03e04/config/AiConfig.java` / readiness validator (FR-018)

**Checkpoint**: Foundation ready — ChatClient+MCP wired; corpus/MCP fail-fast active; DTOs and validators exist

---

## Phase 3: User Story 1 - Find Items in a City (Priority: P1) 🎯 MVP

**Goal**: `POST /api/city` returns comma-separated exact item names for exactly one city, or descriptive `output` errors (400 blank params; 200 domain errors; 500 infra)

**Independent Test**: Start app with corpus+MCP; `POST /api/city` with `{"params":"List all items in Warszawa"}` returns HTTP 200 and `output` of exact `items.csv` names joined by `,` (no spaces), or a valid descriptive error within 4–500 UTF-8 bytes

### Implementation for User Story 1

- [ ] T018 [US1] Implement `src/main/java/pl/tomaszko/s03e04/service/CitySearchService.java` using LlmSearchGateway + city system prompt (single city, exact names, overflow/missing/multi-city/out-of-scope errors)
- [ ] T019 [US1] Implement `POST /api/city` in `src/main/java/pl/tomaszko/s03e04/web/SearchController.java` validating non-blank `params` (HTTP 400) and returning `SearchResponse` with correct status (200 domain / 500 infra)
- [ ] T020 [US1] Ensure city prompt path and tips are parametrized via `AppProperties` / `application.yml` for `/api/city` only

**Checkpoint**: User Story 1 fully functional and testable independently via `/api/city`

---

## Phase 4: User Story 2 - Find Cities for Items (Priority: P1)

**Goal**: `POST /api/items` returns city names for intersection of all named items, or descriptive errors (missing item, empty intersection, out of scope, overflow)

**Independent Test**: `POST /api/items` with `params` naming known item(s) returns HTTP 200 `output` of exact intersecting city names (`Name1,Name2`) or descriptive error; missing item yields error without partial cities

### Implementation for User Story 2

- [ ] T021 [P] [US2] Implement `src/main/java/pl/tomaszko/s03e04/service/ItemsSearchService.java` using LlmSearchGateway + items system prompt (intersection, missing-item error, overflow/out-of-scope)
- [ ] T022 [US2] Add `POST /api/items` to `src/main/java/pl/tomaszko/s03e04/web/SearchController.java` with same request validation and status mapping as `/api/city` (200 domain / 500 infra)
- [ ] T023 [US2] Ensure items prompt path/tips are parametrized in `application.yml` / `AppProperties` for `/api/items` only

**Checkpoint**: User Stories 1 and 2 both work independently (`/api/city` and `/api/items`)

---

## Phase 5: User Story 3 - Start Ready-to-Serve Search Service (Priority: P2)

**Goal**: Document and smoke-validate long-running ready service (fail-fast already in Phase 2 via T024/T025)

**Independent Test**: (a) With valid jar+workspace, app starts and both endpoints respond; (b) with missing CSV or bad MCP jar path, process fails to become ready (Phase 2 behavior)

### Implementation for User Story 3

- [ ] T026 [US3] Document run/stop expectations in `README.md` at repo root (JAVA_HOME, env keys, `mvn spring-boot:run`, Ctrl+C); note fail-fast when corpus/MCP missing

**Checkpoint**: Operators can start/stop from README; readiness behavior matches Spec US3

---

## Phase 6: User Story 4 - Handle Unexpected Public Commands (Priority: P3)

**Goal**: Off-topic / unfulfillable / wrong-intent commands return HTTP 200 descriptive `output` (4–500 bytes) without inventing names; prompts enforce grounding

**Independent Test**: Send nonsense or wrong-endpoint intent to `/api/city` and `/api/items`; receive HTTP 200 descriptive `output`, never fabricated city/item names, never empty success string

### Implementation for User Story 4

- [ ] T027 [P] [US4] Harden out-of-scope / inability-to-fulfill instructions in `src/main/resources/prompts/city-system.st` and `src/main/resources/prompts/items-system.st`
- [ ] T028 [US4] Ensure `LlmSearchGateway` / services never treat blank model text as success; map ungrounded/empty finals to descriptive domain errors in `src/main/java/pl/tomaszko/s03e04/service/LlmSearchGateway.java`
- [ ] T029 [US4] Confirm controller status mapping keeps domain unfulfillable cases as HTTP 200 + `output` (not 500) in `src/main/java/pl/tomaszko/s03e04/web/SearchController.java`

**Checkpoint**: All four user stories independently demonstrable

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Logging completeness, contract alignment, quickstart validation

- [ ] T030 [P] Verify tool + model exchange logging covers all ChatClient/tool calls end-to-end; adjust `ToolExecutionLogger.java` / `ModelExchangeLogger.java` as needed
- [ ] T031 [P] Align `application.yml` defaults with research.md paths (filesmcp JAR, workspace data root, model id)
- [ ] T032 Run validation scenarios from `specs/001-city-item-search/quickstart.md` against a running instance and fix gaps
- [ ] T033 [P] Final pass: ensure `output` formatting uses commas with no spaces and FR-020 byte window on all response paths in `OutputConstraintValidator.java` / services

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies
- **Phase 2 Foundational**: Depends on Setup — **BLOCKS** all user stories; includes FR-018 fail-fast (`T024`/`T025`)
- **Phase 3 US1 (P1)**: Depends on Foundational — MVP
- **Phase 4 US2 (P1)**: Depends on Foundational (can proceed after or parallel to US1 if staffed; shares controller file carefully)
- **Phase 5 US3 (P2)**: Depends on Foundational; docs/smoke after endpoints exist (prefer after US1 or US2)
- **Phase 6 US4 (P3)**: Depends on US1+US2 prompts/services existing
- **Phase 7 Polish**: Depends on desired stories complete

### User Story Dependencies

- **US1**: After Foundational only (fail-fast already active)
- **US2**: After Foundational; shares `SearchController.java` / `LlmSearchGateway.java` with US1 — coordinate sequential edits on those files
- **US3**: After Foundational; README + operator smoke validation
- **US4**: After US1+US2 prompt/service paths exist

### Parallel Opportunities

- T003–T005 (setup files) in parallel after T001–T002
- T008–T011, T013 in parallel within Foundational where noted
- T021 can start in parallel with T018 only if different owners avoid conflicting controller edits; otherwise sequence US1 then US2
- T027 parallelizable across two prompt files
- T030, T031, T033 polish tasks parallelizable

---

## Parallel Example: Foundational

```text
Task: "Implement ToolExecutionLogger.java"
Task: "Implement ModelExchangeLogger.java"
Task: "Create SearchRequest.java and SearchResponse.java"
Task: "Add city-system.st and items-system.st templates"
```

## Parallel Example: User Story 1

```text
# After Foundational:
Task: "Implement CitySearchService.java"
# Then controller binding:
Task: "Implement POST /api/city in SearchController.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 Setup
2. Complete Phase 2 Foundational (includes fail-fast T024/T025)
3. Complete Phase 3 US1 (`/api/city`)
4. **STOP and VALIDATE** with Independent Test for US1
5. Demo MVP

### Incremental Delivery

1. Setup + Foundational (fail-fast included) → foundation ready
2. US1 `/api/city` → validate → MVP
3. US2 `/api/items` → validate
4. US3 operator docs/smoke → validate
5. US4 out-of-scope hardening → validate
6. Polish + quickstart.md

### Suggested MVP Scope

- **MVP = Phase 1 + Phase 2 (incl. fail-fast) + Phase 3 (US1 `/api/city`)**

---

## Notes

- [P] = different files / safe parallel
- No automated test tasks (not requested); use quickstart.md for manual validation
- JDK: `C:\tools\jdk-23.0.2`; require `OPENROUTER_API_KEY`
- Task IDs `T024`/`T025` live in Phase 2 (remediation C1); Phase 5 keeps `T026` only
- Commit after each task or logical group
- Stop at checkpoints to validate independently
