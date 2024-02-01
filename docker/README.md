# BPDM Dockerfiles

This Docker directory contains all the BPDM modules dockerfiles.

### Container images

For demonstration purposes, BPDM provides container images. These images are built using the `eclipse-temurin:17-jre-alpine` base image.

Docker Hub:

* [eclipse-temurin](https://hub.docker.com/_/eclipse-temurin)
* [17-jre-alpine](https://hub.docker.com/layers/library/eclipse-temurin/17-jre-alpine/images/sha256-02c04793fa49ad5cd193c961403223755f9209a67894622e05438598b32f210e?context=explore)

Source:

* [temurin-build](https://github.com/adoptium/temurin-build)
* [temurin docker repo info](https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin)

### Contains the following dockerfiles of five modules:

- Bpdm-Gate
- Bpdm-Pool
- Bpdm-Bridge-dummy
- Bpdm-Orchestrator
- Bpdm-Cleaning-service-dummy

### How to use

Firstly open a terminal and make sure that the current path is on the tx-bpdm parent module, otherwise following commands will not work. The Pool module
dockerfile will be used as an example here.

After that execute the following command. This will build the Docker image.

```
docker build . -t bpdm-pool -f ./docker/pool/Dockerfile
```

After this, an image should be available to be used on docker. It can be used to create a docker container using the following command. Be aware that the
argument ***bpdm-pool-container***
can be altered for a different name.

```
docker run -d -p 8080:8080 --name bpdm-pool-container bpdm-pool
```

After this, the application should be available

### Dockerfile Arguments

The Dockerfiles specified in this directory, all contain different types of arguments that can be customized. They are:

- **ARG USERNAME=bpdm**: Change the username for the non-root user.
- **ARG USERID=10001**: Change the user ID for the non-root user.
- **ARG GID=3000**: Change the group ID for the non-root user.

## Notice for Docker image

* [BPDM Pool](./pool/DOCKER_NOTICE.md)
* [BPDM Gate](./gate/DOCKER_NOTICE.md)
* [BPDM Orchestrator](./orchestrator/DOCKER_NOTICE.md)
* [BPDM Cleaning Service Dummy](./cleaning-service-dummy/DOCKER_NOTICE.md)
* [BPDM Bridge Dummy](./bridge-dummy/DOCKER_NOTICE.md)
