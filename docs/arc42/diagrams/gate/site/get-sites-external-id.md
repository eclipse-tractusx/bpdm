````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /input/sites/{externalId}
    Note over Client,Controller: Method: GET

    Client->>Controller: getSiteByExternalId(externalId)
    Controller->>SiteService: getSiteByExternalId(externalId)

    SiteService->>SiteRepository: findByExternalIdAndDataType(externalId, OutputInputEnum.Input)
    SiteRepository-->>SiteService: Returns Site

    SiteService->>SiteService: toSiteGateInputResponse (site)

    SiteService-->>Controller: Returns SiteGateInputDto

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: SiteGateInputDto

````

### 1. Client Request

The client sends a request to retrieve a site using an external id.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `SiteService` for processing.

### 3. Database Request

A request to the Database is done in the `SiteService`. It searches for a site with a specific external id and Input type.

### 4. Query response

It is returned a site from the query or if nothing is found an exception is thrown.

### 5. Sites Mapping

The retrieved site is now mapped to a SiteGateInputDto

### 6. Response Preparation

An `SiteGateInputDto` is prepared to be returned

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of a site.