# Association Environments

BPDM maintains deployments on the Catena-X association environments.
They serve the association's Portal, give sharing members something to integrate against, and are the target of the [end-to-end tests](e2e-testing.md) that validate every release.

Keeping them running, upgraded and configured is a maintainer duty.
This guide describes what runs where and how it is deployed; the deployment configuration itself lives on the [`environments` branch](https://github.com/eclipse-tractusx/bpdm/tree/environments).

<!-- TOC -->
* [Association Environments](#association-environments)
  * [What Runs Where](#what-runs-where)
  * [How a Deployment Is Defined](#how-a-deployment-is-defined)
  * [Secret Management](#secret-management)
  * [Setting Up the Golden Record Process](#setting-up-the-golden-record-process)
  * [Operator API Access](#operator-api-access)
  * [Setting Up a Sharing Member Gate](#setting-up-a-sharing-member-gate)
  * [Setting Up the EDC](#setting-up-the-edc)
  * [Feature Branch Deployments](#feature-branch-deployments)
  * [Open Items](#open-items)
<!-- TOC -->

## What Runs Where

The association offers two environments, both operated through ArgoCD:

- **INT** — the environment the maintainer keeps current. It always follows the newest release version, and it is where release candidates are validated by the [end-to-end tests](e2e-testing.md).
- **STABLE** — updated on demand, when an update is announced. It is expected to run an older line than INT.

### INT — [argocd.int.catena-x.net](https://argocd.int.catena-x.net)

| ArgoCD app                   | Purpose                                                    | Chart source                     | Root URL                                                                     |
|------------------------------|------------------------------------------------------------|----------------------------------|------------------------------------------------------------------------------|
| `bpdm`                       | Golden record core (Pool, Gate, Orchestrator, Cleaning)    | released chart                   | https://business-partners.int.catena-x.net/                                  |
| `bpdm-test-sharing-member`   | Gate for the test sharing member                           | released chart                   | https://business-partners.int.catena-x.net/companies/test-sharing-member     |
| `bpdm-test-sharing-member-2` | Gate for a second test sharing member                      | released chart                   | https://business-partners.int.catena-x.net/companies/test-sharing-member-2   |
| `bpdm-snapshot`              | Development state, chart taken from `main`                 | `main` branch                    | https://business-partners-snapshot.int.catena-x.net/                         |
| `edc-bpdm`                   | Operator EDC providing the BPDM assets                     | `tractusx-connector`             | https://bpdm-edc.int.catena-x.net                                            |
| `edc-test-sharing-member`    | Consumer EDC with the test sharing member's identity       | `tractusx-connector`             | https://bpdm-sharing-member-edc.int.catena-x.net                             |
| `edc-test-sharing-member-2`  | Consumer EDC for the second test sharing member            | `tractusx-connector`             | https://bpdm-sharing-member-2-edc.int.catena-x.net                           |

The Gate of the `bpdm` core deployment is served at `/companies/test-company` — that is the Portal's Gate, not a sharing member's.

### STABLE — [argocd.stable.catena-x.net](https://argocd.stable.catena-x.net)

| ArgoCD app                 | Purpose                                              | Root URL                                                                        |
|----------------------------|------------------------------------------------------|---------------------------------------------------------------------------------|
| `bpdm`                     | Golden record core                                   | https://business-partners.stable.catena-x.net/                                  |
| `bpdm-test-sharing-member` | Gate for the test sharing member                     | https://business-partners.stable.catena-x.net/companies/test-sharing-member     |
| `edc-bpdm`                 | Operator EDC providing the BPDM assets               | https://bpdm-edc.stable.catena-x.net                                            |
| `edc-test-sharing-member`  | Consumer EDC with the test sharing member's identity | https://bpdm-sharing-member-edc.stable.catena-x.net                             |

Read deployed chart versions from ArgoCD or from each app's `targetRevision`; they are not listed here because they change every release.

INT holds the accumulated data of every end-to-end run ever executed against it, [by design](e2e-testing.md#test-data-is-not-cleaned-up), so upgrading it exercises a real migration against a populated database.

## How a Deployment Is Defined

Each ArgoCD app is two files under [`<environment>/<app>/` on the `environments` branch](https://github.com/eclipse-tractusx/bpdm/tree/environments):

- **`spec.yaml`** — the ArgoCD application specification: which chart, from which repository, at which `targetRevision`, into which namespace. It also passes the values file to Helm as a raw URL and enables the vault plugin.
- **`values.yaml`** — the Helm values for that deployment: ingress hosts and context paths, the URLs the BPDM components use to reach each other, the Central-IDP configuration, and vault references for every secret.

Creating a new app in ArgoCD means creating it from the corresponding `spec.yaml`; changing an existing deployment means editing `values.yaml` and re-synchronizing.
Since ArgoCD fetches the values file over its raw URL at sync time, a change is live for the next synchronization as soon as it is merged.

### Why the configuration lives on its own branch

ArgoCD combines a `values.yaml` read at sync time with whichever chart version that deployment's `spec.yaml` pins — two versions chosen independently.
A values file is therefore correct for *its deployment's pinned chart*, not for the newest one, which inverts the rule everywhere else on `main`.
Off `main`, no chart pull request can sweep it along and no `release/X.Y.x` branch freezes a stale copy. `gh-pages` is the same arrangement.

Four rules follow:

- **Change a `values.yaml` only when its deployment is being upgraded** — during the release cycle for INT, on announcement for STABLE. If a chart change alters a values key, record it in the [migration guide](../admin/MIGRATION_GUIDE.md) and apply it to the values when the chart version in `spec.yaml` moves.
- **Read the chart, not its changelog**, before deciding what a chart change requires here. Chart 7.0.0 added generated Secrets behind `externalApplicationConfig` but kept `applicationConfig` / `applicationSecrets` in the subcharts, where they still take precedence. Every `values.yaml` sets them deliberately — do not migrate them to the new structure.
- **Never add `syncPolicy.automated`.** No `spec.yaml` declares a sync policy, so a merge changes only what *would* be deployed and the maintainer decides when it is. Automating it turns every merge into a live change on an association environment.
- **Scan a `values.yaml` by hand before merging it.** The branch sits outside the TruffleHog and KICS triggers, which name `main` only — [still outstanding](#open-items).

## Secret Management

No secret value is ever written into a `values.yaml`.
The association operates a central [HashiCorp Vault](https://vault.core.catena-x.net) — one instance for all environments — and the ArgoCD instances run a vault plugin that resolves references at sync time.

A reference has the form:

```
<path:bpdm/data/<environment>/<path/to/secret>#<secret-value>>
```

| Segment              | Meaning                                                                                                          |
|----------------------|-------------------------------------------------------------------------------------------------------------------|
| `bpdm`               | The secret engine holding all BPDM secrets, created by the association administrators                              |
| `data`               | The API path for reading secrets of an engine. It is not shown in the vault UI and must not be used when creating a secret |
| `<environment>`      | The environment the secret belongs to, e.g. `int` or `stable`                                                      |
| `<path/to/secret>`   | The secret's path. It is kept identical across environments so a values file differs only in the environment segment |
| `#<secret-value>`    | The named value inside the secret — one secret can hold several                                                    |

The paths follow the deployment structure: `<environment>/<deployment>/<component>/<purpose>`, for example `int/bpdm/gate/pool-client#client-secret`.

Secrets whose value name is `content` are a different mechanism: those are read by the EDC directly from the vault at runtime, not resolved by the ArgoCD plugin.

## Setting Up the Golden Record Process

The core deployment the Portal depends on for onboarding companies and serving its partner network and company data pages.
[INSTALL.md](../../INSTALL.md) covers generic chart installation; what follows is only what an association environment adds.

### 1. Gather the client credentials

The golden record core does not need new IdP clients — the Portal already provisions them.
Get an account for the `Cx-Operator` company in that environment's Portal (the team operating the association's Portal issues the invitation), then read the credentials of these existing technical users from the Portal's technical user management page:

| Client         | Belongs to                                    |
|----------------|-----------------------------------------------|
| `sa-cl7-cx-1`  | The Portal Gate's client to access the Pool   |
| `sa-cl25-cx-1` | Cleaning Service Dummy → Orchestrator         |
| `sa-cl25-cx-2` | Pool → Orchestrator                           |
| `sa-cl25-cx-3` | Gate → Orchestrator                           |

### 2. Create the vault secrets

The golden record components of one deployment share a single database, so two database passwords and the four client credential pairs are needed.
For an environment `<env>`:

```
<env>/bpdm/postgresql#postgres-password              (free to choose)
<env>/bpdm/postgresql#password                       (free to choose)
<env>/bpdm/gate/pool-client#client-id                (from sa-cl7-cx-1)
<env>/bpdm/gate/pool-client#client-secret            (from sa-cl7-cx-1)
<env>/bpdm/gate/orchestrator-client#client-id        (from sa-cl25-cx-3)
<env>/bpdm/gate/orchestrator-client#client-secret    (from sa-cl25-cx-3)
<env>/bpdm/pool/orchestrator-client#client-id        (from sa-cl25-cx-2)
<env>/bpdm/pool/orchestrator-client#client-secret    (from sa-cl25-cx-2)
<env>/bpdm/cleaning-service/orchestrator-client#client-id      (from sa-cl25-cx-1)
<env>/bpdm/cleaning-service/orchestrator-client#client-secret  (from sa-cl25-cx-1)
```

### 3. Deploy

Create a new app in that environment's ArgoCD from [`<env>/bpdm/spec.yaml`](https://github.com/eclipse-tractusx/bpdm/tree/environments).
ArgoCD previews the Kubernetes resources it would create; review them, then synchronize.
The app reaches a green state after a few minutes.

## Operator API Access

To reach the BPDM APIs directly — for debugging, or to run the [end-to-end tests](e2e-testing.md) — create a technical user in the Portal as a member of `Cx-Operator` and assign the BPDM permissions.
Full access to the three APIs of the golden record core:

| API          | Permission                 |
|--------------|----------------------------|
| Pool         | `BPDM Pool Admin`          |
| Portal Gate  | `BPDM Sharing Admin`       |
| Orchestrator | `BPDM Orchestrator Admin`  |

## Setting Up a Sharing Member Gate

Sharing members get their own Gate, deployed as a separate ArgoCD app with only the Gate component and its database enabled.

Unlike the core deployment, a sharing member Gate needs its own technical users, created in the Portal:

| Technical user           | Permission                        |
|--------------------------|-----------------------------------|
| Gate → Pool access       | `BPDM Pool Sharing Consumer`      |
| Gate → Orchestrator access | `BPDM Orchestrator Task Creator` |

Its Gate deploys its own database, so it needs its own database passwords:

```
<env>/bpdm-<member>/postgresql#postgres-password              (free to choose)
<env>/bpdm-<member>/postgresql#password                       (free to choose)
<env>/bpdm-<member>/gate/pool-client#client-id                (from the Pool access user)
<env>/bpdm-<member>/gate/pool-client#client-secret            (from the Pool access user)
<env>/bpdm-<member>/gate/orchestrator-client#client-id        (from the Orchestrator access user)
<env>/bpdm-<member>/gate/orchestrator-client#client-secret    (from the Orchestrator access user)
```

Then create the ArgoCD app from that member's `spec.yaml`.

## Setting Up the EDC

Sharing members reach their Gate over EDC communication.
That needs an operator EDC providing the [BPDM assets](../architecture/08_Crosscutting_Concepts.md#edc-communication), and — to test the connection — a consumer EDC carrying the sharing member's identity.

### The operator EDC

The EDC needs technical users with the **member company's** identity, not the operator's, because the sharing member's Gate is branded to that member's BPNL.
The Portal only issues those through app subscriptions: register the BPDM apps in the marketplace ([INSTALL.md — Create BPDM marketplace apps](../../INSTALL.md#create-bpdm-marketplace-apps)), have the member company subscribe, and once the subscription is activated the Portal lists the created clients:

1. BPDM Pool Consumer Client
2. BPDM Sharing Input Manager Client
3. BPDM Sharing Output Consumer Client

The EDC also needs access to its company's wallet. As a `Cx-Operator` account, create an `external` technical user with the `Identity Wallet Management` permission; the credentials take a few moments to appear.

Finally the EDC needs an RSA key pair for token signing, which is free to create.
All of it goes into the vault:

```
<env>/edc-bpdm/api#management-key                     (free to choose)
<env>/edc-bpdm/api#proxy-key                          (free to choose)
<env>/edc-bpdm/postgres#password                      (free to choose)
<env>/edc-bpdm/vault#token                            (the vault token)
<env>/edc-bpdm/token-private-key#content              (private RSA key)
<env>/edc-bpdm/token-public-key#content               (public RSA key)
<env>/edc-bpdm/client-secret#content                  (from the wallet client)
<env>/edc-bpdm/asset-secrets/<member>/pool-member-read#content     (from the Pool Consumer Client)
<env>/edc-bpdm/asset-secrets/<member>/input-full-access#content    (from the Sharing Input Manager Client)
<env>/edc-bpdm/asset-secrets/<member>/output-read-access#content   (from the Sharing Output Consumer Client)
```

Deploy from [`<env>/edc-bpdm/spec.yaml`](https://github.com/eclipse-tractusx/bpdm/tree/environments); the values may need the correct wallet client id.

### Creating the assets

With the EDC running, create the assets the sharing member consumes.
[INSTALL.md — Creating offers](../../INSTALL.md#creating-offers) describes them, and the [EDC Provider Setup Postman collection](../postman) automates the calls.
The collection variables for an environment are checked in next to that environment's EDC configuration, for example [`int/edc-bpdm/`](https://github.com/eclipse-tractusx/bpdm/tree/environments/int/edc-bpdm).
The `CLIENT_SECRET_PATH_…` variables hold vault paths, not secrets — the EDC resolves them itself.

### The consumer EDC

The sharing member's own EDC is the same deployment without assets, which removes the whole asset client and marketplace subscription part:

1. Create a wallet client in the sharing member's company.
2. Create the same vault secrets minus the `asset-secrets` entries, under `<env>/edc-<member>/`.
3. Deploy from that app's `spec.yaml`, adapting the BPNL and wallet client id in the values.

To exercise the connection, use the [EDC BPDM Consumer Postman collection](../postman) pointed at the consumer's management API and the provider's dataspace API.

## Feature Branch Deployments

A feature that needs validation on a real environment before it is merged can get its own INT deployment.
It is a normal app whose `spec.yaml` takes the chart from the feature branch of the BPDM repository rather than from a released chart version, on its own hostname — a `bpdm-hq-relocation` app built from `feat/headquarter-relocation` is the shape of it.

Pin the images to that branch as well, through each component's `image.tag`.
The branch's Docker build publishes under the branch name with `/` replaced by `-`, so `feat/headquarter-relocation` produces `feat-headquarter-relocation`.
Chart from the feature branch with images left at `latest-SNAPSHOT` validates nothing.

These are temporary. When the feature is merged, remove the directory and the ArgoCD app.

## Open Items

- **The live ArgoCD apps still load their values from personal accounts.** The checked-in specs all resolve against this repository, but each running app holds its own copy of the `helm_args` URL from when it was created. Update that URL in ArgoCD, per app.
- **The test sharing member EDC changes identity on its next synchronization.** `int/edc-test-sharing-member/spec.yaml` now loads its own values file instead of the second member's. The running deployment keeps the wrong participant identity until it is synchronized — do that deliberately, then re-run the [EDC BPDM Consumer collection](../postman) against it.
- **TruffleHog and KICS do not scan the `environments` branch.** The change adding the triggers is ready on `chore/scan-environments-branch`, held back until 7.5.0 is released. Merge it then.

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
