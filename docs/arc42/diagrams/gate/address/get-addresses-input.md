````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/input/addresses
    Note over Client,Controller: Method: GET

    Client->>Controller: getAddresses(paginationRequest)
    Controller->>AddressService: getAddresses(page, size)

    AddressService->>GateAddressRepository: findByDataType (OutputInputEnum.Input, PageRequest.of(page, size))
    GateAddressRepository-->>AddressService: Returns  Page<LogisticAddress>
    AddressService->>AddressService: toValidLogisticAddresses() <br/>Mapping of LogisticAddress to AddressGateInputDto 

    AddressService-->>Controller: Returns PageDto that contains page, totalElements, <br/>totalPages, contentSize and content ( List<AddressGateInputDto>)

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<AddressGateInputDto>






````

### 1. Client Request

The client sends a request to retrieve addresses using pagination.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `AddressService` for processing.

### 3. Database Request

A request to the Database is done in the `AddressService`. It searches for addresses with input type, and inserted pagination (page, size).

### 4. Query response

It is returned a Page of addresses from the query.

### 5. Address Mapping

The retrieved Page of addresses is now mapped to a list of AddressGateInputDto

### 6. Response Preparation

An `PageDTO<AddressGateInputDto>` is prepared to be returned. It contains page, totalElements, totalPages, contentSize and the content which is
the `list<AddressGateInputDto>`

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of the addresses.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
