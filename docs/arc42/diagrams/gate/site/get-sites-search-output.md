````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /output/sites
    Note over Client,Controller: Method: POST

    Client->>Controller: getSitesOutput(paginationRequest)
    Controller->>SiteService: getSitesOutput(paginationRequest)

    SiteService->>SiteRepository: findByDataType (OutputInputEnum.Output, PageRequest.of(page, size))

    SiteRepository-->>SiteService: Returns Page<Site>

    SiteService->>SiteService: toValidOutputSites( Page<Site>)

    SiteService-->>Controller: Returns PageDto<SiteGateOutputResponse>

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<SiteGateOutputResponse>
    
````

### 1. Client Request

The client sends a request to retrieve a multiple output sites using one or multiple external id's with pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `SiteService` for processing.

### 3. Database Request

A request to the Database is done in the `SiteService`. It searches for sites with output type, and inserted pagination (page, size).

### 4. Query response

It is returned a Page of sites from the query.

### 5. Site Mapping

The retrieved Page of sites is now mapped to a list of SiteGateOutputResponse

### 6. Response Preparation

An `PageDTO<SiteGateOutputResponse>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<SiteGateOutputResponse>`

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of a site.