# Postman Collections

This folder contains Postman collections documenting how to reach the BPDM APIs through an EDC.

<!-- TOC -->
* [Postman Collections](#postman-collections)
  * [What Is In Here](#what-is-in-here)
  * [Setting Up The Consumer Collection](#setting-up-the-consumer-collection)
    * [Environment Variables](#environment-variables)
    * [Getting A Transfer Token](#getting-a-transfer-token)
  * [Adding The BPDM APIs From The Open-API Documents](#adding-the-bpdm-apis-from-the-open-api-documents)
    * [Import The Document](#import-the-document)
    * [Point The Collection At The EDC Data Plane](#point-the-collection-at-the-edc-data-plane)
  * [NOTICE](#notice)
<!-- TOC -->

## What Is In Here

| Collection                                                           | Purpose                                                                        |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------|
| [EDC Provider Setup](EDC%20Provider%20Setup.postman_collection.json) | Registers the BPDM policies, assets and contract definitions on a provider EDC |
| [EDC BPDM Consumer](EDC%20BPDM%20Consumer.postman_collection.json)   | Negotiates access to those offers and obtains a transfer token                 |

Both are documentation, not automated tests.

## Setting Up The Consumer Collection

### Environment Variables

Create a Postman environment with at least these variables. Note that Postman only exports the
**Shared Value** column, so fill that one in if you intend to share the environment; leave
credentials in the local **Value** column only.

| Variable                      | Example                                          |
|-------------------------------|--------------------------------------------------|
| `CONSUMER_EDC_MANAGEMENT_API` | `https://your-edc.example.net/management`        |
| `CONSUMER_EDC_API_KEY`        | the consumer EDC management key (keep local)     |
| `PROVIDER_EDC_DATASPACE_API`  | `https://bpdm-edc.example.net/api/v1/dsp/2025-1` |
| `PROVIDER_DID`                | `did:web:portal-backend...:BPNL00000003CRHK`     |
| `CONSUMER_BPNL`               | your BPNL                                        |

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

| Asset                                   | Service | Access group document               |
|-----------------------------------------|---------|-------------------------------------|
| `ReadAccessPoolForDataSpaceParticipant` | Pool    | `/docs/api-docs/v7-participant`     |
| `FullAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-manager`   |
| `ReadAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-consumer`  |
| `ReadAccessGateOutputForSharingMember`  | Gate    | `/docs/api-docs/v7-output-consumer` |

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
