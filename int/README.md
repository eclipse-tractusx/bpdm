# INT Environment Walkthrough

How to set up the BPDM snapshot landscape on INT from scratch, on the Tractus-X test environment
(`*.test.tractus-x.com`). The snapshot is a complete golden record process built from `main`: core,
three test sharing member Gates, a provider EDC offering those Gates, and an EDC per member.

The steps follow their dependencies: Portal access and companies first, then the app and its
subscriptions, which produce the credentials the vault needs, then ArgoCD, which deploys what the vault
resolves.

> **Release variant.** INT also runs the released chart for release validation: member Gates on
> `business-partners.test.tractus-x.com` (`int/sharing-members/`) and the provider EDC `edc-bpdm`
> (`int/edc/operator`). They share as much as possible with the snapshot: the member companies, the
> technical users, the vault secrets and most of the values. What differs are the deployments: their
> own ArgoCD apps, hosts and databases. The member EDCs serve both variants. Compare the folders under
> `int/` for the details.

<!-- TOC -->
* [INT Environment Walkthrough](#int-environment-walkthrough)
  * [The Landscape](#the-landscape)
  * [1. Access to the Operator Company](#1-access-to-the-operator-company)
  * [2. Onboard the Three Test Sharing Members](#2-onboard-the-three-test-sharing-members)
  * [3. Register the Golden Record Process App](#3-register-the-golden-record-process-app)
  * [4. Subscribe the Test Sharing Members](#4-subscribe-the-test-sharing-members)
  * [5. Set Up the Vault](#5-set-up-the-vault)
    * [Core](#core)
    * [Member Gates](#member-gates)
    * [EDCs](#edcs)
  * [6. Set Up ArgoCD](#6-set-up-argocd)
  * [7. Create the EDC Offers](#7-create-the-edc-offers)
  * [8. Run the End-to-End Tests](#8-run-the-end-to-end-tests)
  * [NOTICE](#notice)
<!-- TOC -->

## The Landscape

| Participant            | BPNL               | Gate                                                                          | EDC                                            |
|------------------------|--------------------|-------------------------------------------------------------------------------|------------------------------------------------|
| Operator               | `BPNL00000003CRHK` | —                                                                             | `bpdm-snapshot-edc.test.tractus-x.com`         |
| Test sharing member    | `BPNL00000000071A` | `business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member`   | `bpdm-sharing-member-edc.test.tractus-x.com`   |
| Test sharing member 2  | `BPNL00000000080L` | `business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member-2` | `bpdm-sharing-member-2-edc.test.tractus-x.com` |
| Test sharing member 3  | `BPNL0000000009Z7` | `business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member-3` | `bpdm-sharing-member-3-edc.test.tractus-x.com` |

The core runs on the same host: Pool at `/pool`, Orchestrator at `/orchestrator`, plus the cleaning
service dummy. Images are `latest-SNAPSHOT`, pulled on every sync.

The BPNLs belong to the companies the test environment operators created for this setup. They are not
chosen: the operator company comes with the environment, and each test sharing member's BPNL is
assigned when its company is created on request ([step 1](#1-access-to-the-operator-company)). On a new
environment, or for a setup from scratch, the companies and therefore the BPNLs are new, and the
configuration has to be changed to match ([step 2](#2-onboard-the-three-test-sharing-members) lists
where).

Other services the configuration depends on:

| Service                        | URL                                              |
|--------------------------------|--------------------------------------------------|
| Central IdP (realm CX-Central) | `https://centralidp.test.tractus-x.com/auth`     |
| SSI DIM wallet stub            | `https://ssi-dim-wallet-stub.test.tractus-x.com` |
| Vault                          | `https://vault.test.tractus-x.com`               |
| Portal                         | `https://portal.test.tractus-x.com`              |
| ArgoCD                         | `https://argocd.test.tractus-x.com`              |

## 1. Access to the Operator Company

The BPDM maintainer acts in the environment's operator company, `Cx-Operator` (`BPNL00000003CRHK`).
That company registers the BPDM app in the marketplace, activates subscriptions, and owns the technical
users the core and the end-to-end tests run with.

1. Get invited to the operator company by the test environment operators. The invitation mail links to
   the Portal registration, where you register your account.
2. In the same request, ask the test environment operators for **three companies** to act as the test
   sharing members. Each gets its own BPNL, which is where the member BPNLs come from.
   [Step 2](#2-onboard-the-three-test-sharing-members) continues with them.

Once you have an account, create the technical user for direct API access. The end-to-end suite and
debugging both use it:

| API          | Permission                |
|--------------|---------------------------|
| Pool         | `BPDM Pool Admin`         |
| Gate         | `BPDM Sharing Admin`      |
| Orchestrator | `BPDM Orchestrator Admin` |

## 2. Onboard the Three Test Sharing Members

Each test sharing member is a company of its own. The tester refuses to run two members with the same
BPNL, and the Gates scope their output by the BPNL of the caller's token, so members cannot share a
company.

For each of the three companies requested in [step 1](#1-access-to-the-operator-company), you receive a
company onboarding invitation mail. Follow it and complete the onboarding:

- **Address:** fill in every field, including the optional ones. Street and post code are free to
  choose, but the country and region codes must be valid ISO codes (ISO 3166-1 and ISO 3166-2). For
  the smoothest process, use Germany (`DE`) for all three.
- **Identifier:** use a VAT ID that exists. Take an arbitrary one from a public company registry; a
  made-up number can fail validation.
- **Company role:** mark each company as a **dataspace participant**.

Then wait for the test environment operators to finish the onboarding. You receive a mail for each
company when it is done, and only then is the company usable for the next steps.

A new setup produces new BPNLs, which must replace the ones in the configuration everywhere they
appear:

- `int/snapshot/member-*/values.yaml` and `int/sharing-members/member-*/values.yaml`:
  `bpdm.bpn.owner-bpn-l`
- `int/edc/member-*/values.yaml`: `participant.id`, `participant.bpnl`, `dcp.sts.oauth.client.id`
- `int/edc/member-*/*.postman_environment.json`: `CONSUMER_BPNL`, `CONSUMER_DID`
- `bpdm-system-tester/src/main/resources/application-int-edc.yml` on `main`

## 3. Register the Golden Record Process App

Once the companies are onboarded, log in to the operator company. The operator offers the golden record
process as an app on the marketplace, and the test sharing members subscribe to it. A subscription is
the only way the Portal issues technical users that carry a **member's** identity, which both the
member's Gate and the provider EDC need to act on that member's behalf.

1. Open **App Management → App Release Process**.
2. Fill in the required fields. Give the app a speaking name such as `Golden Record Process`.
   Descriptions, images and the other fields can hold placeholders.
3. Move through the following steps, filling them with placeholder values and documents.
4. In **Technical Integration**, keep the default roles, and create **five technical user profiles**:

   | Profile                   | Type                                  | Portal role                      | BPDM permission group      | Serves                                       |
   |---------------------------|---------------------------------------|----------------------------------|----------------------------|----------------------------------------------|
   | Pool Consumer             | internal, invisible to the subscriber | `BPDM Pool Consumer`             | Pool Dataspace Participant | Provider EDC: the member's Pool offer        |
   | Sharing Input Manager     | internal, invisible to the subscriber | `BPDM Sharing Input Manager`     | Gate Input Manager         | Provider EDC: the member's Gate input offer  |
   | Sharing Output Consumer   | internal, invisible to the subscriber | `BPDM Sharing Output Consumer`   | Gate Output Consumer       | Provider EDC: the member's Gate output offer |
   | Orchestrator Task Creator | internal, visible to the subscriber   | `BPDM Orchestrator Task Creator` | Orchestrator Task Creator  | Member Gate → Orchestrator                   |
   | Pool Sharing Consumer     | internal, visible to the subscriber   | `BPDM Pool Sharing Consumer`     | Pool Sharing Member        | Member Gate → Pool                           |

   Each profile becomes one technical user per subscribing company, so every subscription creates
   five distinct technical users. Fewer profiles would make several purposes share one user, and with
   it one set of permissions.

   The permission groups are BPDM's own, defined in the [crosscutting
   concepts](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/architecture/08_Crosscutting_Concepts.md#bpdm-permission-groups).
   Note the two Pool roles: `BPDM Pool Consumer` reads only the golden records of dataspace
   participants, which is what the dataspace offer standardises, while the Gate needs all of them and
   gets `BPDM Pool Sharing Consumer`.

5. Finish the registration and submit it.
6. Approve the release yourself under **App Management → Admin Board**. You can, because your account is
   both the app provider and a Portal operator account.

## 4. Subscribe the Test Sharing Members

1. For each test sharing member: log in to that company, open the app marketplace, and subscribe to the
   app released in [step 3](#3-register-the-golden-record-process-app).
2. Log in to the operator company again and approve the pending subscriptions under
   **App Management → Subscription Management**.
3. Once a subscription is active, all five of its technical users are listed in the **operator
   company** under **My Company → User Management → Technical Users**, the invisible and the visible
   ones alike. As the golden record process provider, the operator sets up the member's Gate and EDC
   offers, so it holds every credential they need.

These technical users are the foundation of the vault configuration in
[step 5](#5-set-up-the-vault). Every member's five end up in the vault (`<member>` is
`test-sharing-member`, `test-sharing-member-2` or `test-sharing-member-3`):

| Technical user            | Vault secret                                                                  |
|---------------------------|-------------------------------------------------------------------------------|
| Pool Consumer             | `int/edc-bpdm/asset-secrets/<member>/pool-member-read#content`       |
| Sharing Input Manager     | `int/edc-bpdm/asset-secrets/<member>/input-full-access#content`      |
| Sharing Output Consumer   | `int/edc-bpdm/asset-secrets/<member>/output-read-access#content`     |
| Pool Sharing Consumer     | `int/bpdm-<member>/gate/pool-client#client-id`, `#client-secret`              |
| Orchestrator Task Creator | `int/bpdm-<member>/gate/orchestrator-client#client-id`, `#client-secret`      |

The first three are read by the provider EDC at runtime, which hands them to the member's Gate when the
member consumes an offer. The last two are resolved by ArgoCD into the member's Gate deployment.

Note the client IDs of the first three as well. They go into the Postman environments in
[step 7](#7-create-the-edc-offers), and the Sharing Input Manager and Sharing Output Consumer pairs
are the tester's Gate credentials in [step 8](#8-run-the-end-to-end-tests).

The members need no wallet technical user. Every connector authenticates against the SSI DIM wallet
stub, which takes the BPNL as client ID and does not check the secret (see `int/edc/values.yaml`).

## 5. Set Up the Vault

All secrets live in the `bpdm` engine of `https://vault.test.tractus-x.com`, under `int/`. Create them
without the `data/` segment that the references in the values files carry.

Once you have an account in the operator company, ask the test environment operators for access to the
`bpdm` engine. You log in to the vault over OIDC through the Central IdP, with the same account as for
the Portal.

Two kinds of secrets:

- **Resolved by ArgoCD at sync time**: the `<path:bpdm/data/int/…#key>` references in the values files.
- **Read by the EDC at runtime**: always under the value name `content`, below the connector's
  `vault.hashicorp.paths.folder`.

### Core

```
int/bpdm/postgresql#postgres-password                         (free to choose)
int/bpdm/postgresql#password                                  (free to choose)
int/bpdm/pool/orchestrator-client#client-id                   (sa-cl25-cx-2)
int/bpdm/pool/orchestrator-client#client-secret
int/bpdm/cleaning-service/orchestrator-client#client-id       (sa-cl25-cx-1)
int/bpdm/cleaning-service/orchestrator-client#client-secret
```

The core's Orchestrator clients come from technical users the operator company is seeded with for the
core golden record process, listed under **My Company → User Management → Technical Users**:
`sa-cl25-cx-2` for the Pool and `sa-cl25-cx-1` for the cleaning service dummy.

### Member Gates

For each of `bpdm-test-sharing-member`, `bpdm-test-sharing-member-2`, `bpdm-test-sharing-member-3`:

```
int/<member>/postgresql#postgres-password              (free to choose)
int/<member>/postgresql#password                       (free to choose)
int/<member>/gate/pool-client#client-id                (Pool Sharing Consumer, step 4)
int/<member>/gate/pool-client#client-secret
int/<member>/gate/orchestrator-client#client-id        (Orchestrator Task Creator, step 4)
int/<member>/gate/orchestrator-client#client-secret
```

### EDCs

For each of `edc-bpdm`, `edc-test-sharing-member`, `edc-test-sharing-member-2`,
`edc-test-sharing-member-3`:

```
int/<app>/api#management-key            (free to choose; the tester and Postman need it)
int/<app>/api#proxy-key                 (free to choose)
int/<app>/postgres#password             (free to choose)
int/<app>/token-private-key#content     (RSA private key, PEM)
int/<app>/token-public-key#content      (RSA public key, PEM)
int/<app>/wallet-client-secret#content  (any value; the wallet stub does not check it)
```

Plus the asset secrets of the provider EDC from [step 4](#4-subscribe-the-test-sharing-members).

The snapshot provider EDC `edc-bpdm-snapshot` has no secrets of its own. Its values layer on top of
`edc-bpdm`'s (`int/edc/operator`), so it uses the `int/edc-bpdm/` folder, and overrides only its host
and connector context id.

The token key pair is free to create:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out token-private-key.pem
openssl rsa -in token-private-key.pem -pubout -out token-public-key.pem
```

Each EDC reads its folder with a vault token, kept in two places: `int/edc-bpdm/vault#token` for the
provider EDC, and `int/edc-test-sharing-member/vault#token` for all three member EDCs. Your own vault
token works. A token from an OIDC login expires, though, and the connectors stop reading their secrets
when it does. If that happens, or the token lacks access, ask the test environment operators for a
separate long-lived token for the `bpdm` EDC paths.

## 6. Set Up ArgoCD

ArgoCD runs at `https://argocd.test.tractus-x.com`. Access works like the vault: once you have an
account in the operator company, ask the test environment operators for access, then log in through
the Central IdP with the same credentials.

Every app goes into the project `bpdm` and the namespace `bpdm`. Create each one from its `spec.yaml`
under the name below, which is the Helm release name: a different name starts on an empty database.

Synchronize in this order:

| # | ArgoCD app                                     | Folder                         |
|---|------------------------------------------------|--------------------------------|
| 1 | `bpdm-snapshot`                                | `int/snapshot/core`            |
| 2 | `bpdm-snapshot-test-sharing-member`, `-2`, `-3` | `int/snapshot/member-1` … `-3` |
| 3 | `edc-bpdm-snapshot`                            | `int/edc/operator-snapshot`    |
| 4 | `edc-test-sharing-member`, `-2`, `-3`          | `int/edc/member-1` … `-3`      |

The member Gates need the core, and the offers in the next step need the provider EDC and the Gates.
ArgoCD shows the resources before creating them. Review them, synchronize, and wait for green. No app
synchronizes on its own.

## 7. Create the EDC Offers

With the provider EDC running, create the offers for each member using the [EDC Provider Setup Postman
collection](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/admin/EDC%20Provider%20Setup.postman_collection.json)
from `main`. Each member has a snapshot Postman environment next to its EDC,
`int/edc/member-*/Test Sharing Member* Snapshot.postman_environment.json`. It holds both sides: the
provider variables to create that member's offers on `edc-bpdm-snapshot`, and the consumer variables
to negotiate them through the member's own EDC with the [EDC BPDM Consumer
collection](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/api/EDC%20BPDM%20Consumer.postman_collection.json).

Fill in the empty `CLIENT_ID_…` variables with the client IDs from step 4, and the API keys with the
`api#management-key` values. Do not commit them: the API keys are secrets, and the files are public.
The `CLIENT_SECRET_PATH_…` variables are vault paths, not secrets; the EDC resolves them itself.

## 8. Run the End-to-End Tests

How to run the suite, its pitfalls and the Jira reporting are covered in the [End-to-End Testing
guide](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/maintainer/e2e-testing.md) on `main`.
Its *Trying the suite on the snapshot deployment first* section covers this deployment. This
environment supplies the credentials:

| Variable                                 | Source                                                        |
|------------------------------------------|---------------------------------------------------------------|
| `BPDM_INT_CLIENT_ID` / `_SECRET`         | The operator's technical user, step 1                         |
| `BPDM_INT_GATE_INPUT_CLIENT_*`           | Member 1's Sharing Input Manager, step 4                      |
| `BPDM_INT_GATE_OUTPUT_CLIENT_*`          | Member 1's Sharing Output Consumer, step 4                    |
| `BPDM_INT_GATE_2_*`, `BPDM_INT_GATE_3_*` | The same pair for members 2 and 3, with the member's base URL |
| `BPDM_INT_EDC_*_API_KEY`                 | The member EDCs' `api#management-key`, step 5                 |

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
