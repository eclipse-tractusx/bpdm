# BPDM Cleaning Service Dummy Helm Chart

This Helm Chart deploys the BPDM service to a Kubernetes environment.

## Prerequisites

* [Kubernetes Cluster](https://kubernetes.io)
* [Helm](https://helm.sh/docs/)

In an existing Kubernetes cluster the application can be deployed with the following command:

```bash
helm install release_name ./charts/bpdm-cleanning-service-dummy --namespace your_namespace -f /path/to/my_release-values.yaml
```

This will install a new release of the BPDM Cleaning Service in the given namespace.
On default values this release deploys the latest image tagged as `main` from the repository's GitHub Container Registry.

By giving your own values file you can configure the Helm deployment of the BPDM Cleaning Service freely.
In the following sections you can have a look at the most important configuration options.

## Image Tag

Per default, the Helm deployment references the latest BPDM Cleaning Service release tagged as `main`.
This tag follows the latest version of the Cleaning Service and contains the newest features and bug fixes.
You might want to switch to a more stable release tag instead for your deployment.
In your values file you can overwrite the default tag:

```yaml
image:
  tag: "latest"
```

## Profiles

You can also activate Spring profiles in which the BPDM Cleaning Service should be run.
In case you want to run the Cleaning Service with authorization enabled you can write the following:

```yaml
springProfiles:
  - auth
```

## Ingress

You can specify your own ingress configuration for the Helm deployment to make the BPDM Cleaning Service available over Ingress.
Note that you need to have the appropriate Ingress controller installed in your cluster first.
For example, consider a Kubernetes cluster with an [Ingress-Nginx](https://kubernetes.github.io/ingress-nginx/) installed.
An Ingress configuration for the Cleaning Service deployment could somehow look like this:

```yaml
ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
  hosts:
    - host: business-partners.your-domain.net
      paths:
        - path: /cleaning-service
          pathType: Prefix
```

## Cleaning Service Configuration

For the default deployment you already need to overwrite the configuration properties of the application.
The Helm deployment comes with the ability to configure the BPDM Cleaning Service application directly over the values file.
This way you are able to overwrite any configuration property of the `application.properties` and `application-auth.properties` files.
Consider that you would need to turn on `auth` profile first before overwriting any property in the corresponding properties file could take
effect.
<!--Overwriting configuration properties can be useful for connecting to a remotely hosted BPDM Gate and Pool instance:

```yaml
applicationConfig:
  bpdm:
    orchestrator:
      base-url: http://remote.domain.net/api/catena
```
-->

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

