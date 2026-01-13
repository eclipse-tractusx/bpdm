# Migration Guide

<!-- TOC -->
* [Migration Guide](#migration-guide)
  * [7.2.x to 7.3.x](#72x-to-73x)
    * [Postgres Version Update](#postgres-version-update)
  * [7.1.x to 7.2.x](#71x-to-72x)
    * [Alternative Headquarters Restriction](#alternative-headquarters-restriction)
    * [Default Logging Level](#default-logging-level)
  * [7.0.x to 7.1.x](#70x-to-71x)
    * [EDC Version 0.11](#edc-version-011)
    * [Golden Record Process for IsManagedBy Relations](#golden-record-process-for-ismanagedby-relations)
    * [Business Partner Identifier Amount Limit](#business-partner-identifier-amount-limit)
<!-- TOC -->

## 7.2.x to 7.3.x

### Postgres Version Update

The new version has been updated with Postgres version 18.0 instead of the formerly used version 15.x. With this change there was also the switch from the no longer maintained Bitnami image and helm chart to helm charts provided by Cloud Pirate which make use of the standard Postgres image published on Docker Hub.

As a consequence, for a Kubernetes setup done with the provided helm charts, there is no possibility to do an automatic upgrade with the provided helm charts, as the two images are not supporting that. Instead, during update, a operator has to do the following manual steps;

- Backup PostgreSQL data from the old installation 
- Uninstall the old Helm release 
- Delete the old PVC 
- Perform a fresh installation with the new chart 
- Restore data

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