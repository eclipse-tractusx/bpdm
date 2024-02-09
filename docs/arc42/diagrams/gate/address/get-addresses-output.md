````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/output/addresses/search
    Note over Client,Controller: Method: POST

    Client->>Controller: getAddressesOutput(paginationRequest, externalIds)
    Controller->>AddressService: getAddressesOutput(page, size, externalIds)

    Note over AddressService, GateAddressRepository: If externalIds is not null
    AddressService->>GateAddressRepository: findByExternalIdInAndDataType (externalIds, <br/> OutputInputEnum.Output, PageRequest.of(page, size))
    Note over AddressService, GateAddressRepository: If externalIds is null
    AddressService->>GateAddressRepository: findByDataType (OutputInputEnum.Output, PageRequest.of(page, size))
    GateAddressRepository-->>AddressService: Returns  Page<LogisticAddress>
    AddressService->>AddressService: toValidOutputLogisticAddresses() <br/>Mapping of LogisticAddress to AddressGateOutputDto 

    AddressService-->>Controller: Returns PageDto that contains page, totalElements, <br/>totalPages, contentSize and content ( List<AddressGateOutputDto>)

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: PageDto<AddressGateOutputDto>
````

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
