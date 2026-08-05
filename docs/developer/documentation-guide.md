# BPDM Code Documentation Guide

This guide describes how we document Kotlin code in BPDM: what earns a comment, what does not, and how it is written.
It applies to all application code in every BPDM service.

> **Note:** This guide describes the ideal we strive toward — not a description of the current state of the codebase.
> Existing code carries method docs unevenly and still holds comments this guide would remove.
> Contributions that bring the code closer to this ideal are very welcome.

*Binding rules. MUST = required; MUST NOT = forbidden; SHOULD = strong default, deviate only with a documented reason; MAY = permitted.*

---

## The two axes

Documentation answers two different questions, and they follow different rules.

**Contract** — what a caller can rely on. It lives on classes and public methods, and it is written for someone who uses the code without reading it.

**Deviation** — why the code departs from the norm. It lives wherever the departure is visible, and it is written for someone who has read the code and is puzzled by it.

Everything else is noise. The default for any given line is **no comment**: every comment is a line to maintain, and a stale comment is worse than none.

---

## 1. Classes

- A class MUST state its responsibility in one sentence.
- It SHOULD add a second sentence only where using it correctly needs knowledge that is invisible from the outside — that it hands back transient, unpersisted data, or that its methods must be called in a given order.
- It MUST NOT describe how it works internally, which collaborators it uses, or what its individual methods do. Method docs carry that (see [2](#2-public-methods)).
- Every sentence SHOULD be a single tight one. More than one sentence per point is an exception, not a budget.

## 2. Public methods

- Every public method MUST carry a brief statement of what it does. On a single-method class this MAY largely repeat the class's responsibility sentence.
- Each overload MUST carry its own doc. Kotlin does not inherit KDoc between overloads, so an undocumented overload shows an empty popup in IDE autocomplete even when its sibling is fully documented.
- A delegating method MUST be documented in full. The doc states the contract, never the delegation: it MUST NOT name the class or collaborator it forwards to.
- On a class with several methods, the docs SHOULD be written together, and each SHOULD say what the others do not. If two docs could be swapped without either looking wrong, neither is doing its job.
- That distinction rule does NOT apply between overloads of the same method. Near-identical docs are correct there; what they owe the reader is the choice — batch or single, and what comes back.
- An override or interface implementation MUST NOT repeat the interface's doc, which IDEs already surface on the implementing member. It SHOULD be documented only where it adds something a caller must know.
- A private method SHOULD NOT be documented. A private method whose purpose is unclear is a naming problem, not a documentation problem.

## 3. Types that are not services

- A type that represents a **concept** — an outcome, a staged intermediate, a verdict — MUST state in one sentence what it represents. `ParseResult`, `PendingAddressWrite` and `UpsertResult` are concepts.
- A type that is a **record** MUST NOT be documented. `…Request` and `…Parsed` models and JPA entities are records. The test: if the type's name and its fields already tell you what it is, it is a record.
- An entity field MUST NOT document a business rule such as being derived or service-maintained. That rule is decided above the entity and MUST be documented there, normally as part of the contract of the operation that maintains it.
- API DTOs MUST be documented through their OpenAPI `@Schema` descriptions and MUST NOT carry KDoc as well; a second copy will drift from the first.

## 4. Method bodies

Body comments are the most scrutinised documentation in the codebase. Needing one is usually a sign that the code itself is not simple enough.

- A body comment MUST be rare.
- Before writing one, you MUST first try to make it unnecessary: name the value, name the step, extract the method. A comment is warranted only when the complexity is essential and cannot be named away.
- A body comment MUST explain why the code is shaped the way it is. It MUST NOT narrate what the lines below it do.
- A comment you would have to write in more than one place, or one that explains a literal, MUST NOT be written. Both are naming problems: give the concept a method or the literal a constant.

The kind of code that earns one: a sequence whose order is load-bearing for reasons the reader cannot see — a cyclic link that must be wired before either side is flushed, or a changelog order that callers depend on.

## 5. Design rationale

This axis is orthogonal to the levels above; the rule is the same wherever it applies.

- A rationale comment MUST be written only where the code visibly departs from a rule in these guides or from an established pattern in the repository.
- Before writing one, you MUST be able to name the rule or the file that establishes the norm being departed from. If you cannot name it, there is no departure and the comment MUST NOT be written.
- Existing code MUST NOT be treated as that norm. Much of the codebase predates these guides, so a neighbouring class shows what someone wrote, not what is correct; a doc MUST be derived from the rules rather than copied from a sibling file.
- It MUST name the trigger for the departure, briefly. It MUST NOT re-explain the concept being departed from — that concept is already documented in the guide that defines it.
- It SHOULD sit on the code that deviates, not on the code that forces the deviation.
- Where the deviating *shape* and the deviating *sequence* are separate facts, each MUST be documented where it is visible; this is not duplication.

### Worked example: is it really a departure?

The hard part is not the rule, it is judging whether something departs at all. A parser that returns a `ParseResult` follows the norm — [application-code-guide.md §2.3](application-code-guide.md#23-parser-layer) requires it — so there is nothing to explain:

```kotlin
// Wrong. Presents the rule as an exception to itself, then restates the contract of the method below it.
/**
 * Validates the criteria of a BPN-by-request-identifier search.
 *
 * Unlike the other search parsers this one yields a `ParseResult`: a request identifier that was never issued a BPN is
 * an empty result rather than a rejection, but a request exceeding the configured search request limit is refused.
 */

// Right. The responsibility, once.
/**
 * Validates the criteria of a BPN-by-request-identifier search.
 */
```

The mirror case does earn a rationale, because returning the parsed value directly is the narrow exception the same rule permits, and a reader who knows the norm will otherwise look for the missing `ParseResult`:

```kotlin
/**
 * Turns loose address search criteria into the normalized form the search operation queries with.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: no search criterion
 * can be rejected — an unknown or malformed filter value matches nothing — so there is no failure to report.
 */
```

The test the two cases differ on: name the rule, then check which side of it the code sits on.

## 6. Never document

1. Anything the name, signature, or type already says.
2. How a method achieves its result — collaborators, delegation targets, internal steps. The doc is the contract.
3. Concepts the guides already define. A deviation doc names the trigger, not the concept.
4. Simple data holders and their fields.
5. API DTOs in KDoc.
6. Narration of the lines below a comment.
7. Anything you would write twice, or that explains a literal.
8. Section banners such as `// ---- helpers ----`.
9. Commented-out code.
10. Author, date, ticket, or changelog notes. Git holds this.
11. `TODO` or `FIXME` without a tracked issue.
12. Overrides that repeat their interface.

## 7. Style

- A doc SHOULD be a single tight sentence. Where more is genuinely needed it is the author's call, but each extra sentence must earn its place. There is no fixed limit and none is wanted.
- A method doc MAY reference its parameters and types declared in the same file. It MUST NOT link to types outside its own file — such links are maintenance overhead, and naming the type in plain prose reads the same.
- Docs MUST NOT use `@param`, `@return` or `@throws` tags. They restate the signature and rot. Where a parameter matters, name it in the prose.
- A doc MUST start with a third-person verb — "Persists…", "Resolves…", "Applies…" — not "This method persists" and not the imperative "Persist".
- Every comment, including body comments, MUST be a full sentence: capitalised, ending in a period.
- Docs MUST NOT contain filler: "simply", "just", "note that", "basically", "in order to". Each is a sign the sentence has not been tightened.

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
