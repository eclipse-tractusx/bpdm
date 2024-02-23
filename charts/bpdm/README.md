# BPDM Helm Chart

This Helm Chart deploys the BPDM services to a Kubernetes environment.

## Prerequisites

* [Kubernetes Cluster](https://kubernetes.io/)
* [Helm](https://helm.sh/docs/)

In an existing Kubernetes cluster the application can be deployed with the following command:

```bash
helm install release_name ./charts/bpdm --namespace your_namespace
```

This will install a new release of the BPDM in the given namespace.
On default values this release deploys the latest image tagged as `main` from the repository's GitHub Container Registry.
The application is run on default profile without authorization.
Additionally, the Helm deployment contains a PostgreSQL database and Opensearch instance which the BPDM Pool connects to.

On the default values deployment no further action is needed to make the BPDM deployment run.
However, per default, ingress as well as authentication for endpoints are disabled.

By giving your own values file you can configure the Helm deployment of the BPDM freely:

```bash
helm install release_name ./charts/bpdm --namespace your_namespace -f ./path/to/your/values.yaml
```

In the following sections you can have a look at the most important configuration options.

## Image Tag

Per default, the Helm deployment references a certain BPDM release version where the newest Helm release points to the newest version.
This is a stable tag pointing to a fixed release version of the BPDM.
For your deployment you might want to follow the latest application releases instead.

In your values file you can overwrite the default tag:

```yaml
image:
  tag: "latest"
```

## Helm Dependencies

On default, the Helm deployment also contains a PostgreSQL deployment.
You can configure these deployments in your value file as well.
For this, consider the documentation of the correspondent dependency [PostgreSQL](https://artifacthub.io/packages/helm/bitnami/postgresql/11.9.13).
In case you want to use an already deployed database instance you can also disable the respective dependency and overwrite the default host
address in the `applicationConfig`:

```yaml
applicationConfig:
  spring:
    datasource:
      url: jdbc:postgresql://remote.host.net:5432/bpdm
postgres:
  enabled: false
```

## Notice

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm