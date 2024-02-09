# BPDM Gate Helm Chart

This Helm Chart deploys the BPDM Gate service to a Kubernetes environment.

## Prerequisites

* [Kubernetes Cluster](https://kubernetes.io)
* [Helm](https://helm.sh/docs/)
* Running BPDM Pool instance

In an existing Kubernetes cluster the application can be deployed with the following command:

```bash
helm install release_name ./charts/bpdm-gate --namespace your_namespace -f /path/to/my_release-values.yaml
```

This will install a new release of the BPDM Gate in the given namespace.
On default values this release deploys the latest image tagged as `main` from the repository's GitHub Container Registry.
The application is run on default profile (without authorization for its own endpoints or BPDM Pool endpoints).
This deployment requires a BPDM Pool deployment to be reachable under host name `bpdm-pool` on port `8080`.

By giving your own values file you can configure the Helm deployment of the BPDM Gate freely.
In the following sections you can have a look at the most important configuration options.

## Image Tag

Per default, the Helm deployment references the latest BPDM gate release tagged as `main`.
This tag follows the latest version of the Gate and contains the newest features and bug fixes.
You might want to switch to a more stable release tag instead for your deployment.
In your values file you can overwrite the default tag:

```yaml
image:
  tag: "latest"
```

## Profiles

You can also activate Spring profiles in which the BPDM Gate should be run.
In case you want to run the Gate with authorization and oAuth Pool client enabled you can write the following:

```yaml
springProfiles:
  - auth
  - pool-auth
```

## Ingress

You can specify your own ingress configuration for the Helm deployment to make the BPDM Gate available over Ingress.
Note that you need to have the appropriate Ingress controller installed in your cluster first.
For example, consider a Kubernetes cluster with an [Ingress-Nginx](https://kubernetes.github.io/ingress-nginx/) installed.
An Ingress configuration for the Gate deployment could look like this:

```yaml
ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
  hosts:
    - host: business-partners.your-domain.net
      paths:
        - path: /companies/test-company
          pathType: Prefix
```

## Gate Configuration

For the default deployment you already need to overwrite the configuration properties of the application.
The Helm deployment comes with the ability to configure the BPDM Gate application directly over the values file.
This way you are able to overwrite any configuration property of the `application.properties`,  `application-auth.properties`
and  `application-pool-auth.properties` files.
Consider that you would need to turn on `auth` and `pool-auth` profile first before overwriting any property in the corresponding properties file could take
effect.
Overwriting configuration properties can be useful for connecting to a remotely hosted BPDM Pool instance:

```yaml
applicationConfig:
  bpdm:
    pool:
      base-url: http://remote.domain.net/api/catena
```

Entries in the "applicationConfig" value are written directly to a configMap that is part of the Helm deployment.
This can be a problem if you want to overwrite configuration properties with secrets.
Therefore, you can specify secret configuration values in a different Helm value `applicationSecrets`.
Content of this value is written in a Kubernetes secret instead.
If you want to specify a keycloak client secret for example:

```yaml
applicationSecrets:
  bpdm:
    security:
      credentials:
        secret: your_client_secret
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
