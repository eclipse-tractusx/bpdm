````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/output/addresses
    Note over Client,Controller: Method: PUT

    Client->>Controller: upsertAddressesOutput(Collection<AddressGateOutputRequest>)

    Controller->>Controller: Check for duplicate external id's

    Controller->>AddressService: upsertOutputAddresses(Collection<AddressGateOutputRequest>)

    AddressService->>AddressPersistenceService: persistOutputAddressBP(addresses, OutputInputEnum.Output)

    AddressPersistenceService->>GateAddressRepository: findByExternalIdIn(externalIdColl) 
    GateAddressRepository-->>AddressPersistenceService: Returns Set<LogisticAddress>

    loop For each address

    AddressPersistenceService->> LegalEntityRepository: findByExternalIdAndDataType()
    LegalEntityRepository-->>AddressPersistenceService: Returns LegalEntity or Null
    AddressPersistenceService->> SiteRepository: findByExternalIdAndDataType()
    SiteRepository-->>AddressPersistenceService: Returns Site or Null

    end

    loop For each address

    AddressPersistenceService->>AddressPersistenceService: toAddressGateOutput (legalEntityRecord, siteRecord, dataType)

    alt If an record is found in the DB
    AddressPersistenceService->>AddressPersistenceService: updateAddressOutput (existingAddress, address, legalEntityRecord, siteRecord)
    AddressPersistenceService->>GateAddressRepository: update address in the Database
    AddressPersistenceService->>AddressPersistenceService: saveChangelog(address.externalId, ChangelogType.UPDATE, dataType)
    end

    alt If no record is found

    AddressPersistenceService->>GateAddressRepository: save address in the Database if Input Address is persisted

    AddressPersistenceService->>SharingStateService: upsertSharingState(address.toSharingStateDTO(SharingStateType.Success))
    AddressPersistenceService->>AddressPersistenceService: saveChangelog(address.externalId, ChangelogType.CREATE, dataType)
    end

    end

    Note over Controller, Client: Response: 200 OK 
    Note over Controller, Client: Content-Type: application/json
    Controller-->>Client: Response: ResponseEntity<Unit>




````

### 1. Client Request

The client sends a request to persist an output address.

### 2. Controller Check

In the controller a duplicate check is done in the inserted collection of addresses output request. Also, a verification is done to see if the inserted address
has linked sites/legal entity external id's. If both fields are null or both have an external id assigned at the same time, a 400 BAD_REQUEST is thrown.

### 3. Controller Handling

The controller receives the client's request and forwards it to the `AddressService` for processing.

### 4. Service Handling

The service receives the client's request and forwards it to the `AddressPersistenceService` for processing.

### 5 and 6. Database Request and response for Addresses

A request to the Database is done in the `AddressPersistenceService`. It searches for multiple addresses with a collection of inserted external id's. It returns
a Set<LogisticAddress>.

### 7,8,9 and 10. Query Request and response linked Site and Legal Entity to Address

In this loop for each address inserted, two request to the DB are done to check if the inserted address corresponding assigned site or legal entity exist in the
database. if neither legal entity record nor site record are found a 400 BAD_REQUEST is thrown.

### 11. Address Mapping

The address is mapped using the function toAddressGateOutput which assigns it the found site or legal entity in the DB, and also assigns an OutputInputEnum
type, which in this case in output.

### 12. Address data update

If the iterated address is found in the DB in the step 5 and 6, the update logic is used. In this step, the retrieved DB record is updated with the new client
inserted data regarding the address.

### 13. Address update

The new address data is updated in the database

### 14. Changelog creation

A changelog is created in regard to the address update. The function saveChangelog needs the updated external id, OutputInputEnum type which is an output here,
and a ChangelogType which is UPDATE.

### 15. Address data save

If the iterated address is NOT found in the DB in the step 5 and 6, the save logic is used. Also, if the Logistic Address with OutputInputEnum type Input is not
in this step an 400 BAD_REQUEST with the message "Input Logistic Address doesn't exist"
is thrown. Otherwise, the address is persisted to the DB.

### 16. Sharing state upsert

A new sharing state is created as a new address was created. As this in an output persist, the SharingStateType.Success is used for update on the previously
created sharing state in the input.

### 17. Changelog creation

A changelog is created in regard to the address upsert. The function saveChangelog needs the updated external id, OutputInputEnum type which is an output here,
and a ChangelogType which is CREATE.

### 18. Controller Response

The controller sends a response back to the client, indicating the successful persist/update of the address/'s.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
