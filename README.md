# BPDM Association Environment Configuration

This branch holds the deployment configuration of the BPDM applications on the Catena-X association
environments. It is **not** part of the BPDM product: nothing here is built, released or shipped.

It is an orphan branch, with no shared history with `main`, because this content has a different
lifecycle from the product. A deployment runs a *released* chart, while `main` carries the next one —
so these files describe chart versions that `main` has already moved past, on purpose. Keeping them
off `main` means a commit there stays a statement about the product alone, and no chart change can
sweep this configuration along with it.

The same reasoning is why `gh-pages` is a separate orphan branch for the released chart index.

## Layout

```
<environment>/<argocd-app>/
  spec.yaml     the ArgoCD application definition: chart, targetRevision, namespace,
                the vault plugin, and the raw URL this branch serves values.yaml from
  values.yaml   the Helm values for that deployment
```

`int/` is the environment the maintainer keeps current; `stable/` is updated on demand.

## Documentation

The guides live on `main` and are the entry point — what runs where, how ArgoCD and the vault plugin
are wired, how to set up a golden record deployment, a sharing member Gate or an EDC:

- [Association Environments](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/maintainer/environments.md)
- [Release Process](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/maintainer/release-process.md)
- [End-to-End Testing](https://github.com/eclipse-tractusx/bpdm/blob/main/docs/maintainer/e2e-testing.md)

## Working on this branch

- **Secrets never appear here.** Every credential is a vault reference resolved at sync time by the
  argocd-vault-plugin: `<path:bpdm/data/<environment>/path/to/secret#value>`. A literal value in
  place of a reference is a leaked credential in a public repository.
- **Change a `values.yaml` only when its deployment is being upgraded**, not when the chart changes
  on `main`. Which keys are correct depends on the chart version pinned in that deployment's
  `spec.yaml`, not on what `main` holds.
- **ArgoCD reads `values.yaml` from this branch at sync time**, so a merge here is what the next
  synchronization applies. No app declares a `syncPolicy`, so nothing deploys on its own.
- Editing a `spec.yaml` does not reconfigure a running app: each live ArgoCD app holds its own copy
  of these settings from when it was created. Change both.

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
