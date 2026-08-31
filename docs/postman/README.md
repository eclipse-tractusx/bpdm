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
    * [Which Endpoints An Asset Actually Grants](#which-endpoints-an-asset-actually-grants)
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

In Postman choose *Import* and select the document for the API behind your asset:

| Asset                                     | Open-API document          |
|-------------------------------------------|----------------------------|
| `BPDMPool`                                | [pool.json](../api/pool.json)       |
| `BPDMGate`                                | [gate.json](../api/gate.json)       |
| (not exposed over EDC)                    | [orchestrator.json](../api/orchestrator.json) |

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

### Which Endpoints An Asset Actually Grants

An imported document describes the whole API, while an asset only grants the subset its technical
user is permitted to call. Everything else answers `403`. The permissions come from composite roles
in Keycloak:

| Asset                                   | Consumer variable suffix | Composite role              | Grants                                                                                                   |
|-----------------------------------------|--------------------------|-----------------------------|----------------------------------------------------------------------------------------------------------|
| `ReadAccessPoolForDataSpaceParticipant` | `POOL_PARTICIPANT_READ`  | `BPDM_POOL:participant`     | `read_partner_member`, `read_changelog_member`, `read_metadata`                                            |
| `FullAccessGateInputForSharingMember`   | `GATE_INPUT_FULL`        | `BPDM_GATE:input_manager`   | `read_input_partner`, `write_input_partner`, `read_input_changelog`, `read_sharing_state`, `write_sharing_state`, `read_stats` |
| `ReadAccessGateInputForSharingMember`   | `GATE_INPUT_READ`        | `BPDM_GATE:input_consumer`  | `read_input_partner`, `read_input_changelog`, `read_sharing_state`, `read_stats`                           |
| `ReadAccessGateOutputForSharingMember`  | `GATE_OUTPUT_READ`       | `BPDM_GATE:output_consumer` | `read_output_partner`, `read_output_changelog`, `read_sharing_state`, `read_stats`                         |

Each endpoint declares the permission it requires in its `@PreAuthorize` annotation, so an endpoint
is reachable through an asset exactly when that permission appears in the row above. The
[API documentation](../api/README.md) describes the same grouping as user groups.

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
