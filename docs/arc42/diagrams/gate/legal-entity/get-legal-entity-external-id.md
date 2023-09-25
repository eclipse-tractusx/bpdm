````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: /input/legal-entities/{externalId}
    Note over Client,Controller: Method: GET

    Client->>Controller: getLegalEntityByExternalId(externalId)
    Controller->>LegalEntityService: getLegalEntityByExternalId(externalId)

    LegalEntityService->>LegalEntityRepository: findByExternalIdAndDataType(externalId, OutputInputEnum.Input)
    LegalEntityRepository-->>LegalEntityService: Returns LegalEntity

    LegalEntityService->>LegalEntityService: toValidSingleLegalEntity(legalEntity)

    LegalEntityService-->>Controller: Returns LegalEntityGateInputDto

    Note over Controller: Response: 200 OK 
    Note over Controller: Content-Type: application/json
    Controller-->>Client: Response: LegalEntityGateInputDto

````

### 1. Client Request

The client sends a request to retrieve a legal entity using an external id.

### 2. Controller Handling

The controller receives the client's request and forwards it to the `LegalEntityService` for processing.

### 3. Database Request

A request to the Database is done in the `LegalEntityService`. It search for an legal entity with a specific external id and Input type.

### 4. Query response

It is returned an LegalEntity from the query or if nothing is found an exception is thrown.

### 5. LegalEntity Mapping

The retrieved legal entity is now mapped to a LegalEntityGateInputDto

### 6. Response Preparation

An `LegalEntityGateInputDto` is prepared to be returned

### 7. Controller Response

The controller sends the prepared response back to the client, indicating the successful retrieval of a legal entity.