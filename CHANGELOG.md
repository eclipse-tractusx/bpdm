# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

## [4.0.0] - 2023-08-04

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
