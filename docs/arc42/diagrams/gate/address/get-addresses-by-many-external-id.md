````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/input/addresses/search
    Note over Client,Controller: Method: POST

    Client->>Controller: getAddressesByExternalIds(paginationRequest, externalIds)
    Controller->>AddressService: getAddresses(page, size, externalIds)

    Note over AddressService, GateAddressRepository: If externalIds is not null
    AddressService->>GateAddressRepository: findByExternalIdInAndDataType (externalIds, <br/> OutputInputEnum.Input, PageRequest.of(page, size))
    Note over AddressService, GateAddressRepository: If externalIds is null
    AddressService->>GateAddressRepository: findByDataType (OutputInputEnum.Input, PageRequest.of(page, size))
    GateAddressRepository-->>AddressService: Returns  Page<LogisticAddress>
    AddressService->>AddressService: toValidLogisticAddresses() <br/>Mapping of LogisticAddress to AddressGateInputDto 

    AddressService-->>Controller: Returns PageDto that contains page, totalElements, <br/>totalPages, contentSize and content ( List<AddressGateInputDto>)

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<AddressGateInputDto>

````

### 1. Client Request

The client sends a request to retrieve a multiple addresses using one or multiple external id's with pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `AddressService` for processing.

### 3 and 4. Database Request

A request to the Database is done in the `AddressService`. It searches for addresses with inserted external id's, input type, and inserted pagination (page,
size).

If the external id's collection is null, the `findByDataType` is used which does not take into account the external id's. Otherwise,
the `findByExternalIdInAndDataType` is used.

### 5. Query response

It is returned a Page of addresses from the query.

### 6. Address Mapping

The retrieved Page of addresses is now mapped to a list of AddressGateInputDto

### 7. Response Preparation

An `PageDTO<AddressGateInputDto>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<AddressGateInputDto>`

### 8. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of addresses.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
