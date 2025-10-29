# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

For changes to the BPDM Helm charts please consult the [changelog](charts/bpdm/CHANGELOG.md) of the charts directly.

## [7.2.0] - tbd

### Added

- BPDM: Add validity on business partner relation to the golden record process for legal entity relations [#1295](https://github.com/eclipse-tractusx/bpdm/issues/1295)

### Changed

- BPDM Pool: each legal entity can now have only one alternative headquarter

## [7.1.0] - 2025-09-30

### Added

- BPDM: Support for 'IsManagedBy' business partner relations across Gate, Orchestrator, and Pool, including golden record processing and validation for relation chains and unique manager constraints [#1364](https://github.com/eclipse-tractusx/bpdm/issues/1364).
- CICD: Code Coverage Analysis and Reporting for BPDM [#1338](https://github.com/eclipse-tractusx/bpdm/issues/1338)
- BPDM Cleaning Dummy: Configuration and scheduling support for relation cleaning dummy service with unit test coverage [#1383](https://github.com/eclipse-tractusx/bpdm/issues/1383).
- BPDM Pool: Add validation for too many identifiers when creating/updating business partners [1388](https://github.com/eclipse-tractusx/bpdm/issues/1388)
- BPDM: Add IsOwnedBy relation to the golden record process for sharing majority-owned legal hierarchies [1399](https://github.com/eclipse-tractusx/bpdm/issues/1399)

## [7.0.0] - 2025-06-16

### BREAKING

- Gate: Removed DELETE endpoint for business partner relations [#1264](https://github.com/eclipse-tractusx/bpdm/issues/1264)
- Gate: Removed POST endpoint for business partner relations [#1264](https://github.com/eclipse-tractusx/bpdm/issues/1264)
- Gate: Replace GET endpoint for business partner relations with a POST search endpoint having the same capabilties [#1264](https://github.com/eclipse-tractusx/bpdm/issues/1264)
- Gate: Replaced PUT endpoint for business partner relations with a bulk upsert variant [#1264](https://github.com/eclipse-tractusx/bpdm/issues/1264)
- Gate: Removed constraint checks when upserting business partner relations ([1288](https://github.com/eclipse-tractusx/bpdm/issues/1288))
- App: Renamed api attributes/fields with corresponding 25.06 standards for BPDM Pool, Gate and Orchestrator Services([#1304](https://github.com/eclipse-tractusx/bpdm/issues/1304))
- Gate: Changed endpoint paths to better differentiate between business partner or relation endpoints ([#1327](https://github.com/eclipse-tractusx/bpdm/issues/1327))
- Pool: Enforce dataspace participant validation for managing entity in IsManagedBy relation([#1390](https://github.com/eclipse-tractusx/bpdm/issues/1390))

### Known Knowns

- When creating a marketplace app the Portal does not support an internal technical user profile for BPDM permission group [Gate Input Consumer](docs/architecture/08_Crosscutting_Concepts.md).
Therefore, it is currently not possible to create a marketplace app that enables access to [ReadAccessGateInputForSharingMember offers](INSTALL.md#edc-installation) for external VAT services wanting to access sharing member Gates.

### Added

- BPDM Pool: Enhanced logic for legal entity on relation based on relation type IsAlternativeHeadquarterFor.([#1254](https://github.com/eclipse-tractusx/bpdm/issues/1254))
- BPDM Orchestrator: New POST api endpoint creating golden record tasks for business partner relations. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Orchestrator: New POST api endpoint to search for the state of relations golden record tasks  by task identifiers. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Orchestrator: New POST api endpoint to search for the result-state of relations golden record tasks  by task identifiers. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Orchestrator: New POST api endpoint to reserve relations golden record tasks waiting in the given step queue. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Orchestrator: New POST api endpoint to post step results for reserved relations golden record tasks in the given step queue. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Orchestrator: New GET api endpoint to retrieve event log of relations golden record tasks that have finished processing. ([#1274](https://github.com/eclipse-tractusx/bpdm/issues/1274))
- BPDM Gate: Added relation type IsAlternativeHeadquarterFor ([1286](https://github.com/eclipse-tractusx/bpdm/issues/1286))
- BPDM Pool: Added functionality to consume relation golden record tasks. ([#1279](https://github.com/eclipse-tractusx/bpdm/issues/1279))
- BPDM Gate: New GET api endpoint to retrieve sharing states for business partner relations ([1277](https://github.com/eclipse-tractusx/bpdm/issues/1277))
- BPDM Gate: Add functionality to create golden record tasks for business partner relations ([1277](https://github.com/eclipse-tractusx/bpdm/issues/1277))
- BPDM Gate: Add functionality to resolve golden record tasks for business partner relations ([1277](https://github.com/eclipse-tractusx/bpdm/issues/1277))
- BPDM Gate: Add business partner relation output endpoints to Gate API ([1277](https://github.com/eclipse-tractusx/bpdm/issues/1277))
- BPDM Gate: Add relation information to business partner outputs ([1278](https://github.com/eclipse-tractusx/bpdm/issues/1278))
- BPDM Orchestrator: Increased api version to v7 with simultaneous support for version v6([#1294](https://github.com/eclipse-tractusx/bpdm/issues/1294))
- BPDM Pool: Increased api version to v7 with simultaneous support for version v6([#1294](https://github.com/eclipse-tractusx/bpdm/issues/1294))
- BPDM Gate: Increased api version to v7 with simultaneous support for version v6([#1294](https://github.com/eclipse-tractusx/bpdm/issues/1294))
- BPDM Gate: Add sharing state type and updatedAfter query filters to sharing state GET endpoint ([#1249](https://github.com/eclipse-tractusx/sig-release/issues/1249))
- BPDM Pool: Add identifier type format and categories ([1297](https://github.com/eclipse-tractusx/bpdm/issues/1297))
- BPDM Gate: Add changelog endpoints for business partner relations ([1328](https://github.com/eclipse-tractusx/bpdm/issues/1328))

### Changed

## [6.3.0] - 2025-03-06

### Added

- BPDM Pool: Post endpoint to fetch the BPNL/S/A based on the requested identifiers.([#1052](https://github.com/eclipse-tractusx/bpdm/issues/1052))
- BPDM Gate & Orchestrator: Enhance the error handling mechanism for the orchestrator and gate components by extending the list of available error codes.([#1003](https://github.com/eclipse-tractusx/bpdm/pull/1003#pullrequestreview-2477395867))
- BPDM System Test: Tester module which performs automated end-to-end tests on an existing golden record process.([#1070](https://github.com/eclipse-tractusx/bpdm/pull/1070))
- BPDM Gate & Pool: Update linkage from AdditionalAddress to SiteMainAddress and LegalAddress to LegalAndSiteMainAddress.([#1121](https://github.com/eclipse-tractusx/bpdm/issues/1121))
- BPDM Gate: Add endpoints for managing business partner relations ([#1027](https://github.com/eclipse-tractusx/bpdm/issues/1027))
- BPDM System Test: End-to-end test CI/CD workflow setup for golden record process. ([#1155](https://github.com/eclipse-tractusx/bpdm/issues/1155))
- Apps : Enhanced dependency readiness checks with a scheduler to verify connections to required services every 30 seconds and during startup for Pool, Cleaning Service Dummy, and Gate service. ([#1161](https://github.com/eclipse-tractusx/bpdm/issues/1161))
- BPDM Gate: Add consistency check with the golden record Pool making sure that referenced BPNs are still existing ([#1130](https://github.com/eclipse-tractusx/bpdm/issues/1130))
- BPDM Gate: Add externalTimestamp to prevent old request overwrite newer request ([#986](https://github.com/eclipse-tractusx/bpdm/pull/986))

### Changed

- BPDM Gate: Fetched and updated legal name of legal entity from pool while performing partner upload process via CSV([#1141](https://github.com/eclipse-tractusx/bpdm/issues/1141))
- BPDM Orchestrator: When trying to resolve tasks for a step that have has been resolved before, the request is ignored. A HTTP OK instead of a BadRequest will be returned ([#1092](https://github.com/eclipse-tractusx/bpdm/issues/1092))
- BPDM Pool: Remove duplicate BPN request identifier mapping entries and prevent the creation of new duplicates ([#1159](https://github.com/eclipse-tractusx/bpdm/issues/1159))
- BPDM Gate: Fix the Gate not updating business partner output data when it receives already outdated output ([#1185](https://github.com/eclipse-tractusx/bpdm/issues/1185))
- BPDM Pool: Fix resolved tasks by providing business partner data based on the database state. Returned result is now consistent. ([#1158](https://github.com/eclipse-tractusx/bpdm/issues/1158))
- BPDM Pool: Fix missing legal entity (BPNL) associated to address in response while performing entity search ([#1191](https://github.com/eclipse-tractusx/bpdm/issues/1191))
- BPDM Pool: Fix not returning the most-up-to-date legal and site main address when it has been modified by an update to the site ([#1209](https://github.com/eclipse-tractusx/bpdm/issues/1209))
- BPDM Pool: Add check for correct LSA parent hierarchy when consuming golden record tasks ([#1230](https://github.com/eclipse-tractusx/bpdm/issues/1230))
- BPDM Apps: Fix Open-API specification not having scopes property in clientCredentials flow ([#1234](https://github.com/eclipse-tractusx/bpdm/issues/1234))
- BPDM Apps: Fix access rights for the Open-API documents in YAML format ([#1237](https://github.com/eclipse-tractusx/bpdm/issues/1237))

## [6.2.0] - 2024-11-28

### Added

- BPDM Pool: Post endpoint to create a site for LegalAndSiteMainAddress addressType.([#739](https://github.com/eclipse-tractusx/sig-release/issues/739))
- BPDM Orchestrator: Endpoint for checking the result state of given tasks([#1039](https://github.com/eclipse-tractusx/bpdm/pull/1039))
- BPDM Orchestrator: Endpoint for getting event log for finished tasks([#1039](https://github.com/eclipse-tractusx/bpdm/pull/1039))
- BPDM Pool: Enhanced data model for IdentifierTypes by adding three new fields/attributes: abbreviation, transliteratedName, and transliteratedAbbreviation.([#605](https://github.com/eclipse-tractusx/sig-release/issues/605))
- BPDM Pool: Add CX endpoints for searching and updating the CX membership information of legal entities.([#1069](https://github.com/eclipse-tractusx/bpdm/issues/1069))


### Changed

- BPDM Gate: Fix possible out of memory exception when handling large golden record process requests([#1034](https://github.com/eclipse-tractusx/bpdm/pull/1034))
- BPDM Pool: Fix not resolving golden record tasks on exceptions([#1033](https://github.com/eclipse-tractusx/bpdm/pull/1033))
- BPDM Gate: Fixed Gate not resending business partner data to the golden record process on error sharing state when member sends the exact same business partner again([#1035](https://github.com/eclipse-tractusx/bpdm/pull/1035))
- BPDM Orchestrator: Search task endpoint now requires the private record ID for each task. This means only the task creator is able to fetch the task state([#1039](https://github.com/eclipse-tractusx/bpdm/pull/1039))
- BPDM Orchestrator: Now aborts tasks that are outdated (that is when a Gate will send newer business partner data for the same record to the golden record process)([#1036](https://github.com/eclipse-tractusx/bpdm/pull/1036))
- BPDM Pool & Gate: Reduce standard batch size for golden record task processing ([#1032](https://github.com/eclipse-tractusx/bpdm/pull/1032))
- BPDM Orchestrator: Fix possible out-of-memory exception during the execution of large volumes of tasks ([#1029](https://github.com/eclipse-tractusx/bpdm/pull/1029))
- BPDM Cleaning Service Dummy: Add whitespaces between name parts when creating legal name from them ([#1041](https://github.com/eclipse-tractusx/bpdm/pull/1041))
- BPDM Cleaning Service Dummy: Improve duplication check to better distinguish between incoming business partners ([#1040](https://github.com/eclipse-tractusx/bpdm/pull/1040))
- Apps: Updated double precision data type for Geographic-data([#978](https://github.com/eclipse-tractusx/bpdm/issues/978))
- BPDM Gate: Improved error response by adding external id details and reduced csv columns by removing support for uncategorized fields in csv file for partner upload process([#700](https://github.com/eclipse-tractusx/sig-release/issues/700))
- BPDM Cleaning Service Dummy: Added a null check for name parts to ensure proper whitespace handling when constructing the legal name from them. ([#1059](https://github.com/eclipse-tractusx/bpdm/pull/1059))
- BPDM Gate: Enabled Tax Jurisdiction code to save it to the Output. ([#1058](https://github.com/eclipse-tractusx/bpdm/pull/1058))
- BPDM Cleaning Service Dummy: Removed assignment of uncategorized states while performing cleaning legal entity process. ([#1061](https://github.com/eclipse-tractusx/bpdm/pull/1061))
- BPDM Gate: Fixed construction logic for states and identifiers by enabling business partner type ([#1067](https://github.com/eclipse-tractusx/bpdm/pull/1067))
- BPDM Gate: Fixed logic for identifiers to retrieve only generic type on output business partner ([#1067](https://github.com/eclipse-tractusx/bpdm/pull/1067))
- BPDM Gate: Fixed construction logic for states and identifiers by enabling business partner type ([#1067](https://github.com/eclipse-tractusx/bpdm/pull/1067))
- BPDM Pool: When processing golden record tasks the Pool now ignores isCatenaXMemberData field if it is set to null. ([#1069](https://github.com/eclipse-tractusx/bpdm/issues/1069))
- BPDM Gate: Fixed gate output logic to provide states based on business partner type. ([#1097](https://github.com/eclipse-tractusx/bpdm/pull/1097))
- BPDM Cleaning Service Dummy: Removed assignment of uncategorized identifier while performing cleaning task process. ([#1098](https://github.com/eclipse-tractusx/bpdm/pull/1098))
- BPDM Pool: Fix error querying legal forms when non-gleif legacy legal forms are present in the database ([#1107](https://github.com/eclipse-tractusx/bpdm/issues/1107))
- BPDM: Fix denial of service attack vulnerability CVE-2024-47535 ([#1112](https://github.com/eclipse-tractusx/bpdm/issues/1112))
- BPDM Gate: Fix error on writing golden record task result missing legal name into output stage. Result is now correctly written. ([#1115](https://github.com/eclipse-tractusx/bpdm/issues/1115))

## [6.1.0] - [2024-07-15]

### Added

- BPDM Gate: Post endpoint to upload business partner input data using csv file.([#700](https://github.com/eclipse-tractusx/sig-release/issues/700))
- BPDM Gate: GET endpoint to download the csv file template for business partner upload. ([#700](https://github.com/eclipse-tractusx/sig-release/issues/700))
- Apps: Tax Jurisdiction Code to the physical address of a business partner ([#955](https://github.com/eclipse-tractusx/bpdm/issues/955))
- BPDM Orchestrator: Tasks will now be persisted ([#722](https://github.com/eclipse-tractusx/sig-release/issues/722))
- BPDM Orchestrator: Tasks now come with a gate record identifier. This makes it possible for cleaning services to match tasks for the same Gate record ([#711](https://github.com/eclipse-tractusx/sig-release/issues/711)) 

### Changed:

- BPDM Gate: Fix sending business partner data to the golden record service even when they have no changes ([#988](https://github.com/eclipse-tractusx/bpdm/pull/988))
- BPDM Gate: Fix sharing states sometimes taking the wrong task id from the orchestrator ([#989](https://github.com/eclipse-tractusx/bpdm/pull/989))


## [6.0.2] - [2024-07-03]

### Changed

- BPDM Gate: Now sends alternative addresses which are NULL correctly to the Orchestrator ([#801](https://github.com/eclipse-tractusx/portal-backend/issues/801))
- BPDM Pool: Changed Checksum generation algorithm: Now checksum includes the BPN with prefix ([#699](https://github.com/eclipse-tractusx/sig-release/issues/699))


## [6.0.1] - [2024-05-27]

### Removed

- BPDM Gate: Remove unused business partner type filter in query parameters from sharing state endpoint (Does not affect behaviour of API)
- Apps: Add no-auth profile to all BPDM applications. This introduced a shortcut to run an application without any authentication configuration for its API and clients

### Changed

- BPDM Gate: Now correctly sending NULL values for alternative addresses to the golden record process
- BPDM Pool: Name search for business partners now case-insensitive and delivers results on partial matches
- BPDM Pool: Now validate and reject data in golden record tasks only if it is needed for processing (Unchanged business partners are now ignored)

## [6.0.0] - [2024-05-15]

### Removed

- BPDM Bridge (The Cleaning Dummy Service and Orchestrator are the successor applications to simulate a dummy golden record process)
- BPDM Gate: Removed all Legal Entity, Site and Address endpoints (The generic business partner endpoint is their successor)
- BPDM Gate: Removed legal entity classifications form business partner
- BPDM Gate: Removed business partner type from API models and filters
- BPDM Gate: Removed BPN from the sharing state (BPNs can be viewed in the output data of the business partner)
- APIs: Removed the static prefixes 'api/catena' from all API endpoint paths
- BPDM Pool: Removed the flags for `is legal address` and `is site main address` from the logistic address (since it is now expressed by the address type)

### Added

- BPDM Gate: Configuration to prevent the uploaded business partner input data to immediately enter the golden record process. 
In this configuration the business partner data needs to be sent to the golden record process manually over the new state/ready API endpoint.
Default configuration remains automatically sharing.
- BPDM Gate: Limited multi-tenancy support. Business partners are now separated by owner-BPNL.
The owner is determined from the 'bpn' claim in the token.
This means users of a Gate can only see and edit their own business partner data.
- BPDM Pool: New API endpoints to query business partner data which belongs to Catena-X members only
- APIs: Added a major version number to all API endpoint paths indicating the current version of the BPDM APIs.
In the future we will use version numbers in the URL to differentiate between all currently supported major versions of the API
- BPDM Gate Client: Now supports the stats endpoints of the BPDM API

### Changed

- Confidence Criteria: Corrected field name for number of sharing members (formerly number of business partners)
- BPDM Cleaning Service Dummy: Now determines business partner data to be Catena-X member data if it is claimed to be owned
- BPDM Cleaning Service Dummy: Fix ignoring BPNs coming from the sharing member
- BPDM Gate: Fix not correctly updating confidence values it receives from the golden record process
- BPDM Gate: Fix not correctly updating business partner output data from golden record updates in the Pool.
- JAVA version to 21
- BPDM API Permissions: Overhaul of the permissions needed to access the BPDM API endpoints.
Permissions are now more fine-granular and differentiate more clearly between read or write.
For more details consult the Arc42, API documentation and properties files of the respective applications.
- BPDM App Configuration: Now all applications are secured (authenticated and authorized) by default.
You can still deactivate security in the BPDM apps for testing or development purposes though.
- BPDM Pool: Fix Pool trying to update golden records which the golden record process indicated to have no changes
- BPDM Orchestrator: The business partner data for golden record process tasks has been completely overhauled.
Now business partner data is clearly divided into `uncategorized`, `legal entity`, `site` and `additonal address` data.
This model is less verbose and contains less duplicate data.
Additionally, both Pool and Gate can write and read from it making it unnecessary for a cleaning service to provide the data in two different models.



## [5.0.0] - [2024-02-10]

## Added

- Golden Record Process Use Case: Update sharing member business partner from update in golden record
- BPDM Gate: Stats endpoints
- BPDM Gate: Additional possible roles to business partner
- BPDM Cleaning Service Dummy: Duplicate check based on business partner name
- Confidence criteria for all data models

### Changed

- Increase Spring Boot version to 3.1.8
- Changed generic business partner model to include LSA typed properties
- Addresses can now be legal and site main addresses at the same time
- BPDM Gate: Refuse request if token is not issued for owner BPNL

### Deleted

- BPDM Pool: Opensearch component

## [4.1.0] - 2023-11-03

### Added

- Golden record sharing process with dummy cleaning (see points below for details)
- BPDM Orchestrator: New BPDM application managing golden record tasks
- BPDM Cleaning Service Dummy: New BPDM application creating dummy cleaning results from golden record tasks
- BPDM Gate: Endpoints for sharing generic business partner input
- BPDM Gate: Endpoints for querying generic business partner output
- BPDM Gate: Service logic for creating golden record tasks from business partner input and writing result in output
- BPDM Pool: Service Logic for creating golden records from tasks
- BPDM Pool: Service logic for assigning BPNs to golden record tasks
- BPDM Pool: Search for name on address and legal entity level
- Swagger: Bearer token authorization flow
- Workflows: latest-alpha tag for Docker images
- Docker: Healthcheck for images

### Changed

- Apps: Increase Spring Boot version to 3.1.5
- BPDM Gate: New business partner type 'GENERIC' for changelog
- BPDM Gate: New business partner type 'GENERIC' for sharing state
- Workflows: Trivy now targets the latest alpha Docker image instead of the latest release version
- Apps: Increase projectreactor.netty version to fix Trivy vulnerability 


## [4.0.1] - 2023-08-28

### Changed

- BPDM Pool API: Adjust API text descriptions to BPDM Standard
- BPDM Gate API: Adjust API text descriptions to BPDM Standard

### Added

- BPDM Apps: Add legal files to packaged Jar files: LICENSE, NOTICE and DEPENDENCIES
- BPDM Bridge Dummy: Add missing tests for updating business partners

### Fixed

- BPDM Pool: Fix duplicate identifier validation on creating new legal entities

## [4.0.0] - 2023-08-15

### Note

This version introduces breaking changes. Migration files will DELETE existing data in order to perform migration.
Please create a back-up of your business partner data before updating.

### Changed

- BPDM Pool API: Adapt new data model for easier and more intuitive usage, independent of SaaS data model.
- BPDM Gate API: Adapt new data model for easier and more intuitive usage, independent of SaaS data model.
- BPDM: Update dependencies to mitigate vulnerabilities in old versions.

### Added

- BPDM bridge dummy: As new application module.
- BPDM: Umbrella Chart with BPDM Bridge Dummy.

### Fixed
  - BPDM: Deprecated endpoints for retrieving business partners in legacy format.
  - Endpoint for retrieving changelog entries has now improved filtering (breaking API change)

## [3.2.2] - 2023-05-12

### Changed

- Move license "header" of ingress under metadata
- Increase chart versions and adapt changelogs on each chart folder
  * [bpdm-gate](https://github.com/eclipse-tractusx/bpdm/commit/3139a201e78345f2233a24da6ed9cb444ac12f4b#diff-9d1af39984002e7b571d7b1ab9cd19064d00128c3506bf773c718277099e150a)
  * [bpdm-pool](https://github.com/eclipse-tractusx/bpdm/commit/3139a201e78345f2233a24da6ed9cb444ac12f4b#diff-cf14cb4bb6951fd450b91b159c9bb90afc282ea13493c0e5bd317ab011537909)

- Increase version release to 3.2.2

## [3.2.1] - 2023-04-20

### Changed

- Changed increase to spring boot starter version 3.0.5
- Override spring expression transitive dependency to versiomn to 6.0.8
- Increase version release to 3.2.1

## [3.2.0] - 2023-03-17

### Added

- BPDM Pool API: client library for accessing BPDM Pool API application
- BPDM Gate API: client library for accessing a BPDM Gate API application

### Fixed

- BPDM Gate: internal server error when invoking the POST business-partners/type-match endpoint
- BPDM Gate: output endpoints could miss returning any information for Business Partners in some cases
- BPDM: security issue CVE-2022-1471
- BPDM: applications returning 403 status code on internal errors

## [3.1.0] - 2023-03-08

### Changed

- BPDM Gate: Endpoints returning input versions of legal entity, site and address now provide processStartedAt timestamp.
- BPDM Gate: Endpoints for output versions of legal entity, site and address now return error infos and pending entries

### Fixed

- BPDM Gate: For a business partner with a child relation, this relation could be returned as parent relation erroneously. 
- BPDM Gate: When a business partner with a child relation was updated, this relation was erroneously deleted, rendering the previous child invalid.

## [3.0.3] - 2023-02-23

### Security

- BPDM Pool: Update dependencies to mitigate vulnerabilities in old versions

### Changed

- Replaced manual JSON deserializer implementations for various DTOs by generic DataClassUnwrappedJsonDeserializer.

## [3.0.2] - 2023-02-15

### Changed

- BPDM Pool: SaaS Sharing Service Importer now also adds missing import entries (SaaS-ID to BPN) when encountering Business Partners that already have BPNs in
  the SaaS storage (possible due to legacy imports done before)
- BPDM Pool: Business Partner searches have now limited pagination length. Limit is adjustable in configuration.

### Fixed

- BPDM Gate: Now possible to startup without supplying any environment variables or property overwrites
- BPDM Pool: Now possible to deep paginate over business partners (>10000 entries)
- Fixed various vulnerabilities by upgrading affected libraries

## [3.0.1] - 2023-01-24

### Security

- Fixed various vulnerabilities by upgrading affected libraries

## [3.0.0] - 2022-11-30

### Added

- New endpoint to search for addresses in pool
- New endpoint to get valid/mandatory identifiers per country

### Changed

- Importer in pool now supports Legal Entities, Sites and Addresses
- Update helm charts to align with best practices
- Update logic for fetching data from SaaS provider via gate

## [2.0.2] - 2022-11-17

### Added

- New component "gate" for the integration into the Golden Record process
- Endpoint for creating and retrieving legal entities via gate
- Endpoint for type matching via gate

## [2.0.1] - 2022-10-20

### Fixed

- Fixed issue in legal entity import for missing "thoroughfare" values
- Fix issue when elliptic curve signing key is configured in keycloak

### Security

- Update dependencies with CVEs

## [2.0.0] - 2022-09-21

### Added

- Added the possibility to get BusinessPartner-Legal Entities, BusinessPartner-Sites and BusinessPartner-Addresses as additional information
- Added OpenSearch functionality to search Business Partners by different parameters
- Added reference integration for one SaaS controller
- Helm Charts available via Helm repository
- Swagger UI now integrated with Portal authentication
- BPDM data model

### Deprecated

- Endpoints for retrieving business partners in legacy format
