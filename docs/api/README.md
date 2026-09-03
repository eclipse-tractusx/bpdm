# API Documentation

<!-- TOC -->
* [API Documentation](#api-documentation)
  * [BPDM APIs](#bpdm-apis)
    * [Pool API](#pool-api)
    * [Gate API](#gate-api)
      * [Additional information](#additional-information)
    * [Orchestrator API](#orchestrator-api)
    * [Authorization](#authorization)
  * [Access BPDM over EDC](#access-bpdm-over-edc)
    * [Negotiating For A Data Offer](#negotiating-for-a-data-offer)
    * [Reaching The BPDM APIs With The Transfer Token](#reaching-the-bpdm-apis-with-the-transfer-token)
      * [Setting Up An Imported Collection](#setting-up-an-imported-collection)
  * [NOTICE](#notice)
<!-- TOC -->

Here you can find documentation on how to access and integrate BPDM APIs.

## BPDM APIs

This section details concepts for the BPDM APIs.

### Pool API

With the [Pool API](pool.yaml) you can query golden record and available metadata information like legal forms and identifier types.
Dataspace participants use it to resolve the BPNs they receive, and it is also of interest to sharing members who want to see which metadata information the golden record process provider supports.
Have a look at the corresponding [BPDM Pool API standard](https://catenax-ev.github.io/docs/standards/CX-0012-BusinessPartnerDataPoolAPI) for more information.

The golden record levels, the metadata lists and what a dataspace participant may read of them are
explained from a dataspace participant's perspective in the
[Dataspace Participant Guide](dataspace-participant-guide.md).

### Gate API

With the [Gate API](gate.yaml) you can share business partner data with the golden record process and query the results.
This API is important for sharing members.
More general information can be obtained from the [BPDM Gate standard](https://catenax-ev.github.io/docs/standards/CX-0074-BusinessPartnerGateAPI).

The generic business partner format, the two data stages, the sharing state and the changelog are
explained from a sharing member's perspective in the [Sharing Member Guide](sharing-member-guide.md).

#### Additional information

The Gate API only works in context of one sharing member at a time.
This means you only ever see the business partner data of one sharing member through it.

The full implementation of the Golden Record Process - which includes duplication checks, categorizing and cleaning of data - is not yet provided in this repository.
Instead, BPDM offers a dummy golden record processing service that performs rudimentary checks and processing to offer a limited Golden Record Process without relying on an external provider.
Since the dummy service is very limited, using the BPDM API behind such a dummy golden record process comes with [restrictions](../architecture/11_Risks_And_Technical_Debts.md).


> NOTE:
> The dummy refines business partners only.
> It reserves relation tasks as well but passes them through unchanged, so a golden record process for business partner relations requires an actual refinement service to be integrated.


### Orchestrator API

With the [Orchestrator API](orchestrator.yaml) you retrieve and resolve business partner data being processed inside the golden record process.

What a golden record task is, what its business partner data can express, what to put into a step
result, and how a refinement service is granted access to this API are explained from a refinement
service provider's perspective in the
[Refinement Service Provider Guide](refinement-service-guide.md).
Unlike the Pool and the Gate, the Orchestrator is not exposed over an EDC.

### Authorization

All three APIs are OAuth2 resource servers.
Every request carries a bearer token, and which endpoints a token may call follows from the
permissions behind it; the guides linked above say which endpoints each role calls.
The Pool and the Gate additionally publish an Open-API document per role, holding exactly the
endpoints that role may call - the quickest way to see what a set of credentials is good for.
Where you reach an API through an EDC, the data plane supplies the credentials itself and you send
the transfer token instead; see [Access BPDM over EDC](#access-bpdm-over-edc).

## Access BPDM over EDC

Some users can not directly access the BPDM API but may only do so over the EDC public API.
This section details how a sharing member EDC can access an EDC exposing the BPDM API as assets. Before you can access the assets make sure that the BPDM EDC
has been configured to [provide assets for your company's BPN](../../INSTALL.md).
Offers are separated into purposes (defined in the BPDM framework agreement) on why you want to access the BPDM API.
First, you need to select the offer based on your purpose.
Afterward you can negotiate for a contract agreement in order to get access to the data.
The final result of that negotiation will be a transfer token with which you can navigate the BPDM APIs over the BPDM EDC's public API (which acts as a proxy).

The [EDC BPDM Consumer Postman collection](EDC%20BPDM%20Consumer.postman_collection.json) documents that negotiation.
It is documentation, not an automated test.

### Negotiating For A Data Offer

Set up a Postman environment with at least the following variables.
Mind that Postman only exports the shared value of a variable, so credentials you keep local are not part of an exported environment.

| Variable                      | Example                                          |
|-------------------------------|--------------------------------------------------|
| `CONSUMER_EDC_MANAGEMENT_API` | `https://your-edc.example.net/management`        |
| `CONSUMER_EDC_API_KEY`        | the consumer EDC management key                  |
| `PROVIDER_EDC_DATASPACE_API`  | `https://bpdm-edc.example.net/api/v1/dsp/2025-1` |
| `PROVIDER_DID`                | the provider's decentralized identifier          |
| `CONSUMER_BPNL`               | your BPNL                                        |

The `Negotiate for Access` folder is ordered as the flow runs:

1. **Select Asset**: pick the offer for your purpose.
   Each request stores the asset, the offer and the purpose, and restores any agreement previously negotiated for that asset.
   Run it whenever you switch assets.
2. **Negotiate**: run once per asset, ever.
   `Negotiate Selected Asset` starts the negotiation and `Confirm Agreement` polls until it is finalized, storing the agreement per asset.
3. **Access**: run whenever a token expires.
   `Find Transfer Process` locates the transfer belonging to the agreement and `Get Transfer Token` fetches the token.
   `Start New Transfer` is only needed when the previous transfer is gone.

Because agreements and tokens are stored per asset, you can hold access to several assets at once without negotiating again.
The token lands in `TRANSFER_TOKEN_<ASSET>` and the address of the EDC data plane in `baseUrl`.

### Reaching The BPDM APIs With The Transfer Token

The consumer collection deliberately contains no BPDM API requests.
Instead, the Pool and the Gate publish an Open-API document per user group which holds only the endpoints that user group may call.
Importing the group that matches your asset into Postman gives you a collection already scoped to that asset:

| Asset                                   | Service | Access group document               |
|-----------------------------------------|---------|-------------------------------------|
| `ReadAccessPoolForDataSpaceParticipant` | Pool    | `/docs/api-docs/v7-participant`     |
| `FullAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-manager`   |
| `ReadAccessGateInputForSharingMember`   | Gate    | `/docs/api-docs/v7-input-consumer`  |
| `ReadAccessGateOutputForSharingMember`  | Gate    | `/docs/api-docs/v7-output-consumer` |

The same groups appear in the Swagger-UI dropdown of a running application, which is the quickest way to see what an asset exposes without importing anything.
An endpoint belongs to a group exactly when the permission it requires is one of the group's permissions, so these documents describe what the application actually enforces.

#### Setting Up An Imported Collection

None of this is guessable from the import dialog, and skipping any one step produces a failure that points somewhere else.
Work through it once per access group.

**1. Select the environment you negotiated with.**
The imported collection is a collection of its own and cannot read the consumer collection's collection variables.
`Get Transfer Token` therefore publishes the three values that have to cross that boundary — `baseUrl`, `TRANSFER_TOKEN` and `TRANSFER_TOKEN_<ASSET>` — as *environment* variables, and refuses to run with no environment selected.
Both collections read them from that one environment.
The tokens are written as current values, so they stay local and do not reach the checked-in environment file when you export it.

**2. Import the access group document** from the table above, for example `https://<host>/pool/docs/api-docs/v7-participant`, with *Import → Link* (or save it to a file and import that).
Postman creates a collection whose requests are exactly the endpoints that asset may call.

**3. Set the collection's authorization**, on the collection itself — not on a request:

| Field | Value |
|-------|-------|
| Type | **API Key** |
| Key | `Authorization` |
| Value | `{{TRANSFER_TOKEN_<ASSET>}}`, e.g. `{{TRANSFER_TOKEN_POOL_PARTICIPANT_READ}}` |
| Add to | **Header** |

Three things go wrong here in particular:

- **Name the asset's own token variable, not the generic `TRANSFER_TOKEN`.** The generic one holds whichever asset was selected last, so a Pool request sent with a Gate token reaches the Gate backend and the data plane answers `Failed to read data from source: NOT_FOUND` — an error that names no URL and looks like a wrong path.
- **Use API Key, not Bearer Token.** Bearer Token prepends `Bearer ` to a value that already carries whatever scheme the connector issued.
- **Leave each request at *Inherit auth from parent*.** A collection imported from an OpenAPI document carries that document's security scheme per request, and request-level auth wins over collection-level, so the collection setting is silently ignored until you clear it.

Do not send a Keycloak token here at all; the data plane injects the backend credentials itself.

**4. Remove the leading `/v7` from the path of each request.**
An asset points at the API's `/v7` path already and the data plane appends the path it is called with, so a request left as imported arrives as `/v7/v7/...`.
Which version an offer serves is fixed by the asset, not by the path you send.

`baseUrl` needs no editing: Postman resolves an environment variable ahead of a collection variable of the same name, so the environment's `baseUrl` overrides the one the import took from the document's server URL.

The imported requests then run against the data plane exactly as they would run against the API directly.
Mind that Postman fills required parameters with generated placeholder values on import, so query parameters and request bodies still need real values.

| The data plane answers | Most likely |
|------------------------|-------------|
| `401` | The request carried no `Authorization` header at all. |
| `Failed to read data from source: NOT_FOUND` | The token belongs to another asset, or the path still carries its leading `/v7`. The backend URL the data plane built is in its own log. |
| `403` | The transfer token expired, or the offer's access policy does not name your BPNL, or the endpoint needs a permission the asset's technical user does not hold. A data plane answers every token it will not accept the same way, so try `Get Transfer Token` first - it requests with `auto_refresh=true` and republishes a fresh one without renegotiating - and read the body for the reason when a fresh token is refused too. |

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