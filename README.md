# GPDM

## Description

This repository is part of the overarching CatenaX project.

GPDM is a german abbreviation for Geschäfts-Partner-Daten-Management (business partner data management). 
This project lets other CatenaX services query, add and change information on CatenaX business partners.

## How to run

GPDM is a SpringBoot Kotlin software project managed by Maven. 

The project can be run with the following command:`mvn clean spring-boot:run`

On default configuration the project initializes and starts an in-memory H2 database. 
After the project has finished initializing you can access the database on `http://localhost:8080/h2-console/` 
as per default configuration. You can find the standard user and password as well as further database configurations 
int the `application.properties` file in the resource folder.

#### Prerequisites

1. Maven
2. JDK11
3. PostgreSQL 13.2 (on persist profile)
4. Keycloak 17.0.0 (on auth profile)
5. Connection to CDQ API v4.0  (on cdq profile)
6. Elasticsearch 7.17.0 (on elastic profile)

### Profiles

The project offers a variety of different Spring profiles for configuration purposes.
These profiles enable and configure additional project components which rely on external services and systems.
Profiles are categorized by component and the stage they belong to. 
Currently, we distinguish between the local and dev stage. 
The local profile expects all enabled components to be locally available on the host machine. 
On the other hand, the dev stage configures external dependencies for the remote dev environment.
In order to run the application with a specific profile you can use the appropriate maven flag `Dspring.profiles.active`.
Single profiles are named via pattern `stage_component` but can also be called as a bundle by their stage name.
For example you can run the application in full local profile by this command:

`mvn clean spring-boot:run -Dspring.profiles.active=local`

Or you can specify a single component profile to be activated like such:

`mvn clean spring-boot:run -Dspring.profiles.active=local_persist`

The above command only enables persistence in local configuration without enabling any other local components such as Elasticsearch.


#### Persist

A persist profile enables and configures the usage of an external PostgreSQL database, instead of the in-memory H2 database. 
Therefore, a running Postgres database instance is required (Tested and working on version 13.2). User and password need to be provided as  environment variables 
`BPDM_DB_USER` and `BPDM_DB_PASS`. The connection URL can be looked at and adapted in respective application properties files.

#### Auth

An auth profile enables authorization of endpoints and configures the connection to an external Keycloak instance on which the authorization relies on.
Besides the URL of the Keycloak the configuration requires the client credentials to validate incoming tokens (Standard behaviour of this application is bearer-only).
The client secret has to be submitted via environment variable `BPDM_KEYCLOAK_SECRET`. 

This profile also enables the login form in the auto-generated Swagger documentation. The Swagger login uses the client
specified in the property `springdoc.swagger-ui.oauth.client-id`.

#### Cdq

A cdq profile configures the connection to a remote [CDQ API](https://www.apimatic.io/apidocs/data-exchange/v/4_0#/rest/getting-started) with which the application can exchange business partner information.
On activation the profile enables new endpoints to import records from and to export Business Partner Numbers to CDQ. 
Among others, the profile determines the storage and datasource to use.
For this the profile requires the environment variable `BPDM_CDQ_KEY` to contain an API key with necessary privileges.

#### Elastic

An elastic profile enables and configures the connection to an external Elasticsearch instance. Therefore, a running 
Elasticsearch instance is required (Tested on version 7.17.0). 
You can specify the URL to connect to and if need be add user and password over the spring elasticsearch adapter properties.
When this profile is enabled the application is able to search and filter business partners by their properties other than identifiers.
Additionally, suggestions for autocompletion can be obtained for each business partner property.
With the activation of the Elasticsearch component the application also features new endpoints for exporting
business partner records to the Elasticsearch instance as well as clearing the current Elasticsearch index.

Without the Elasticsearch component enabled the suggestions are always empty and search requests do not filter any business partners.

## Project Structure

The root of the project is reserved for basic repository files and the Maven project file (pom.xml).
The source folder is split between test and application files. Source code files are in the kotlin subdirectory (analogous to java source folders). 
Additional files such as configuration files can be found in the resources subdirectory.

As per Spring framework's default structure the domain model and persistence object information is encapsulated in entities. 
Each entity in the project derives from the `BaseEntity` type which contains standard fields/columns such as identifier 
and timestamps.

Services describe the business logic of the application. They primarily work on entities but may also map such entities to
data transfer objects (DTOs) which are needed for communication with outside systems. Most important DTOs are request and response
objects which describe the model of the application's API.

Repositories describe the interface with the persistence layer and should be used by the services to gather and save records
from the database. Where possible repositories should be defined as interfaces and auto-implemented by Spring Data JPA. 
In cases when that is not feasible custom repositories can be defined.

Configuration classes configure the services and components in the application. Such configuration classes enable or disable
component logic on startup. They are supplemented by the configuration properties.
These property classes contain values obtained from the application.properties files and are available via dependency injection.
When possible, configuration classes services and components should use configuration properties instead of accessing property values
from the application.properties directly. However, in some cases such as conditional activation on configurations by annotation
such an approach is not possible and direct access is permissible.

Optional components which require more logic than just simple configuration files are placed in the `component` package
such as the cdq and elastic component subpackages. Such a component package is structured again like a mirror of the project structure.
That is, a component package can contain its own repository, service, configuration packages and so on. By default, the application
component scan ignores the component packages. By enabling the corresponding properties component packages can be included
in the component scan.

## Kubernetes Deployment

This repository contains Docker and Helm files for deploying the application to a Kubernetes environment.
In order to deploy the application to a Kubernetes Cluster you need to containerize the application, push the resulting
image to a container registry and deploy a Helm release on the prepared cluster.

#### Prerequisites
1. [Kubernetes Cluster](https://kubernetes.io/)
2. [Docker](https://docs.docker.com/)
3. [Helm](https://helm.sh/docs/)
4. A Container Registry (Currently [ACR](https://docs.microsoft.com/en-us/azure/container-registry/))
5. Kubernetes Ingress Controller (Tested with [Ingress-Nginx](https://kubernetes.github.io/ingress-nginx/))
6. [Kubernetes Certmanager](https://cert-manager.io/docs/)
7. [Kubernetes Cluster Issuer](https://cert-manager.io/docs/concepts/issuer/)

The kubernetes deployment expects a kubernetes environment which already has an Ingress Controller installed in order to
be available over ingress routing. Additionally, the ingress works over SSH and expects a Certmanager and Cluster Issuer to be present for
obtaining a trustworthy certificate. When the Kubernetes cluster is configured with these components, the application can be 
deployed with the following steps:

1. Specify your container registry in the Helm values.yaml: 
```yaml
image:
   registry: your_registry.io
``` 
2. Package the application as a jar file: `mvn clean package`
3. Containerize the packaged application: `docker build -f kubernetes/Dockerfile -t your_container_registry.io/catena-x/bpdm:version .`
4. Push the image to your registry: `docker push your_container_registry.io/catena-x/bpdm:version`
5. Install the Helm release on the cluster: `helm install release_name ./kubernetes/bpdm -n your_namespace`

When the deployment needs to be updated you can follow the same steps above, except for the last. In order to
update the Helm release you need the Helm upgrade command: `helm upgrade release_name ./kubernetes/bpdm -n your_namespace`

### Deployment with Profiles

The instructions above deploys an application with the default Spring profile enabled.
You can set the active profiles in the `springProfiles` value. Like so:

```yaml
springProfiles:
  - dev
``` 

Be aware that additional profiles usually require secrets to be passed to the application. The helm deployment automatically
creates Kubernetes applicaton secrets which are being used by the deployed application based on the `applicationSecrets` values.
You can determine which secrets should be created by specifying the name of the entry in the Kubernetes secret with it's
corresponding environment variable name (defined in the Spring profiles) and the actual value of the secret like so:

```yaml
applicationSecrets:
    db-user:
        envName: BPDM_DB_USER
        secret: some_value
``` 

In order to avoid pushing secrets to the Github repository it's a good practice to leave the secret value empty and pass it
over command line when deploying a helm release via the set flag like `--set applicationSecrets.db-user.secret=some_secret`.

### Pull Secrets
 
Private container registries may require authentication in order to be accessed. In this case the Helm deployment needs to
be given pull secrets to pull the image from such a registry. Pull secrets are specified in the values.yaml like so:

```yaml
imagePullSecrets:
    mail: your_email@your_org.com
    user: your_user
    password: your_pass
``` 

As with application secrets instead of writing your credentials directly into a value.yaml you better pass them via
command line when deploying the helm release: `--set imagePullSecret.user=your_user`


### Dev Deployment

In order to deploy the application
with the dev environment profile you can use the provided dev-values.yaml which starts the application with the Spring dev profile.
Taking in all the previous points, for a full dev deployment in it's own dev namespace you would need to use the following command:

```bash
helm install release_name ./kubernetes/bpdm -f ./kubernetes/dev-values.yaml \
--namespace your_namespace \
--set imagePullSecrets.user=$BPDM_PULL_USER \
--set imagePullSecrets.password=$BPDM_PULL_PASS \
--set applicationSecrets.db-user.secret=$BPDM_DB_USER \
--set applicationSecrets.db-pass.secret=$BPDM_DB_PASS \
--set applicationSecrets.keycloak-secret.secret=$BPDM_KEYCLOAK_SECRET \
--set applicationSecrets.cdq-key.secret=$BPDM_CDQ_KEY
``` 
Where the environment variables hold the necessary secret values.

For an update, in case no helm values need to changed you can reuse the old values:

```bash
helm upgrade release_name ./kubernetes/bpdm -n your_namespace --reuse-values
``` 

Otherwise, you need to provide all values again:

```bash
helm upgrade release_name ./kubernetes/bpdm -f ./kubernetes/dev-values.yaml \
--namespace your_namespace \
--set imagePullSecrets.user=$BPDM_PULL_USER \
--set imagePullSecrets.password=$BPDM_PULL_PASS \
--set applicationSecrets.db-user.secret=$BPDM_DB_USER \
--set applicationSecrets.db-pass.secret=$BPDM_DB_PASS \
--set applicationSecrets.keycloak-secret.secret=$BPDM_KEYCLOAK_SECRET \
--set applicationSecrets.cdq-key.secret=$BPDM_CDQ_KEY
``` 