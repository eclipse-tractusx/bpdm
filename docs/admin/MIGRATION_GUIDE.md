# Migration Guide

<!-- TOC -->
* [Migration Guide](#migration-guide)
  * [7.2.x to 7.3.x Migration Guide](#72x-to-73x-migration-guide)
    * [1. Breaking rename of relation DTO fields (Gate)](#1-breaking-rename-of-relation-dto-fields-gate)
      * [Impact](#impact)
      * [Rationale](#rationale)
    * [2. New relation type for addresses](#2-new-relation-type-for-addresses)
    * [3. No required operator actions](#3-no-required-operator-actions)
  * [7.1.x to 7.2.x](#71x-to-72x)
    * [Alternative Headquarters Restriction](#alternative-headquarters-restriction)
    * [Default Logging Level](#default-logging-level)
  * [7.0.x to 7.1.x](#70x-to-71x)
    * [EDC Version 0.11](#edc-version-011)
    * [Golden Record Process for IsManagedBy Relations](#golden-record-process-for-ismanagedby-relations)
    * [Business Partner Identifier Amount Limit](#business-partner-identifier-amount-limit)
<!-- TOC -->

## 7.2.x to 7.3.x Migration Guide

### 1. Breaking rename of relation DTO fields (Gate)

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

### 2. New relation type for addresses

Gate now exposes a dedicated relation type:

- `IsReplacedBy`

This type applies only to address relations and is validated accordingly.

---

### 3. No required operator actions

- No existing data needs to be changed.
- No cleanup or special deployment steps needed.

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