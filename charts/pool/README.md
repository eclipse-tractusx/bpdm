# BPDM Pool Helm Chart

This Helm Chart deploys the BPDM Pool service to a Kubernetes environment.

## Prerequisites

* [Kubernetes Cluster](https://kubernetes.io/)
* [Helm](https://helm.sh/docs/)

In an existing Kubernetes cluster the application can be deployed with the following command:

```bash
helm install release_name ./charts/pool --namespace your_namespace
```

This will install a new release of the BPDM Pool in the given namespace.
On default values this release deploys the latest image tagged as `main` from the repository's GitHub Container Registry.
The application is run on default profile (without authorization and CDQ connection).
Additionally, the Helm deployment contains a PostgreSQL database and Opensearch instance which the BPDM Pool connects to.

On the default values deployment no further action is needed to make the BPDM Pool deployment run.
However, per default ingress is disabled, as well as no authentication for endpoints and no import from CDQ.

By giving your own values file you can configure the Helm deployment of the BPDM Pool freely:

```bash
helm install release_name ./charts/pool --namespace your_namespace -f ./path/to/your/values.yaml
```

In the following sections you can have a look at the most important configuration options.

## Image Tag

Per default, the Helm deployment references a certain BPDM Pool release version where the newest Helm release points to the newest Pool version.
This is a stable tag pointing to a fixed release version of the BPDM Pool.
For your deployment you might want to follow the latest application releases instead.

In your values file you can overwrite the default tag:

```yaml
image:
  tag: "latest"
```

## Profiles

You can also activate Spring profiles in which the BPDM Pool should be run.
In case you want to run the Pool with authorization and CDQ connection enabled you can write the following:

```yaml
springProfiles:
  - auth
  - cdq
```

## Ingress

You can specify your own ingress configuration for the Helm deployment to make the BPDM Pool available over Ingress.
Note that you need to have the appropriate Ingress controller installed in your cluster first.
For example, consider a Kubernetes cluster with an [Ingress-Nginx](https://kubernetes.github.io/ingress-nginx/) installed.
An Ingress configuration for the Pool deployment could look like this:

```yaml
ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
  hosts:
    - host: partners-pool.your-domain.net
      paths:
        - path: /
          pathType: Prefix
```

## Pool Configuration

The Helm deployment comes with the ability to configure the BPDM Pool application directly over the values file.
This way you are able to overwrite any configuration property of the `application.properties`,  `application-auth.properties` and  `application-cdq.properties`
files.
Consider that you would need to turn on `auth` and `cdq` profile first before overwriting any property in the corresponding properties file could take effect.
Overwriting configuration properties can be useful to connect to a remote service:

```yaml
applicationConfig:
  bpdm:
    security:
      auth-server-url: https://remote.keycloak.domain.com
      realm: CUSTOM_REALM
      client-id: POOL_CLIENT
```

In this example above a Pool with authenticated activated connects to a remote Keycloak instance and uses its custom realm and resource.

Entries in the "applicationConfig" value are written directly to a configMap that is part of the Helm deployment.
This can be a problem if you want to overwrite configuration properties with secrets.
Therefore, you can specify secret configuration values in a different Helm value `applicationSecrets`.
Content of this value is written in a Kubernetes secret instead.
If you want to specify a custom database password for example:

```yaml
applicationSecrets:
  spring:
    datasource:
      password: your_database_secret
```

## Helm Dependencies

On default, the Helm deployment also contains a PostgreSQL and Opensearch deployment.
You can configure these deployments in your value file as well.
For this, consider the documentation of the correspondent dependency [PostgreSQL](https://artifacthub.io/packages/helm/bitnami/postgresql/11.9.13)
or [Opensearch](https://opensearch.org/docs/latest/dashboards/install/helm/).
In case you want to use an already deployed database or Opensearch instance you can also disable the respective dependency and overwrite the default host
address in the `applicationConfig`:

```yaml
applicationConfig:
  spring:
    datasource:
      url: jdbc:postgresql://remote.host.net:5432/bpdm
postgres:
  enabled: false
```
