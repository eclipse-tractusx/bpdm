````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /input/sites
    Note over Client,Controller: Method: GET

    Client->>Controller: getSites(paginationRequest)
    Controller->>SiteService: getSites(paginationRequest)

    SiteService->>SiteRepository: findByDataType (OutputInputEnum.Input, PageRequest.of(page, size))

    SiteRepository-->>SiteService: Returns Page<Site>

    SiteService->>SiteService: toValidSite( Page<Site>)

    SiteService-->>Controller: Returns PageDto<SiteGateInputDto>

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<SiteGateInputDto>

````

### 1. Client Request

The client sends a request to retrieve sites using pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `SiteService` for processing.

### 3. Database Request

A request to the Database is done in the `SiteService`. It searches for sites with input type, and inserted pagination (page, size).

### 4. Query response

It is returned a Page of sites from the query.

### 5. Sites Mapping

The retrieved Page of sites is now mapped to a list of SiteGateInputDto

### 6. Response Preparation

An `PageDTO<SiteGateInputDto>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<SiteGateInputDto>`

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of the sites.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
