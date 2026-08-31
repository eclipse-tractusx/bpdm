# Migration Guide

<!-- TOC -->
* [Migration Guide](#migration-guide)
  * [7.4.x to 7.5.x](#74x-to-75x)
    * [Alternative Headquarter Relation Directionality](#alternative-headquarter-relation-directionality)
      * [Required Operator Actions](#required-operator-actions)
      * [What Happens After Upgrade](#what-happens-after-upgrade)
      * [Disjoint Periods](#disjoint-periods)
    * [Unique site names per legal entity](#unique-site-names-per-legal-entity)
    * [Script variants removed from the deprecated Pool v6 API](#script-variants-removed-from-the-deprecated-pool-v6-api)
      * [Impact](#impact)
    * [Script variants are validated like invariant data (Pool)](#script-variants-are-validated-like-invariant-data-pool)
      * [Impact](#impact-1)
  * [7.3.x to 7.4.x](#73x-to-74x)
    * [Breaking rename of relation DTO fields (Gate)](#breaking-rename-of-relation-dto-fields-gate)
      * [Impact](#impact-2)
      * [Rationale](#rationale)
    * [New relation type for addresses](#new-relation-type-for-addresses)
    * [No required operator actions](#no-required-operator-actions)
    * [Reason Codes](#reason-codes)
    * [Postgres Upgrade (BREAKING)](#postgres-upgrade-breaking)
    * [Keycloak Upgrade (BREAKING)](#keycloak-upgrade-breaking)
  * [7.2.x to 7.3.x](#72x-to-73x)
    * [Automatic Confidence Level](#automatic-confidence-level)
  * [7.1.x to 7.2.x](#71x-to-72x)
    * [Alternative Headquarters Restriction](#alternative-headquarters-restriction)
    * [Default Logging Level](#default-logging-level)
  * [7.0.x to 7.1.x](#70x-to-71x)
    * [EDC Version 0.11](#edc-version-011)
    * [Golden Record Process for IsManagedBy Relations](#golden-record-process-for-ismanagedby-relations)
    * [Business Partner Identifier Amount Limit](#business-partner-identifier-amount-limit)
<!-- TOC -->


## 7.4.x to 7.5.x

### Alternative Headquarter Relation Directionality

The IsAlternativeHeadquarterFor relation now carries semantic meaning through its direction: the relation starts at the alternative entity and ends at the main entity.
Previously, relation direction was normalized by creation timestamp (`createdAt`), making both ends symmetric.
This version establishes **directional** relations and enforces **star topology** to keep ownership trees separate per real-world entity.

#### Required Operator Actions

**CRITICAL: This is a mandatory data remediation step before upgrading.**

1. **Review and re-establish relation directions:**
   - All existing IsAlternativeHeadquarterFor relations were stored with arbitrary direction (based on `createdAt`).
   - Operators must review each relation and determine the intended roles:
     - **Alternative**: The headquarter entity that serves as a secondary representation
     - **Main**: The authoritative headquarter entity that participates in ownership
   - In both the **Pool** and the **Gate output database**, re-create relations with the correct direction:
     - Delete the incorrectly-directed relation
     - Create a new relation with source = alternative and target = main

2. **Move ownership and flags to main entity:**
   - Any alternative entity currently participating in IsOwnedBy relations (as source or target) must have that ownership moved to its corresponding main entity.
   - Any alternative entity carrying the `ownershipUltimate = true` flag must have that flag cleared and moved to its corresponding main entity.
   - Update the **Pool** database directly to:
     - Remove the entity from ownership relations
     - Clear the `ownershipUltimate` flag
     - Apply the flag and ownership relations to its main entity

3. **Validate after remediation:**
   - Confirm that in the Pool and Gate:
     - Each IsAlternativeHeadquarterFor relation has the correct direction (alternative → main)
     - No alternative entity participates in ownership
     - No alternative entity carries the `ownershipUltimate` flag
     - Star topology is maintained (multiple alternatives can point to the same main, but no cycles or reversed relations)

#### What Happens After Upgrade

After this version, the Pool enforces:

- **Directionality**: IsAlternativeHeadquarterFor relations are stored and validated with direction as supplied; no reordering.
- **Star topology**: 
  - An alternative cannot point at another alternative
  - A main cannot point at an alternative
  - An alternative cannot be both main and alternative in overlapping validity periods
  - Re-upsert of the same pair still works and updates validity periods
- **Ownership separation**:
  - Only the main entity can participate in IsOwnedBy relations
  - Only the main entity can carry the `ownershipUltimate` flag
  - An alternative caught violating these rules causes the task to fail because an alternative headquarter cannot carry the ultimate-owner flag
- **Rejection rules**:
  - Relations are rejected if:
    - Source is already the main of an overlapping relation
    - Target is already the alternative of an overlapping relation
    - Source is already the alternative of a different main in an overlapping period
    - Source participates in an overlapping IsOwnedBy relation
    - Source carries `ownershipUltimate = true`
    - Either end is an alternative in an overlapping period (for IsOwnedBy)
  - The rejection reason names both BPNLs and the broken rule

#### Disjoint Periods

Entities may still take different roles in non-overlapping periods:

- An entity may be alternative to different mains in non-overlapping periods
- An entity may participate in ownership in periods after its alternative-headquarter relation has ended
- No action is required for relations that do not overlap in time

### Unique site names per legal entity

A site's name must now be unique within its owning legal entity.
This is enforced by a new database constraint (`uc_sites_legal_entity_name`) on the Pool's `sites` table.

> Very important:
> The migration adds this constraint directly and does **not** resolve pre-existing duplicates.
> If your Pool already contains two or more sites with the same name under the same legal entity, the migration will fail.
> Please make sure that site names are unique within each legal entity before upgrading, otherwise the deployment will not start.

### Script variants removed from the deprecated Pool v6 API

Script variants were introduced in 7.4.0, at which point the Pool v6 API was already frozen.
They should never have become part of it, and the v6 API no longer offers them:
`scriptVariants` is gone from the v6 address create and update requests, from the v6 site DTO used by site create and update, and from the v6 request that creates a site with the legal address as its main address.

#### Impact

- **No operator action is required and no data is migrated.** Script variants themselves are unchanged; only the v6 API surface loses them.
  The v7 API and the golden record process remain the way to read and write them.
- **v6 callers sending `scriptVariants` are not rejected.** The field is ignored rather than refused, so existing v6 requests keep working and no request needs to be changed to avoid an error.
- **A v6 write leaves a business partner without script variants.** Because a write replaces a business partner's full content, updating over v6 a business partner that has script variants — gained over v7 or through the golden record process — removes them.
  This also produces an `UPDATE` changelog entry, so sharing members will see the script variants disappear from their Gate output.
  Mixing v6 writes with script variants is therefore not supported; use the v7 API for business partners that carry them.
- **Consumers compiling against `bpdm-pool-api` must adjust.** The affected v6 request classes lost a constructor parameter.
  Code that constructs them positionally will no longer compile and needs the `scriptVariants` argument removed.

### Script variants are validated like invariant data (Pool)

A script variant is a business partner written out completely in another script, so it now has to carry the same mandatory content as the invariant data it mirrors:

- a legal entity script variant must carry a `legalName`
- a site script variant must carry a `name`
- an address script variant must carry `physicalAddress.city`, and — if it supplies an `alternativeAddress` at all — `alternativeAddress.city`
- no two script variants of one business partner may share the same script code
- a legal entity or site script variant is only valid where its legal address or site main address covers the same script code,
  and no write may take that coverage away from a business partner that still needs it

Those fields are non-null in the v7 API, so the requirement is part of the schema rather than a check applied afterwards.
Blank and whitespace-only values are still rejected by validation, per entry.

#### Impact

- **The migration deletes script variants that do not meet the rules.** `V7_5_0_5__require_script_variant_content.sql` removes
  `legal_entity_script_variants` rows without a `legal_name`, `site_script_variants` rows without a `name`, `address_script_variants` rows without a `phy_city`,
  and legal entity or site script variants whose script code the owning legal address or site main address does not cover.
  It then sets those three columns `NOT NULL`.
  If you need to keep such data, export it before upgrading — the rows are invalid under the new contract and cannot be represented by the API any more.
- **Requests missing a required field are rejected as a whole.** Because the field is non-null in the schema, a missing value fails deserialization with a `400` for the entire request instead of a per-entry error.
  A blank value still yields a per-entry `ErrorInfo`; the new codes are `ScriptVariantLegalNameMissing`, `ScriptVariantNameMissing`, `ScriptVariantCityMissing`, `ScriptVariantDuplicateScriptCode`,
  `ScriptVariantNotCoveredByMainAddress`, `ScriptVariantCoverageStillNeeded` and the `LegalAddress…` / `MainAddress…` counterparts.
- **A script variant can no longer translate only the address.** In the v7 and Orchestrator models one script variant carries the name *and* the address of a script code together.
  A sharing member who transliterates the legal address must therefore also transliterate the legal name (and likewise for a site).
  Golden record tasks whose script variants do not meet the rules end up in the error state, and the sharing member sees the reason in the Gate sharing state.
- **A site created on an existing address can only be named in the scripts that address covers.** The endpoint that takes the legal address as the site main address
  rejects a site script variant whose script code the legal address does not cover.
- **An update may not strand another business partner's coverage.** Every write that replaces an address's script variants — an address update, a site update, a legal entity update, and the
  golden record process creating a site on an existing address — is rejected with `ScriptVariantCoverageStillNeeded` when the new content drops a script code that the address's legal entity
  or one of its sites is still named in. This is reachable wherever an address serves more than one business partner: a main address shared by several sites, or an address that is both a
  legal address and a site main address. The error names the script code and the BPN that still requires it.
- **A golden record task that puts a site on an existing address must carry that address's coverage.** The Pool now applies the site main address payload of such a task to the referenced
  address instead of discarding it, which is what lets a newly shared site be named in its own scripts. In exchange, an enrichment service that places a site on an address another site is
  already named on has to include that address's existing coverage in its payload — the Pool does not merge them, it rejects the task. Records that fail this way end in the error sharing state.
  A task is judged as a whole, not per write: it may freely rewrite the business partners it carries itself — a legal entity and its site sharing one address can move to another script
  together — and is only rejected for coverage it would take away from a business partner it does not carry.
- **A headquarter relocation drops the legal entity's uncovered script variants.** When the new legal address does not cover a script code, the legal entity's script variant for it is removed
  and an `UPDATE` changelog entry is emitted, so sharing members will see the script variant disappear from their Gate output.
- **Consumers compiling against `bpdm-pool-api` must adjust.** `SiteScriptVariantDto.name`, `SiteHeaderScriptVariantDto.name`, `LegalEntityScriptVariantDto.legalName`,
  `PhysicalAddressScriptVariantDto.city` and `AlternativeAddressScriptVariantDto.city` are non-null, and `PhysicalAddressScriptVariantDto.city` and `PostalAddressScriptVariantDto.physicalAddress`
  lost their default values, so they must be supplied explicitly.

## 7.3.x to 7.4.x

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
The [Exposing BPDM Over EDC](README.md#exposing-bpdm-over-edc) section and the consumer side under [Access BPDM over EDC](../api/README.md#access-bpdm-over-edc) show how to setup an offer and negotiation between two 0.11 EDCs.
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