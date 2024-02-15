````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /output/legal-entities/search
    Note over Client,Controller: Method: POST

    Client->>Controller: getLegalEntitiesOutput(paginationRequest, externalIds)
    Controller->>LegalEntityService: getLegalEntitiesOutput(page, size, externalIds)

    Note over LegalEntityService, LegalEntityRepository: If externalIds is not null
    LegalEntityService->>LegalEntityRepository: findByExternalIdInAndDataType (externalIds, <br/> OutputInputEnum.Output, PageRequest.of(page, size))
    Note over LegalEntityService, LegalEntityRepository: If externalIds is null
    LegalEntityService->>LegalEntityRepository: findByDataType (OutputInputEnum.Output, PageRequest.of(page, size))

    LegalEntityRepository-->>LegalEntityService: Returns Page<LegalEntity>

    LegalEntityService->>LegalEntityService: toValidOutputLegalEntities(Page<LegalEntity>)

    LegalEntityService-->>Controller: Returns PageDto<LegalEntityGateOutputResponse>

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<LegalEntityGateOutputResponse>


````

### 1. Client Request

The client sends a request to retrieve a multiple output legal entity's using one or multiple external id's with pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `LegalEntityService` for processing.

### 3. Database Request

A request to the Database is done in the `LegalEntityService`. It searches for legal entities with output type, and inserted pagination (page, size).

### 4. Query response

It is returned a Page of Legal entity from the query.

### 5. LegalEntity Mapping

The retrieved Page of legal entity is now mapped to a list of LegalEntityGateOutputResponse

### 6. Response Preparation

An `PageDTO<LegalEntityGateOutputResponse>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<LegalEntityGateOutputResponse>`

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of a legal entity.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
