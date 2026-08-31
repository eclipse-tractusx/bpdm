# Postman Collections

This folder contains Postman collections documenting how to reach the BPDM APIs through an EDC.

<!-- TOC -->
* [Postman Collections](#postman-collections)
  * [What Is In Here](#what-is-in-here)
  * [Why The BPDM API Requests Are Not In The Collection](#why-the-bpdm-api-requests-are-not-in-the-collection)
  * [Setting Up The Consumer Collection](#setting-up-the-consumer-collection)
    * [Environment Variables](#environment-variables)
    * [Getting A Transfer Token](#getting-a-transfer-token)
  * [Adding The BPDM APIs From The Open-API Documents](#adding-the-bpdm-apis-from-the-open-api-documents)
    * [Import The Document](#import-the-document)
    * [Point The Collection At The EDC Data Plane](#point-the-collection-at-the-edc-data-plane)
    * [How The Access Groups Are Derived](#how-the-access-groups-are-derived)
  * [NOTICE](#notice)
<!-- TOC -->

## What Is In Here

| Collection                                                                            | Purpose                                                                    |
|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| [EDC Provider Setup](EDC%20Provider%20Setup.postman_collection.json)                    | Registers the BPDM policies, assets and contract definitions on a provider EDC |
| [EDC BPDM Consumer](EDC%20BPDM%20Consumer.postman_collection.json)                      | Negotiates access to those offers and obtains a transfer token             |

Both are documentation, not automated tests.

## Why The BPDM API Requests Are Not In The Collection

The consumer collection used to carry a copy of the BPDM Pool and Gate endpoints, one folder per
asset. Those copies were maintained by hand and went stale on every API change.

The BPDM APIs are already documented as Open-API documents in [docs/api](../api), generated from the
running applications. Rather than duplicating them, import the document you need and point it at the
EDC data plane. The consumer collection keeps only the part that no Open-API document describes: the
EDC negotiation dance.

## Setting Up The Consumer Collection

### Environment Variables

Create a Postman environment with at least these variables. Note that Postman only exports the
**Shared Value** column, so fill that one in if you intend to share the environment; leave
credentials in the local **Value** column only.

| Variable                      | Example                                              |
|-------------------------------|------------------------------------------------------|
| `CONSUMER_EDC_MANAGEMENT_API` | `https://your-edc.example.net/management`             |
| `CONSUMER_EDC_API_KEY`        | the consumer EDC management key (keep local)          |
| `PROVIDER_EDC_DATASPACE_API`  | `https://bpdm-edc.example.net/api/v1/dsp/2025-1`      |
| `PROVIDER_DID`                | `did:web:portal-backend...:BPNL00000003CRHK`          |
| `CONSUMER_BPNL`               | your BPNL                                             |

### Getting A Transfer Token

The `Negotiate for Access` folder is ordered as the flow runs:

1. **Select Asset** — pick the offer you want. Each request stores the asset, the offer and the
   usage purpose, and restores any agreement previously negotiated for that asset. Run this whenever
   you switch assets.
2. **Negotiate** — run once per asset, ever. `Negotiate Selected Asset` starts the negotiation,
   `Confirm Agreement` polls until it is `FINALIZED` and stores the agreement under
   `AGREEMENT_<ASSET>`.
3. **Access** — run whenever a token expires. `Find Transfer Process` locates the transfer belonging
   to the agreement, `Get Transfer Token` fetches the token. `Start New Transfer` is only needed when
   the previous transfer is gone.

Agreements and tokens are stored per asset, so you can hold access to several assets at once without
re-negotiating. The token lands in `TRANSFER_TOKEN_<ASSET>` and the data plane address in `baseUrl`.

## Adding The BPDM APIs From The Open-API Documents

### Import The Document

Each BPDM service publishes an Open-API document per user group, holding only the endpoints that
user group may call. Import the group matching your asset and the generated collection is already
scoped to it — nothing to prune:

| Asset                                   | Service | Access group document                     |
|-----------------------------------------|---------|-------------------------------------------|
| `ReadAccessPoolForDataSpaceParticipant` | Pool    | `/docs/api-docs/v7-participant`            |
| `FullAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-manager`          |
| `ReadAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-consumer`         |
| `ReadAccessGateOutputForSharingMember`  | Gate    | `/docs/api-docs/v7-output-consumer`        |

Fetch the document from a running application and hand the file to Postman's *Import*, or paste the
URL directly. The same groups appear in the Swagger UI dropdown, which is the quickest way to check
what an asset exposes without importing anything.

The full documents for both services remain available as [pool.json](../api/pool.json) and
[gate.json](../api/gate.json), along with [orchestrator.json](../api/orchestrator.json) for the
Orchestrator, which is not exposed over EDC.

Postman generates a collection with one folder per Open-API tag. Re-import to refresh it after an
API change instead of editing requests by hand.

### Point The Collection At The EDC Data Plane

The EDC data plane proxies to the asset's backend, so the imported collection needs two edits — both
on the collection, not per request:

1. **Variables** — set the generated `baseUrl` variable to `{{baseUrl}}`, which
   `Get Transfer Token` fills with the data plane address from the EDR.
2. **Authorization** — set the collection auth to API Key, with key `Authorization` and value
   `{{TRANSFER_TOKEN_<ASSET>}}` for the asset you negotiated, for example
   `{{TRANSFER_TOKEN_POOL_PARTICIPANT_READ}}`. Do not send a Keycloak token; the data plane injects
   the backend credentials itself.

The imported requests then run against the proxy exactly as they would against the API directly.
Mind that Postman fills required parameters with generated placeholder values on import, so query
parameters and request bodies still need real values.

### How The Access Groups Are Derived

Every endpoint declares the permission it requires in its `@PreAuthorize` annotation, and each access
group is a set of permissions. An endpoint appears in a group exactly when the permission it requires
belongs to that group, so the documents cannot drift from what the applications enforce.

| Service | Group                 | Permissions                                                                                                                                    |
|---------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Pool    | `v7-participant`      | `read_partner_member`, `read_changelog_member`, `read_metadata`                                                                                  |
| Pool    | `v7-sharing-member`   | `read_partner`, `read_changelog`, `read_metadata`                                                                                                |
| Pool    | `v7-admin`            | all Pool permissions                                                                                                                             |
| Gate    | `v7-input-consumer`   | `read_input_partner`, `read_input_changelog`, `read_input_relation`, `read_sharing_state`, `read_stats`                                           |
| Gate    | `v7-output-consumer`  | `read_output_partner`, `read_output_changelog`, `read_sharing_state`, `read_stats`                                                               |
| Gate    | `v7-input-manager`    | the input consumer permissions plus `write_input_partner`, `upload_input_partner`, `write_input_relation`, `write_sharing_state`                  |
| Gate    | `v7-admin`            | all Gate permissions                                                                                                                             |

The groups mirror the composite roles a technical user holds in Keycloak. If an asset's user is
granted a different role than the table assumes, the group document will describe access the
application then refuses, so keep the two in step. `AccessGroupOpenApiIT` in each service asserts
that every v7 endpoint belongs to at least one group, which catches an endpoint whose permission was
never added to any of them.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 SAP SE
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
