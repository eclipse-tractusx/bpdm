````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /input/sites/search
    Note over Client,Controller: Method: POST

    Client->>Controller: getSitesByExternalIds (paginationRequest, externalIds)
    Controller->>SiteService: getSites(page, size, externalIds)

    Note over SiteService, SiteRepository: If externalIds is not null
    SiteService->>SiteRepository: findByExternalIdInAndDataType (externalIds, <br/> OutputInputEnum.Input, PageRequest.of(page, size))
    Note over SiteService, SiteRepository: If externalIds is null
    SiteService->>SiteRepository: findByDataType (OutputInputEnum.Input, PageRequest.of(page, size))

    SiteRepository-->>SiteService: Returns Page<Site>

    SiteService->>SiteService: toValidSite( Page<Site>)

    SiteService-->>Controller: Returns PageDto<SiteGateInputDto>

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<SiteGateInputDto>

````

### 1. Client Request

The client sends a request to retrieve a multiple sites using one or multiple external id's with pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `SiteService` for processing.

### 3 and 4. Database Request

A request to the Database is done in the `SiteService`. It searches for sites with inserted external id's, input type, and inserted pagination (page, size).

If the external id's collection is null, the `findByDataType` is used which does not take into account the external id's. Otherwise,
the `findByExternalIdInAndDataType` is used.

### 5. Query response

It is returned a Page of Sites from the query.

### 6. Sites Mapping

The retrieved Page of sites is now mapped to a list of SiteGateInputDto

### 7. Response Preparation

An `PageDTO<SiteGateInputDto>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<SiteGateInputDto>`

### 8. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of a sites.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
