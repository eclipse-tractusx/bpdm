````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /input/legal-entities
    Note over Client,Controller: Method: GET

    Client->>Controller: getLegalEntities(paginationRequest)
    Controller->>LegalEntityService: getLegalEntities(paginationRequest)

    LegalEntityService->>LegalEntityRepository: findByDataType (OutputInputEnum.Input, PageRequest.of(page, size))

    LegalEntityRepository-->>LegalEntityService: Returns Page<LegalEntity>

    LegalEntityService->>LegalEntityService: toValidLegalEntities(Page<LegalEntity>)

    LegalEntityService-->>Controller: Returns PageDto<LegalEntityGateInputDto>

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<LegalEntityGateInputDto>

````

### 1. Client Request

The client sends a request to retrieve a legal entity's using pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `LegalEntityService` for processing.

### 3. Database Request

A request to the Database is done in the `LegalEntityService`. It searches for legal entities with input type, and inserted pagination (page, size).

### 4. Query response

It is returned a Page of Legal entity from the query.

### 5. LegalEntity Mapping

The retrieved Page of legal entity is now mapped to a list of LegalEntityGateInputDto

### 6. Response Preparation

An `PageDTO<LegalEntityGateInputDto>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<LegalEntityGateInputDto>`

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of the legal entity's.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
