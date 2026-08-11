# Feature Specification: City–Item File Search API

**Feature Branch**: `001-city-item-search`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "I'm building application that's exposing rest endpoints which can be used to search through local files. There will be two endpoints: 1. cities - endpoint return list of items found in given city 2. items - endpoint returns list of cities that has given items. Both endpoints take one parameter, which is command that has to be processed by LLM model. For understanding the command LLM model integration is to be deployed. LLM model is given command that will most likely looking though city items or searching a city having specified items, although command comes through public endpoint so there's no guarantee what that command will exactly be. To fullfill given command the LLM model will be given stdio files mcp server, that has to be started upon application run. LLM searches through available files using files mcp server and prepares answer for given command, that in turn is the endpoint response. There are 3 files in total: 1. cities.csv - city list, each city has it's own ID 2. items.csv - item list, each item has it's own ID 3. connections.csv - intersection between city and item, IDs only. Application upon start starts stdio files mcp server and exposed it's own 2 endpoints, then waits for executions. No other special preparations is required. Application runs until externally terminated."

## Clarifications

### Session 2026-08-11

- Q: When a command on the items endpoint names more than one item, should the returned cities be those that have all of the named items, or those that have any of them? → A: Intersection (all named items); if at least one named item is not found in the local item list, return a descriptive error
- Q: When a command on the cities endpoint names a city that is not in the local city list, should the response be a descriptive error or an empty item list? → A: Descriptive error when any named city is missing
- Q: When a command on the cities endpoint names more than one known city, should the returned items be the combined items from all of those cities, or should the endpoint require exactly one city? → A: Single city only; if more than one city is named, return a clear client error
- Q: When a command is off-topic or cannot be answered from the local city/item data (but the search machinery itself is working), should the endpoint always return a descriptive error, or may it return an empty list? → A: Always descriptive error when the command cannot be fulfilled from local data
- Q: If the local data files or the files MCP server cannot be started successfully, should the application refuse to start, or start and fail later when a search request arrives? → A: Fail fast: application exits/refuses to become ready if files or MCP cannot start

### Session 2026-08-12

- Q: On a successful search, should the endpoint response body be only a structured list of cities or items, or may it also include free-form text from the language model? → A: Structured list only; each entry is a display name only (no IDs); no free-form model text in the success body

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find Items in a City (Priority: P1)

A client calls the cities endpoint with a natural-language command that asks which items are associated with a city. The service interprets the command, looks up the local city–item data, and returns the list of matching items.

**Why this priority**: This is one of the two core search outcomes and delivers immediate value for city-centric queries.

**Independent Test**: Can be fully tested by sending a clear city-focused command to the cities endpoint and verifying the returned item list matches the local data for that city.

**Acceptance Scenarios**:

1. **Given** the service is running with the three local data files available, **When** a client sends a command that identifies exactly one known city, **Then** the cities endpoint returns the item display names linked to that city according to the local connection data.
2. **Given** a known city has no linked items, **When** a client asks for that city's items, **Then** the cities endpoint returns an empty name list (not a failure that implies the city was unknown).
3. **Given** the service is running, **When** a client omits the required command parameter, **Then** the cities endpoint rejects the request with a clear error response.
4. **Given** a command names a city that is not present in the local city list, **When** the cities endpoint processes the command, **Then** it returns a descriptive error and MUST NOT return a partial item list.
5. **Given** a command names more than one city, **When** the cities endpoint processes the command, **Then** it returns a clear client error and MUST NOT return an item list.

---

### User Story 2 - Find Cities for Items (Priority: P1)

A client calls the items endpoint with a natural-language command that asks which cities have given item(s). The service interprets the command, looks up the local data, and returns the list of matching cities.

**Why this priority**: Equal core value to city→items search; both endpoints are required for the product.

**Independent Test**: Can be fully tested by sending a clear item-focused command to the items endpoint and verifying the returned city list matches the local data for those items.

**Acceptance Scenarios**:

1. **Given** the service is running with the three local data files available, **When** a client sends a command that identifies one or more known items, **Then** the items endpoint returns only city display names linked to every named item (intersection) according to the local connection data.
2. **Given** a known item is not linked to any city, **When** a client asks which cities have that item, **Then** the items endpoint returns an empty name list.
3. **Given** the service is running, **When** a client omits the required command parameter, **Then** the items endpoint rejects the request with a clear error response.
4. **Given** a command names multiple items and at least one of those items is not present in the local item list, **When** the items endpoint processes the command, **Then** it returns a descriptive error and MUST NOT return a partial city list.

---

### User Story 3 - Start Ready-to-Serve Search Service (Priority: P2)

An operator starts the application. The service becomes ready to accept the two search endpoints without further manual setup steps, and keeps running until it is stopped externally.

**Why this priority**: Startup readiness is required for the endpoints to be usable, but search behavior itself is the primary product value.

**Independent Test**: Can be tested by starting the application and confirming both endpoints accept requests without additional preparation steps.

**Acceptance Scenarios**:

1. **Given** the local data files are present in the expected location and the files MCP server can start, **When** the application starts, **Then** both search endpoints become available and the service waits for requests until externally terminated.
2. **Given** the application has started successfully, **When** a client sends a valid command to either endpoint, **Then** the service can fulfill the search using the local files without requiring an extra startup action from the operator.
3. **Given** the local data files are missing/unreadable or the files MCP server cannot start, **When** the application attempts to start, **Then** it refuses to become ready (fails fast) and MUST NOT advertise the search endpoints as available.

---

### User Story 4 - Handle Unexpected Public Commands (Priority: P3)

Because the command parameter is public and unconstrained, clients may send commands that are ambiguous, unrelated to cities/items, or impossible to answer from the local files. The service still responds in a predictable, safe way.

**Why this priority**: Important for robustness of a public interface, but secondary to the happy-path search flows.

**Independent Test**: Can be tested by sending nonsensical or off-topic commands and verifying a clear descriptive error without fabricated city/item facts.

**Acceptance Scenarios**:

1. **Given** the service is running, **When** a client sends a command that cannot be answered from the local city/item data, **Then** the endpoint returns a descriptive error and MUST NOT invent cities or items that are not in the files and MUST NOT return an empty success list.
2. **Given** the service is running, **When** a client sends a command whose intent does not match the endpoint (for example, an item-oriented question on the cities endpoint), **Then** the service returns a descriptive inability-to-fulfill error unless it can still produce a valid single-city items response (cities endpoint) or a valid cities list response (items endpoint) grounded in the local data.

---

### Edge Cases

- Command names a city that does not exist: descriptive error (not an empty item list).
- Command names an item that does not exist: descriptive error (not an empty city list).
- Command names multiple items: cities must match all named items; any missing item yields a descriptive error.
- Command names multiple cities: clear client error (cities endpoint accepts exactly one city).
- Command is empty, whitespace-only, extremely long, off-topic, or otherwise unfulfillable from local data: descriptive error (not an empty success list).
- Local data files are missing or unreadable at startup: application fails fast and does not become ready.
- Files MCP server cannot start: application fails fast and does not become ready.
- Local data files are internally inconsistent at request time (for example orphan connection IDs): descriptive error for that request.
- File-access assistance for the language model fails during a request (after a successful start): descriptive error for that request.
- Language model provider is unavailable or returns an unusable answer.
- Concurrent requests to both endpoints while the service is running.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a REST cities endpoint that accepts a single command parameter and returns a list of items associated with exactly one city implied by that command.
- **FR-002**: System MUST expose a REST items endpoint that accepts a single command parameter and returns a list of cities associated with the item (or items) implied by that command.
- **FR-013**: When the items endpoint command names multiple items, the system MUST return only cities linked to every named item (intersection over connections).
- **FR-014**: When the items endpoint command names one or more items and at least one named item is not found in the local item list, the system MUST return a descriptive error and MUST NOT return a partial city list.
- **FR-015**: When the cities endpoint command names a city that is not found in the local city list, the system MUST return a descriptive error and MUST NOT return an item list.
- **FR-016**: When the cities endpoint command names more than one city, the system MUST return a clear client error and MUST NOT return an item list.
- **FR-003**: System MUST interpret each command using a language model; the command is free-form text from a public client and MUST NOT be assumed to follow a fixed syntax.
- **FR-004**: System MUST ground answers in the local corpus consisting of exactly three files: a city list with IDs, an item list with IDs, and a connections file linking city IDs to item IDs only.
- **FR-005**: System MUST give the language model access to those local files through a stdio files MCP server that the application starts as part of its own startup.
- **FR-006**: System MUST use the language model’s file-assisted search result as the basis for the endpoint response returned to the client.
- **FR-007**: System MUST NOT invent cities, items, or connections that are not present in the local files.
- **FR-008**: System MUST reject requests that omit the command parameter with a clear client error response.
- **FR-009**: When a command cannot be fulfilled from the local data (including off-topic or uninterpretable commands), or when the language model or file-access assistance fails, the system MUST return a descriptive error response rather than an empty success list that could be mistaken for “no matches.”
- **FR-017**: Empty success lists are reserved exclusively for cases where the referenced entit(ies) exist in local data and the connection lookup finds no matching related entities.
- **FR-010**: On application start, the system MUST verify the local corpus is readable, start the stdio files MCP server, expose the two REST endpoints, and then wait for requests until externally terminated; no additional operator preparation steps are required after a successful start.
- **FR-018**: If the local data files are missing/unreadable or the stdio files MCP server cannot be started, the system MUST fail fast: refuse to become ready and MUST NOT advertise the search endpoints as available.
- **FR-011**: System MUST expose only these search capabilities as its user-facing interface (no graphical user interface).
- **FR-012**: Successful responses MUST be a structured machine-readable list only (no free-form language-model narrative in the success body). For the cities endpoint the list MUST contain item display names only; for the items endpoint the list MUST contain city display names only. Success list entries MUST NOT include IDs.

### Key Entities

- **City**: A named place in the local city list, uniquely identified by a city ID.
- **Item**: A named thing in the local item list, uniquely identified by an item ID.
- **Connection**: A link between one city ID and one item ID indicating that the item is present in that city.
- **Command**: Free-form client text expressing a search intent for either endpoint.
- **Search Result**: The endpoint-specific list of display names (item names for the cities endpoint, city names for the items endpoint) derived from cities, items, and connections after language-model-assisted interpretation of the command. IDs are used internally for joining only and are not returned.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For commands that clearly name a city present in the local data, the cities endpoint returns the correct item name list in at least 90% of representative test cases drawn from that data.
- **SC-002**: For commands that clearly name an item present in the local data, the items endpoint returns the correct city name list (intersection when multiple items are named) in at least 90% of representative test cases drawn from that data.
- **SC-012**: Successful responses contain only name lists (no IDs and no free-form language-model narrative) in 100% of successful test cases.
- **SC-007**: When an items command names at least one item absent from the local item list, the client receives a descriptive error in 100% of cases (no partial city list).
- **SC-008**: When a cities command names a city absent from the local city list, the client receives a descriptive error in 100% of cases (no item list).
- **SC-009**: When a cities command names more than one city, the client receives a clear client error in 100% of cases (no item list).
- **SC-003**: After a successful start, both endpoints are reachable and can complete a valid search without any further manual setup steps.
- **SC-011**: When required local files are missing/unreadable or the files MCP server cannot start, the application fails to become ready in 100% of such startup attempts.
- **SC-004**: Requests missing the command parameter are rejected with a clear error in 100% of cases.
- **SC-005**: When local data, file access, or language-model assistance is unavailable for a request, the client receives a clear failure response in 100% of observed cases (no fabricated matches).
- **SC-010**: Off-topic or otherwise unfulfillable commands (with search machinery working) return a descriptive error in 100% of representative cases, never an empty success list.
- **SC-006**: Under normal local operation, a straightforward valid search completes and returns to the client within 30 seconds.

## Assumptions

- The three CSV files are supplied with the application (or at a known local path) and are the sole search corpus for this feature.
- City and item records include human-readable names in addition to IDs; connections contain IDs only and must be resolved via the city and item lists.
- “List of items” and “list of cities” in successful responses are lists of display names only (no IDs and no free-form model text).
- No authentication or authorization is required for the public command parameter in this version.
- Endpoint paths and the exact command parameter transport (query string vs body) will be chosen during planning, provided each endpoint accepts exactly one command input.
- Distinguishing “no matches in data” from “search infrastructure failed” is required; empty lists are reserved for successful searches that find nothing.
- For multi-item item→city queries, “no matches” means every named item exists but no city is linked to all of them; a missing named item is an error, not an empty list.
- For city→item queries, “no matches” means the single named city exists but has no linked items; a missing named city or multiple named cities is an error, not an empty list.
- The cities endpoint interprets commands as targeting exactly one city; multi-city commands are rejected.
- The application is a long-running process stopped only by external termination (for example operator interrupt or process manager stop).
- Readiness implies corpus readability and successful files MCP startup; otherwise the process fails fast and never becomes ready.
- One language-model provider/configuration will be selected during planning; credentials come from environment or configuration, not from source code.
- Concurrent request handling is best-effort; correctness of individual responses matters more than high throughput in this version.
