````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/input/addresses/{externalId}
    Note over Client,Controller: Method: GET

    Client->>Controller: getAddressByExternalId(externalId)
    Controller->>AddressService: getAddressByExternalId(externalId)

    AddressService->>GateAddressRepository: findByExternalIdAndDataType(externalId, OutputInputEnum.Input)
    GateAddressRepository-->>AddressService: Returns LogisticAddress
    AddressService->>AddressService: Mapping of LogisticAddress to AddressGateInputDto

    AddressService-->>Controller: Returns AddressGateInputDto 

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: AddressGateInputDto

````

### 1. Client Request

The client sends a request to retrieve an address using an external id.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `AddressService` for processing.

### 3. Database Request

A request to the Database is done in the `AddressService`. It searches for an address with a specific external id and Input type.

### 4. Query response

It is returned an address from the query or if nothing is found an exception is thrown.

### 5. Address Mapping

The retrieved address is now mapped to a AddressGateInputDto

### 6. Response Preparation

An `AddressGateInputDto` is prepared to be returned

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of an address.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
