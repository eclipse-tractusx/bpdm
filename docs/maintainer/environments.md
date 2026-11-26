# Association Environments

BPDM maintains deployments on the Catena-X association environments.
They serve the association's Portal, give sharing members something to integrate against, and are the target of the [end-to-end tests](e2e-testing.md) that validate every release.

Keeping them running, upgraded and configured is a maintainer duty.
This guide describes what runs where and how it is deployed; the deployment configuration itself lives on the [`environments` branch](https://github.com/eclipse-tractusx/bpdm/tree/environments).

<!-- TOC -->
* [Association Environments](#association-environments)
  * [What Runs Where](#what-runs-where)
    * [INT — argocd.test.tractus-x.com](#int--argocdtesttractus-xcom)
    * [STABLE — argocd.stable.catena-x.net](#stable--argocdstablecatena-xnet)
  * [How a Deployment Is Defined](#how-a-deployment-is-defined)
    * [Why the configuration lives on its own branch](#why-the-configuration-lives-on-its-own-branch)
  * [Secret Management](#secret-management)
  * [Setting Up the Golden Record Process](#setting-up-the-golden-record-process)
    * [1. Gather the client credentials](#1-gather-the-client-credentials)
    * [2. Create the vault secrets](#2-create-the-vault-secrets)
    * [3. Deploy](#3-deploy)
  * [Operator API Access](#operator-api-access)
  * [Setting Up a Sharing Member Gate](#setting-up-a-sharing-member-gate)
  * [Setting Up the EDC](#setting-up-the-edc)
    * [The provider EDC](#the-provider-edc)
    * [Creating the assets](#creating-the-assets)
    * [The consumer EDC](#the-consumer-edc)
  * [Feature Branch Deployments](#feature-branch-deployments)
  * [NOTICE](#notice)
<!-- TOC -->

## What Runs Where

The association offers two environments, both operated through ArgoCD:

- **INT** — the environment the maintainer keeps current. It always follows the newest release version, and it is where release candidates are validated by the [end-to-end tests](e2e-testing.md). It runs on the Tractus-X test environment (`*.test.tractus-x.com`).
- **STABLE** — updated on demand, when an update is announced. It is expected to run an older line than INT.

### INT — [argocd.test.tractus-x.com](https://argocd.test.tractus-x.com)

The **released golden record core** — Pool, Orchestrator, the Portal's Gate and the Cleaning Service Dummy at https://business-partners.test.tractus-x.com/ — is hosted centrally by the test environment operators, not by BPDM.
It has no ArgoCD app or configuration of ours; moving it to a new chart version is a request to them (see [release process step 4](release-process.md#4-deploy-the-release-candidate-to-int)).

The maintainer deploys everything around it, in the `bpdm` project and namespace:

| ArgoCD app                            | Purpose                                                   | Chart source         | Root URL                                                                              |
|---------------------------------------|-----------------------------------------------------------|----------------------|---------------------------------------------------------------------------------------|
| `bpdm-test-sharing-member`            | Gate of test sharing member 1, on the released core       | released chart       | https://business-partners.test.tractus-x.com/companies/test-sharing-member            |
| `bpdm-test-sharing-member-2`          | Gate of test sharing member 2, on the released core       | released chart       | https://business-partners.test.tractus-x.com/companies/test-sharing-member-2          |
| `bpdm-test-sharing-member-3`          | Gate of test sharing member 3, on the released core       | released chart       | https://business-partners.test.tractus-x.com/companies/test-sharing-member-3          |
| `bpdm-snapshot`                       | Snapshot core (Pool, Orchestrator, Cleaning Service Dummy) | `main` branch        | https://business-partners-snapshot.test.tractus-x.com/                                |
| `bpdm-snapshot-test-sharing-member`   | Gate of test sharing member 1, on the snapshot core       | `main` branch        | https://business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member   |
| `bpdm-snapshot-test-sharing-member-2` | Gate of test sharing member 2, on the snapshot core       | `main` branch        | https://business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member-2 |
| `bpdm-snapshot-test-sharing-member-3` | Gate of test sharing member 3, on the snapshot core       | `main` branch        | https://business-partners-snapshot.test.tractus-x.com/companies/test-sharing-member-3 |
| `edc-bpdm`                            | Provider EDC offering the released member Gates           | `tractusx-connector` | https://bpdm-edc.test.tractus-x.com                                                   |
| `edc-bpdm-snapshot`                   | Provider EDC offering the snapshot member Gates           | `tractusx-connector` | https://bpdm-snapshot-edc.test.tractus-x.com                                          |
| `edc-test-sharing-member`             | Consumer EDC of test sharing member 1                     | `tractusx-connector` | https://bpdm-sharing-member-edc.test.tractus-x.com                                    |
| `edc-test-sharing-member-2`           | Consumer EDC of test sharing member 2                     | `tractusx-connector` | https://bpdm-sharing-member-2-edc.test.tractus-x.com                                  |
| `edc-test-sharing-member-3`           | Consumer EDC of test sharing member 3                     | `tractusx-connector` | https://bpdm-sharing-member-3-edc.test.tractus-x.com                                  |

Each test sharing member is a company of its own, with its own BPNL; the member EDCs serve both the released and the snapshot Gates.
The snapshot core has no Gate — the Portal's Gate runs only on the centrally hosted core.
The [INT walkthrough](https://github.com/eclipse-tractusx/bpdm/blob/environments/int/README.md) on the `environments` branch sets the whole landscape up from scratch: Portal companies, the golden record process app and its subscriptions, the vault, ArgoCD and the EDC offers.

### STABLE — [argocd.stable.catena-x.net](https://argocd.stable.catena-x.net)

| ArgoCD app                 | Purpose                                              | Root URL                                                                        |
|----------------------------|------------------------------------------------------|---------------------------------------------------------------------------------|
| `bpdm`                     | Golden record core                                   | https://business-partners.stable.catena-x.net/                                  |
| `bpdm-test-sharing-member` | Gate for the test sharing member                     | https://business-partners.stable.catena-x.net/companies/test-sharing-member     |
| `edc-bpdm`                 | Operator EDC providing the BPDM assets               | https://bpdm-edc.stable.catena-x.net                                            |
| `edc-test-sharing-member`  | Consumer EDC with the test sharing member's identity | https://bpdm-sharing-member-edc.stable.catena-x.net                             |

Read deployed chart versions from ArgoCD or from each app's `targetRevision`; they are not listed here because they change every release.

INT holds the accumulated data of every end-to-end run executed against it, [by design](e2e-testing.md#test-data-is-not-cleaned-up), so upgrading it exercises a real migration against a populated database.

## How a Deployment Is Defined

Each ArgoCD app is a folder on the [`environments` branch](https://github.com/eclipse-tractusx/bpdm/tree/environments), `<environment>/<group>/<app>/` — or `<environment>/<app>/` for an app that stands alone, as on STABLE:

- **`spec.yaml`** — the ArgoCD application specification: which chart, from which repository, at which `targetRevision`, into which project and namespace. It also passes the values files to Helm as raw URLs and enables the vault plugin.
- **`values.yaml`** — the Helm values for that deployment: ingress hosts and context paths, the URLs the BPDM components use to reach each other, the Central-IDP configuration, and vault references for every secret.

On INT the apps are grouped (`int/sharing-members/`, `int/snapshot/`, `int/edc/`), and each group has a `values.yaml` of its own that every app of the group shares.
An app's `spec.yaml` passes the group file first and its own second, so the app file overrides it; lists such as ingress hosts live in the app files, because Helm replaces lists instead of merging them.
The branch's README maps every folder to its ArgoCD app.

Creating a new app in ArgoCD means creating it from the corresponding `spec.yaml`, under the app name listed above — the name is the Helm release name, which names the database volume, so a different name starts on an empty database.
Changing an existing deployment means editing `values.yaml` and re-synchronizing.
Since ArgoCD fetches the values files over their raw URLs at sync time, a change is live for the next synchronization as soon as it is merged.
A changed `spec.yaml` is not: each live ArgoCD app holds its own copy of the specification from when it was created, so a change there has to be applied in ArgoCD as well.

### Why the configuration lives on its own branch

ArgoCD combines a `values.yaml` read at sync time with whichever chart version that deployment's `spec.yaml` pins — two versions chosen independently.
A values file is therefore correct for *its deployment's pinned chart*, not for the newest one, which inverts the rule everywhere else on `main`.
Off `main`, no chart pull request can sweep it along and no `release/X.Y.x` branch freezes a stale copy. `gh-pages` is the same arrangement.

Four rules follow:

- **Change a `values.yaml` only when its deployment is being upgraded** — during the release cycle for INT, on announcement for STABLE. If a chart change alters a values key, record it in the [migration guide](../admin/MIGRATION_GUIDE.md) and apply it to the values when the chart version in `spec.yaml` moves.
- **Read the chart, not its changelog**, before deciding what a chart change requires here. Chart 7.0.0 added generated Secrets behind `externalApplicationConfig` but kept `applicationConfig` / `applicationSecrets` in the subcharts, where they still take precedence. Every `values.yaml` sets them deliberately — do not migrate them to the new structure.
- **Never add `syncPolicy.automated`.** No `spec.yaml` declares a sync policy, so a merge changes only what *would* be deployed and the maintainer decides when it is. Automating it turns every merge into a live change on an association environment.
- **Scan a `values.yaml` by hand before merging it.**

## Secret Management

No secret value is ever written into a `values.yaml`.
Every environment's ArgoCD runs a vault plugin that resolves references at sync time against that environment's HashiCorp Vault: [vault.test.tractus-x.com](https://vault.test.tractus-x.com) for INT, the association's central [vault.core.catena-x.net](https://vault.core.catena-x.net) for STABLE.
Access to the INT vault and ArgoCD is granted by the test environment operators; both log in through the Central-IDP with the Portal account.

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

The paths follow the deployment structure: `<environment>/<deployment>/<component>/<purpose>`, for example `int/bpdm-test-sharing-member/gate/pool-client#client-secret`.

Secrets whose value name is `content` are a different mechanism: those are read by the EDC directly from the vault at runtime, not resolved by the ArgoCD plugin.

## Setting Up the Golden Record Process

The core deployment the Portal depends on for onboarding companies and serving its partner network and company data pages.
[INSTALL.md](../../INSTALL.md) covers generic chart installation; what follows is only what an association environment adds.

On INT the test environment operators host this core, so the steps apply to STABLE.
The only core the maintainer deploys on INT is the snapshot core (`int/snapshot/core`), which has no Gate: it needs the database passwords and the Pool and Cleaning Service Dummy Orchestrator clients below, not the two Gate clients.

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

Unlike the core deployment, a sharing member Gate needs its own technical users, carrying the member company's identity:

| Technical user           | Permission                        |
|--------------------------|-----------------------------------|
| Gate → Pool access       | `BPDM Pool Sharing Consumer`      |
| Gate → Orchestrator access | `BPDM Orchestrator Task Creator` |

On INT they come from the member's subscription to the golden record process app, as two of its five technical user profiles — the [INT walkthrough](https://github.com/eclipse-tractusx/bpdm/blob/environments/int/README.md#3-register-the-golden-record-process-app) lists them.
The released and the snapshot Gate of a member share these users and the vault secrets below; only their databases differ, one per ArgoCD app.

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
That needs a provider EDC, run by the operator, offering the [BPDM assets](../architecture/08_Crosscutting_Concepts.md#edc-communication), and — to test the connection — a consumer EDC carrying the sharing member's identity.

### The provider EDC

The EDC needs technical users with the **member company's** identity, not the operator's, because the sharing member's Gate is branded to that member's BPNL.
The Portal only issues those through app subscriptions: register the golden record process app in the marketplace, have the member company subscribe, and once the subscription is activated the operator company lists the created technical users.
The provider EDC holds three of them per member:

1. Pool Consumer (`BPDM Pool Consumer`)
2. Sharing Input Manager (`BPDM Sharing Input Manager`)
3. Sharing Output Consumer (`BPDM Sharing Output Consumer`)

The [INT walkthrough](https://github.com/eclipse-tractusx/bpdm/blob/environments/int/README.md#3-register-the-golden-record-process-app) registers the app with all five technical user profiles a member needs, these three and the two of its [Gate](#setting-up-a-sharing-member-gate); [INSTALL.md — Create BPDM marketplace apps](../../INSTALL.md#create-bpdm-marketplace-apps) covers the generic case.

The EDC also needs an RSA key pair for token signing, which is free to create, and access to its company's wallet.
On INT every connector authenticates against the SSI DIM wallet stub, which takes the BPNL as client ID and does not check the secret, so no wallet technical user is needed.
On STABLE the wallet is the company's real one: as a `Cx-Operator` account, create an `external` technical user with the `Identity Wallet Management` permission, and store its secret instead of an arbitrary value.

All of it goes into the vault:

```
<env>/edc-bpdm/api#management-key                     (free to choose)
<env>/edc-bpdm/api#proxy-key                          (free to choose)
<env>/edc-bpdm/postgres#password                      (free to choose)
<env>/edc-bpdm/vault#token                            (the vault token)
<env>/edc-bpdm/token-private-key#content              (private RSA key)
<env>/edc-bpdm/token-public-key#content               (public RSA key)
<env>/edc-bpdm/wallet-client-secret#content           (INT: any value; STABLE: from the wallet client)
<env>/edc-bpdm/asset-secrets/<member>/pool-member-read#content     (from the Pool Consumer)
<env>/edc-bpdm/asset-secrets/<member>/input-full-access#content    (from the Sharing Input Manager)
<env>/edc-bpdm/asset-secrets/<member>/output-read-access#content   (from the Sharing Output Consumer)
```

The vault token is the one the connector reads its own folder with at runtime. A token from an OIDC login expires, and the connector stops reading its secrets when it does; ask the test environment operators for a long-lived one.

On INT, deploy `edc-bpdm` from `int/edc/operator/spec.yaml` and the snapshot's provider `edc-bpdm-snapshot` from `int/edc/operator-snapshot/spec.yaml`.
The snapshot provider layers its values on top of `edc-bpdm`'s and overrides only its host and connector context id, so it reads the same `int/edc-bpdm/` vault folder and needs no secrets of its own.

### Creating the assets

With the EDC running, create the assets the sharing member consumes.
[INSTALL.md — Creating offers](../../INSTALL.md#creating-offers) describes them, and the [EDC Provider Setup Postman collection](../admin/EDC%20Provider%20Setup.postman_collection.json) automates the calls.
The collection variables are checked in next to each member's EDC configuration, for example [`int/edc/member-1/`](https://github.com/eclipse-tractusx/bpdm/tree/environments/int/edc/member-1), one Postman environment for the released provider and one for the snapshot provider.
Each holds both sides: the provider variables to create that member's offers, and the consumer variables to negotiate them through the member's own EDC.
The `CLIENT_SECRET_PATH_…` variables hold vault paths, not secrets — the EDC resolves them itself.
The `CLIENT_ID_…` variables and API keys are left empty in the checked-in files; fill them locally and do not commit them.

### The consumer EDC

The sharing member's own EDC is the same deployment without assets, which removes the whole asset client and marketplace subscription part:

1. Create the same vault secrets minus the `asset-secrets` entries, under `<env>/edc-<member>/`. On INT the three member EDCs share one vault token, `int/edc-test-sharing-member/vault#token`.
2. Deploy from that app's `spec.yaml`, adapting the BPNL — `participant.id`, `participant.bpnl` and, on INT, the wallet client id `dcp.sts.oauth.client.id` — in the values. On STABLE create a wallet client in the sharing member's company first.

Each test sharing member on INT is a company with an EDC of its own, so every negotiation crosses from a member's connector to the provider.

To exercise the connection, use the [EDC BPDM Consumer Postman collection](../api/EDC%20BPDM%20Consumer.postman_collection.json) pointed at the consumer's management API and the provider's dataspace API.
The [system tester](../../bpdm-system-tester) exercises the same negotiation from code and covers every offer in one run:

```bash
SPRING_PROFILES_ACTIVE=int,int-edc BPDM_INT_EDC_API_KEY=... BPDM_INT_EDC_2_API_KEY=... BPDM_INT_EDC_3_API_KEY=... \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar --tags @EdcAccess
```

Those scenarios only read, so they can be repeated against an environment while its offers are being set up.
They cover the Gate offers; the Pool scenario reports as skipped, because the suite keeps the Pool on the direct route — see [testing over the EDC](e2e-testing.md#testing-over-the-edc) for why.
The three `api#management-key` secrets of the connectors are the only values to supply: `bpdm-system-tester/src/main/resources/application-int-edc.yml` names the connector and offer of each sharing member, with the addresses, the provider's DID and each member's BPNL defaulted to the INT deployment.
Layered on `snapshot` with the snapshot provider's dataspace API, the same scenarios check the snapshot offers.
Running the whole suite over the EDC instead of only these scenarios is described under [testing over the EDC](e2e-testing.md#testing-over-the-edc).

## Feature Branch Deployments

A feature that needs validation on a real environment before it is merged can get its own INT deployment.
It is a normal app in the `bpdm` project and namespace whose `spec.yaml` takes the chart from the feature branch of the BPDM repository rather than from a released chart version, on its own hostname — a `bpdm-hq-relocation` app built from `feat/headquarter-relocation` is the shape of it.

Pin the images to that branch as well, through each component's `image.tag`.
The branch's Docker build publishes under the branch name with `/` replaced by `-`, so `feat/headquarter-relocation` produces `feat-headquarter-relocation`.
Chart from the feature branch with images left at `latest-SNAPSHOT` validates nothing.

These are temporary. When the feature is merged, remove the directory and the ArgoCD app.

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
