# Migration Guide

<!-- TOC -->
* [Migration Guide](#migration-guide)
  * [Next](#next)
    * [Breaking rename of relation DTO fields (Gate)](#breaking-rename-of-relation-dto-fields-gate)
      * [Impact](#impact)
      * [Rationale](#rationale)
    * [New relation type for addresses](#new-relation-type-for-addresses)
    * [No required operator actions](#no-required-operator-actions)
    * [Reason Codes](#reason-codes)
  * [7.2.x to 7.3.x](#72x-to-73x)
    * [Postgres Upgrade (BREAKING)](#postgres-upgrade-breaking)
    * [Keycloak Upgrade (BREAKING)](#keycloak-upgrade-breaking)
  * [7.2.x to 7.3.x](#72x-to-73x-1)
    * [Automatic Confidence Level](#automatic-confidence-level)
  * [7.1.x to 7.2.x](#71x-to-72x)
    * [Alternative Headquarters Restriction](#alternative-headquarters-restriction)
    * [Default Logging Level](#default-logging-level)
  * [7.0.x to 7.1.x](#70x-to-71x)
    * [EDC Version 0.11](#edc-version-011)
    * [Golden Record Process for IsManagedBy Relations](#golden-record-process-for-ismanagedby-relations)
    * [Business Partner Identifier Amount Limit](#business-partner-identifier-amount-limit)
<!-- TOC -->


## Next

### Breaking rename of relation DTO fields (Gate)

In previous releases, relation outputs in the Gate API exposed the fields:

- `sourceBpnL`
- `targetBpnL`

These names were technically incorrect:

- They implied the fields were **always** BPNLs.
- They were not suitable for the newly introduced **address relations**, where BPNAs must be returned.

To correct this and make the fields generic, the following rename was implemented:

- `sourceBpnL` → `sourceBpn`
- `targetBpnL` → `targetBpn`

#### Impact
- This is technically a *breaking change* because:
    - API response field names changed.
    - Database column names changed accordingly.
- However, these fields were **not used by any consumers** to date (based on internal usage and customer feedback).
- Therefore the practical impact is negligible.

#### Rationale
- Gate now supports both:
    - Legal entity relations → BPNL
    - Address relations → BPNA
- A neutral naming scheme (`sourceBpn`, `targetBpn`) avoids confusion and future-proofs the API.
- This change is required for consistency with the newly introduced address relation functionality.

---

### New relation type for addresses

Gate now exposes a dedicated relation type:

- `IsReplacedBy`

This type applies only to address relations and is validated accordingly.

---

### No required operator actions

- No existing data needs to be changed.
- No cleanup or special deployment steps needed.


### Reason Codes

Each business partner relation now needs a mandatory reason code.
Reason codes are not standardized and are therefore managed by the operator of the golden record process.

The list of available reason codes should be managed in the golden record Pool through to the new metadata endpoints.

> Very important:
> Since reason codes are mandatory and there are no default reason codes this repository does not contain any migration scripts for existing relations.
> Therefore, if there are already relations present in BPDM the operator needs to add migration scripts assigning reason codes to those relations.

## 7.2.x to 7.3.x

### Postgres Upgrade (BREAKING)

The embedded Postgres of the BPDM Charts has been updated from 15 to 18.
The subchart's vendor also changed from Bitnami to Cloudpirates so we expect not much of any backwards compatibility for Chart features.
In order to migrate your data please consult the [Tractus-X common migration guide](https://github.com/eclipse-tractusx/tutorial-resources/blob/keycloak-migration/migration-guides/GENERIC_POSTGRESQL_MIGRATION_GUIDE.md).

Please note that using the embedded Postgres for BPDM Chart deployments is discouraged for production use.
We recommend to host an external Postgres database and alter the BPDM Chart configuration to access such database.

### Keycloak Upgrade (BREAKING)

The embedded Central-IDP dependency of the BPDM Charts has been replaced by a Cloudpirates Keycloak Chart.
This means not only have the Chart features dramatically changed but also the Keycloak version is upgraded from 25 to 26.
In order to migrate your data please consult the  [Tractus-X common migration guide](https://github.com/eclipse-tractusx/tutorial-resources/blob/keycloak-migration/migration-guides/GENERIC_BITNAMI_TO_CLOUDPIRATES_KEYCLOAK_MIGRATION_GUIDE.md).

Please note that the embedded Keycloak is only meant for test and development purposes and absolutely not for production use.
We recommend to host an external Central-IDP or common Keycloak instance and alter the BPDM Chart configuration to access it.

## 7.2.x to 7.3.x

### Automatic Confidence Level

A golden record's confidence level is now automatically managed by the Pool according to the [golden record standards](https://catenax-ev.github.io/docs/next/standards/CX-0076-GoldenRecordEndtoEndRequirementsStandard#2112-confidence-level).

Please be aware that this version will automatically update the confidence levels of all existing golden records in the Pool.
This update will also result in changelog entries.
This way, the new confidence levels will be propagated to all sharing members and interested parties.

Please note that this migration will create changelog entries for every existing golden record in the Pool.

## 7.1.x to 7.2.x

### Alternative Headquarters Restriction

Now each legal entity can have only up to one alternative headquarters.
Please make sure in your Pool and Gate output database each legal entity is part of up to only one such relation and remove relations that violate this constraint.
Otherwise, the golden record process may show unexpected behaviour. 

### Default Logging Level

In order to reduce unnecessary logging output during CICD processing the new default logging level has been set to `INFO`.
`DEBUG` level is now only meant to be activated when actually debugging BPDM.
If you wish to set the logging level to `DEBUG` as before you can insert the following configuration into the application properties:

```yaml
logging:
  level:
    org:
      eclipse:
        tractusx:
          bpdm: DEBUG
```

## 7.0.x to 7.1.x

### EDC Version 0.11

BPDM 7.1 was tested on and now supports EDC version 0.11.
The [Postman documentation](../postman) shows how to setup an offer and negotiation between two 0.11 EDCs.
The new EDC version now supports two different DCP versions 0.8 and 1.0.
Creating new offers with the new EDC is not backwards compatible for either DCP usage.
However, if you have migrated versions with existing offers, they will still be usable.
In our Postman examples we only show the usage of the 1.0 DCP including how to create new offers.

For more information have a look at the [migration guide for the EDC](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/migration/2025-09-Version_0.10.x_0.11.x.md).

### Golden Record Process for IsManagedBy Relations

This release introduces sharable IsManagedBy relations. 
If you have previously created IsManagedBy relations please note that these relations will be shared with the golden record process after migration.

### Business Partner Identifier Amount Limit

This release introduces a limit of 100 identifiers for a golden record.
If for whatever reason your golden record Pool contains a business partner with over 100 identifiers it is recommended to reduce that number to avoid unintended behaviour.
Sharing members can still upload business partners with more than 100 identifiers in their Gates.
However, the Pool will not accept such business partners (unless adjusted inside the refinement process).