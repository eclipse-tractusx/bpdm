````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/input/changelog/search
    Note over Client,Controller: Method: POST

    Client->>Controller: getInputChangelog(paginationRequest, searchRequest)
    Controller->>ChangelogService: getChangeLogEntries(searchRequest.externalIds, searchRequest.businessPartnerTypes, <br/>searchRequest.timestampAfter, OutputInputEnum.Input, <br/>paginationRequest.page, paginationRequest.size)

    ChangelogService->>Changelog Repository: findAll(spec, pageable)
    Changelog Repository-->>ChangelogService: Returns Page<ChangelogEntry>
    ChangelogService->>ChangelogService: Mapping of ChangelogEntry to DTO

    ChangelogService-->>Controller: Return Paginated Input Changelog entries

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageChangeLogDto<ChangelogGateDto>

````

### 1. Client Request

The client sends a request to retrieve an input changelog with parameters such as pagination, externalIds, businessPartnerTypes and a timestamp.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `ChangelogService` for processing.

### 3. Database Request

A request to the Database is done in the `ChangelogService`. It searches for a changelog using a Specification which contains inserted externalIds,
businessPartnerTypes, timestamp and outputInputEnum type, which is Input in this case.

### 4. Query response

It is returned a Page of ChangelogEntry from the query.

### 5. Address Mapping

The retrieved changelog is now mapped to a ChangelogGateDto in the toGateDto function.

### 6. Response Preparation

An `Page<ChangelogGateDto!>` is prepared to be returned

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of changelogs.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
