````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/input/legal-entities
    Note over Client,Controller: Method: PUT

    Client->>Controller: upsertLegalEntities(Collection<AddressGateInputRequest>)

    Controller->>Controller: Check for duplicate external id's

    Controller->>LegalEntityService: upsertLegalEntities(Collection<AddressGateInputRequest>)

    LegalEntityService->>LegalEntityPersistenceService: persistLegalEntitiesBP(legalEntities, OutputInputEnum.Input)

    LegalEntityPersistenceService->>LegalEntityRepository: findDistinctByExternalIdIn(Collection<String>) 
    LegalEntityRepository-->>LegalEntityPersistenceService: Returns Set<LegalEntity>

    loop For each legal entity

    LegalEntityPersistenceService->>LegalEntityPersistenceService: LegalEntityGateInputRequest <br/> mapping to Legal Entity

    LegalEntityPersistenceService->> GateAddressRepository: findByExternalIdAndDataType()
    GateAddressRepository-->>LegalEntityPersistenceService: Returns LogisticAddress (logisticAddressRecord) or Null

    alt If an record is found in the DB
    LegalEntityPersistenceService->>LegalEntityPersistenceService: updateAddress (logisticAddressRecord, LegalEntity.legalAddress)
    LegalEntityPersistenceService->>LegalEntityPersistenceService: updateLegalEntity(existingLegalEntity, legalEntity, logisticAddressRecord)
    LegalEntityPersistenceService->>LegalEntityRepository: update legalEntity in the Database
    LegalEntityPersistenceService->>LegalEntityPersistenceService: saveChangelog(legalEntity.externalId, ChangelogType.UPDATE, dataType)
    end

    alt If no record is found
    LegalEntityPersistenceService->>LegalEntityRepository: save legal Entity in the Database
    LegalEntityPersistenceService->>SharingStateService: upsertSharingState(legalEntity.toSharingStateDTO())
    LegalEntityPersistenceService->>LegalEntityPersistenceService: saveChangelog(legalEntity.externalId, ChangelogType.CREATE, dataType)
    end

    end

    Note over Controller, Client: Response: 200 OK 
    Note over Controller, Client: Content-Type: application/json
    Controller-->>Client: Response: ResponseEntity<Unit>




````

### 1. Client Request

The client sends a request to persist an input legal entity.

### 2. Controller Check

In the controller an external id duplicate check is done in the inserted collection of legal entities input request. If there are duplicates, a 400 BAD_REQUEST
is thrown.

### 3. Controller Handling

The controller receives the client's request and forwards it to the `LegalEntityService` for processing.

### 4. Service Handling

The service receives the client's request and forwards it to the `LegalEntityPersistenceService` for processing.

### 5 and 6. Database Request and response for Legal Entity

A request to the Database is done in the `LegalEntityPersistenceService`. It searches for multiple Legal Entities with a collection of inserted external id's.
It returns a Set<LegalEntity>.

### 7. Mapping of LegalEntityGateInputRequest

Here a mapping of the LegalEntityGateInputRequest to Legal Entity is done

### 8 and 9. Query Request and response linked Addresses

In this loop for each legal entity inserted, a query is done to found if the associated address external id inserted has already a record in the database. If
there is no need to create a new one, and it can be used in case of a update. If none is found, it is created as long with the legal entity.

### 10 and 11. Address and Legal Entity data update

If the iterated legal entity is found in the DB in the step 5 and 6, the update logic is used. In this step, the retrieved DB record is updated with the new
client inserted data regarding the legal entity. Also, the associated address data is updated in the updateAddress() function.

### 12. Legal Entity update

The new legal entity data is updated in the database

### 13. Changelog creation

A changelog is created in regard to the legal entity update. The function saveChangelog needs the updated external id, OutputInputEnum type which is an Input
here, and a ChangelogType which is UPDATE.

### 14. Legal entity data save

If the iterated legal entity is NOT found in the DB in the step 5 and 6, the save logic is used. In this step, legal entity is persisted to the DB.

### 15. Sharing state upsert

A new sharing state is created as a new legal entity was created.

### 16. Changelog creation

A changelog is created in regard to the legal entity upsert. The function saveChangelog needs the updated external id, OutputInputEnum type which is an Input
here, and a ChangelogType which is CREATE.

### 17. Controller Response

The controller sends a response back to the client, indicating the successful persist/update of the legal entity/'s.