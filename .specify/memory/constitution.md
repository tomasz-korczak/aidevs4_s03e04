<!--
Sync Impact Report:
- Version change: (none/template) → 1.0.0
- Modified principles: N/A (initial ratification from template placeholders)
  - [PRINCIPLE_1_NAME] → I. REST-Only Surface
  - [PRINCIPLE_2_NAME] → II. Local File Search Scope
  - [PRINCIPLE_3_NAME] → III. LLM-Assisted Retrieval
  - [PRINCIPLE_4_NAME] → IV. Explicit JSON Contracts
  - [PRINCIPLE_5_NAME] → V. Bare-Minimum Simplicity
- Added sections: Technical Constraints; Delivery Expectations
- Removed sections: none
- Follow-up TODOs: none
-->

# File Search API Constitution

## Core Principles

### I. REST-Only Surface
The application MUST expose functionality exclusively through HTTP REST
endpoints. It MUST NOT include a graphical user interface, SPA, or browser UI
pages. All client interaction MUST use request/response HTTP APIs.

Rationale: The product is a backend service; UI work is out of scope and MUST
not dilute delivery of searchable endpoints.

### II. Local File Search Scope
Search MUST operate over files available on the local filesystem configured for
the service. Endpoints MUST accept search intent from the client and return
results grounded in that local corpus. The service MUST NOT invent file contents
that are not present in the local files.

Rationale: The core value is retrieval over a known local document set, not
open-ended generation.

### III. LLM-Assisted Retrieval
The service MUST integrate an LLM to help interpret queries and/or select or
summarize relevant local file content for responses. LLM usage MUST remain
subordinate to the local file corpus: model output MUST reference or derive from
retrieved local material. Failures of the LLM provider MUST surface as clear API
errors rather than silent empty success.

Rationale: The LLM is a search aid over local files, not an unconstrained oracle.

### IV. Explicit JSON Contracts
REST endpoints MUST use JSON for request and response bodies unless a specific
endpoint documents otherwise. Responses MUST include unambiguous success and
error shapes. Breaking changes to endpoint paths, methods, or schemas MUST be
versioned or explicitly documented before release.

Rationale: Machine clients need stable, parseable contracts without UI cues.

### V. Bare-Minimum Simplicity
Implement only what is required to expose searchable REST endpoints over local
files with LLM assistance. Features, layers, and dependencies without a clear
need for that goal MUST NOT be added. Prefer one clear path for search over
parallel abstractions.

Rationale: Bare-minimum scope keeps the service deliverable and reviewable.

## Technical Constraints

- No GUI, static marketing pages, or interactive frontends.
- Primary interface: REST over HTTP with JSON payloads.
- Search corpus: local filesystem paths configured for the service.
- LLM integration is required for search assistance; credentials and model
  configuration MUST come from environment or config, not hard-coded secrets in
  source.
- The service MUST remain runnable as a standalone backend process.

## Delivery Expectations

- New or changed endpoints MUST document method, path, request fields, response
  fields, and error cases.
- Changes that affect search behavior or LLM prompting MUST be reviewable
  against these principles before merge.
- Prefer small, end-to-end vertical slices (endpoint → local read → LLM assist →
  JSON response) over large unfinished frameworks.

## Governance

This constitution supersedes conflicting informal practices for this project.
Amendments MUST update this file, bump `CONSTITUTION_VERSION` using semantic
versioning (MAJOR for incompatible principle removals/redefinitions, MINOR for
new or materially expanded guidance, PATCH for clarifications), and set
**Last Amended** to the amendment date (ISO YYYY-MM-DD).

All feature specs, plans, and tasks MUST remain consistent with these
principles. Compliance MUST be checked during planning and review: REST-only
surface, local-file grounding, LLM-assisted retrieval, JSON contracts, and
simplicity. Unjustified complexity MUST be rejected or deferred.

**Version**: 1.0.0 | **Ratified**: 2026-08-11 | **Last Amended**: 2026-08-11
