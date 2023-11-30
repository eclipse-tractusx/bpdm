````mermaid
sequenceDiagram

    autonumber
    Note over Client,Controller: Path: api/catena/output/sites
    Note over Client,Controller: Method: PUT

    Client->>Controller: upsertSitesOutput(Collection<SiteGateOutputRequest>)

    Controller->>Controller: Check for duplicate external id's

    Controller->>SiteService: upsertSitesOutput(Collection<SiteGateOutputRequest>)

    SiteService->>SitePersistenceService: persistSitesOutputBP(sites, OutputInputEnum.Output)

    SitePersistenceService->>SiteRepository: findByExternalIdIn(Collection<String>) 
    SiteRepository-->>SitePersistenceService: Returns Set<Site>

    loop For each site

    SitePersistenceService->> SiteRepository: findByExternalIdAndDataType()
    SiteRepository-->>SitePersistenceService: Returns LegalEntity (legalEntityRecord) 

    SitePersistenceService->>SitePersistenceService: SiteGateOutputRequest <br/> mapping to Site

    alt If an record is found in the DB

    SitePersistenceService->> SiteRepository: findByExternalIdAndDataType()
    SiteRepository-->>SitePersistenceService: Returns LogisticAddress (logisticAddressRecord) 

    SitePersistenceService->>SitePersistenceService: updateAddress (logisticAddressRecord, fullSite.mainAddress)
    SitePersistenceService->>SitePersistenceService: updateSiteOutput(existingSite, site, legalEntityRecord)
    SitePersistenceService->>SiteRepository: update site in the Database
    SitePersistenceService->>SitePersistenceService: saveChangelog(site.externalId, ChangelogType.UPDATE, datatype)
    end

    alt If no record is found
    SitePersistenceService->>SiteRepository: save site in the Database if Input site record is persisted
    SitePersistenceService->>SharingStateService: upsertSharingState(site.toSharingStateDTO(SharingStateType.Success))
    SitePersistenceService->>SitePersistenceService: saveChangelog(site.externalId, ChangelogType.CREATE, datatype)
    end

    end

    Note over Controller, Client: Response: 200 OK 
    Note over Controller, Client: Content-Type: application/json
    Controller-->>Client: Response: ResponseEntity<Unit>


````

### 1. Client Request

The client sends a request to persist an output site.

### 2. Controller Check

In the controller an external id duplicate check is done in the inserted collection of sites output request. If there are duplicates, a 400 BAD_REQUEST is
thrown.

### 3. Controller Handling

The controller receives the client's request and forwards it to the `SiteService` for processing.

### 4. Service Handling

The service receives the client's request and forwards it to the `SitePersistenceService` for processing.

### 5 and 6. Database Request and response for sites

A request to the Database is done in the `SitePersistenceService`. It searches for multiple sites with a collection of inserted external id's. It returns a
Set<Site>.

### 7 and 8. Query Request and response linked Legal Entities

In this loop for each site inserted, a query is done to found if the associated legal entity external id inserted has a record in the database. If there is no
legal entity with the inserted external id, an error is thrown, as a site needs a legal entity created to be saved.

### 9. Mapping of SiteGateOutputRequest

Here a mapping of the SiteGateOutputRequest to Site is done

### 10 and 11. Query Request and response linked Addresses

In this loop for each site inserted, a query is done to found if the associated address external id inserted has already a record in the database. If there is
no need to create a new one, and it can be used in case of an update. If none is found, it is created as long with the site.

### 12 and 13. Address and Site data update

If the iterated site is found in the DB in the step 5 and 6, the update logic is used. In this step, the retrieved DB record is updated with the new client
inserted data regarding the site. Also, the associated address data is updated in the updateAddress() function.

### 14. Site update

The new site data is updated in the database

### 15. Changelog creation

A changelog is created in regard to the site update. The function saveChangelog needs the updated external id, OutputInputEnum type which is an Output here, and
a ChangelogType which is UPDATE.

### 16. Site data save

If the iterated site is NOT found in the DB in the step 5 and 6, the save logic is used. Also, if the Site with OutputInputEnum type Input is not in this step
an 400 BAD_REQUEST with the message "Input Site doesn't exist"
is thrown. Otherwise, the site is persisted to the DB.

### 17. Sharing state upsert

A new sharing state is created as a new site was created. As this in an output persist, the SharingStateType.Success is used for update on the previously
created sharing state in the input.

### 18. Changelog creation

A changelog is created in regard to the site upsert. The function saveChangelog needs the updated external id, OutputInputEnum type which is an Output here, and
a ChangelogType which is CREATE.

### 19. Controller Response

The controller sends a response back to the client, indicating the successful persist/update of the site/'s.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
