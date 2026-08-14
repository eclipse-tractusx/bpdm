# BPDM Logging Guide

This guide describes what BPDM services log and at which level.
It applies to every BPDM service: Pool, Gate, Orchestrator, Cleaning Service Dummy, and any service added later. It does **not** apply to `bpdm-system-tester`, whose log output is its report to the console.

---

## Table of Contents

- [Part 1 — The Big Picture](#part-1--the-big-picture)
  - [1.1 Why INFO is rationed](#11-why-info-is-rationed)
  - [1.2 What counts as a subject](#12-what-counts-as-a-subject)
  - [1.3 The log is not the audit trail](#13-the-log-is-not-the-audit-trail)
- [Part 2 — Rules](#part-2--rules)
  - [2.1 INFO](#21-info)
  - [2.2 DEBUG](#22-debug)
  - [2.3 WARN & ERROR](#23-warn--error)
  - [2.4 Where a line lives](#24-where-a-line-lives)
  - [2.5 Schedules](#25-schedules)
  - [2.6 Mechanics](#26-mechanics)
- [Part 3 — Known limitations](#part-3--known-limitations)
- [NOTICE](#notice)

---

# Part 1 — The Big Picture

*This part is explanatory. The binding rules are in [Part 2](#part-2--rules).*

## 1.1 Why INFO is rationed

INFO is on in production. Every INFO line is therefore paid for on every deployment, forever, and a log where most lines are noise is a log nobody reads.

BPDM services poll each other on 20–30 second schedules and are probed by Kubernetes every 5–10 seconds. Any rule that lets recurring activity reach INFO produces six-figure daily line counts per pod before a single business request arrives. So the test for INFO is not "is this interesting?" — almost everything is, to someone, once. The test is:

> **Did something change that outlives this process?**

If the answer is no, the line is DEBUG. DEBUG is off in production, which is exactly why it can be generous.

## 1.2 What counts as a subject

An INFO line names a *subject* — the thing that changed. The unit is decidable, not a matter of taste:

> **A subject is something with its own stable external identifier. Anything reached only through another thing's identifier is an aspect of that subject.**

| thing | verdict | why |
|---|---|---|
| legal entity, site, address | subject | has a BPN |
| identifier type, legal form, reason code | subject | has a technical key |
| relation | subject | identified by type and endpoints |
| golden record task | subject | has a task id |
| data space participation | aspect of a legal entity | no identifier of its own |
| relation validity period | aspect of a relation | no identifier of its own |
| an issued BPN | aspect of the create that issued it | reported by that create |

This is the same test that decides changelog authority, so it introduces no new judgement.

## 1.3 The log is not the audit trail

Pool's changelog table is the audit trail: it is written inside the transaction, so it commits or rolls back with the data, and it is queryable. Application logs are observability — best-effort, greppable, and allowed to be lost.

That distinction sets the cost ceiling for this guide. We do not build machinery to make log lines transactionally exact. Where a line can be wrong on rollback, we accept it and say so (see [Part 3](#part-3--known-limitations)).

---

# Part 2 — Rules

*Binding rules. MUST = required; MUST NOT = forbidden; SHOULD = strong default, deviate only with a documented reason.*

## 2.1 INFO

- An INFO line MUST report one of exactly three things: **a persisted change**, **effective configuration at startup**, or **a process lifecycle transition**. Nothing else may use INFO.
- A persisted-change line MUST state an **outcome**, not an intent: it is written after the write, in the past tense, and names the subject ([1.2](#12-what-counts-as-a-subject)) by its identifier. `Created legal form 'LLC'`, never `Create new legal form`.
- A line MUST NOT be emitted before the work it describes. A statement at the top of a method describing what the method is about to do is an intent line and MUST be DEBUG or deleted.
- A batch MUST produce one INFO line for the batch, not one per entry. Per-entry detail belongs at DEBUG.
- A run that changed nothing MUST NOT log at INFO. Zero counts, empty poll results and `UpsertType.NoChange` are DEBUG.
- A rejected request MUST NOT log at INFO. It persisted nothing and the caller was already told; its parse errors are DEBUG.
- A line SHOULD name the aspect that changed when the write is partial and the `FieldUpdate` masks make it available — `LEGAL_ENTITY UPDATE (participation)`.
- Where an operational number matters continuously (throughput, rejection rate), it SHOULD be a metric rather than an INFO line. Actuator is configured in every service.

## 2.2 DEBUG

- Everything that is not INFO, WARN or ERROR is DEBUG. There is no volume budget; DEBUG is off in production.
- A DEBUG statement MUST use the lazy lambda form `logger.debug { … }` so a disabled level costs nothing. Where the logger is not `KLogger` — the inherited `logger` in a Spring filter, for instance — the call MUST be guarded with `isDebugEnabled`.
- Full business partner payloads MUST NOT be logged. Company data is largely not personal, but sole-proprietor names and addresses are, and DEBUG lands in the same sink.
- Recurring machinery — every scheduler tick, every poll, every health check, every request — MUST be DEBUG regardless of how useful it looks.
- HTTP request logging MUST skip `/actuator/**` entirely. Probes and dependency checks otherwise drown the DEBUG log, which is the log being read when DEBUG is on.

## 2.3 WARN & ERROR

- ERROR MUST mean an operator has to act. WARN MUST mean something is degraded but handled.
- A client error (validation failure, 4xx) MUST NOT be WARN or ERROR. It is a normal outcome of an open API.
- An ERROR MUST carry the exception where one exists — `logger.error(e) { … }`, not the message alone.

## 2.4 Where a line lives

- A persisted-change line MUST be emitted by the **write authority** for that subject — the operation service that persists it. This is the same single-authority rule as [`application-code-guide.md` §2.4](application-code-guide.md#24-operation-layer), read as a logging rule.
- Application services MUST NOT log at INFO. They are per API version, so a line there is duplicated across v6 and v7, and they are bypassed entirely by the golden record task path — a line there would miss most production writes.
- Parsers MUST NOT log at INFO. They decide; they never persist.
- A subject whose write emits a changelog entry SHOULD rely on `ChangelogCreateService` for its INFO line rather than adding its own. A subject with no changelog — relations, metadata, tasks — MUST log its own.
- Logging MUST NOT be routed through `ChangelogCreateService.record(...)` by subjects that do not belong in the changelog. The changelog is a domain artifact with its own emission policy; coupling observability to it makes a business decision silently change the logs.

## 2.5 Schedules

- Every schedule MUST log its activation once at startup, at INFO, naming the schedule and its cron — or stating that it is disabled. This is the configuration case of [2.1](#21-info) and it is what replaces per-tick narration.
- A scheduled run MUST log at INFO only when it changed something, and MUST report what it changed. Every other part of the run is DEBUG.
- Where a service schedules through one central helper, the activation line SHOULD live in that helper rather than being repeated per schedule. `GoldenRecordTaskConfiguration.scheduleIfEnabled` in Gate is the reference.

## 2.6 Mechanics

- Message text MUST distinguish schedules and subjects that would otherwise be indistinguishable at INFO. Two schedulers logging `Total of 0 processed` are two lines nobody can act on.
- Message text MUST NOT be assembled from lazily-loaded entity state outside an open persistence context.
- A log line MUST NOT be relied on as a test assertion target unless the test commits — lines emitted on transaction commit never fire in `@Transactional` rollback-style tests.
- Log correlation MUST come from the MDC (`%X{user}`, `%X{request}`) rather than from repeating identifiers in every message.

---

# Part 3 — Known limitations

*Accepted, not oversights. Each is a candidate for a later contribution.*

- **Inline lines can lie on rollback.** Relation and metadata INFO lines are written inside the transaction, so a rollback leaves a line claiming a change that did not happen. There is always an ERROR alongside it. Business partner lines come from `ChangelogCreateService`, which logs on commit and is therefore exact. Making the rest exact needs a transaction-scoped commit hook; it was deliberately deferred.
- **Aspects are not reported for business partner writes.** `ChangelogRecord` carries no aspect, so those lines name the subject and change type only.
- **Not every write reports a count.** `GoldenRecordConsistencyService` and the Pool trigger batch plumbing return no number of changes, so their runs stay silent at DEBUG rather than reporting an outcome.

---

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2026 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2026 SAP SE
- SPDX-FileCopyrightText: 2023,2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2026 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2026 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2026 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2026 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
