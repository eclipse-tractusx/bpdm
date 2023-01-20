# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

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
