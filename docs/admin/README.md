# Admin Guide

Here you can find information about operating a running BPDM system.
Please note that configuration and setup of BPDM applications is covered by the [INSTALL](../../INSTALL.md) documentation.

## BPDM Pool

The Pool offers several endpoints that are intended for administration purposes.

### Managing Metadata

Golden records reference metadata information like identifier types, administrative areas and legal forms.
Unlike other data points metadata constitute a given list of available values.
It is for example not allowed for a golden record to refer to arbitrary identifier types.
Instead, a golden record needs to specify the correct technical key of a predefined list of available identifier types.

While most available metadata is established by configuration of database migration scripts it is also possible to add some metadata as an administrator during runtime.

Namely, an administrator can add additional legal forms and identifier types through the Pool API:

- `POST legal-forms`: Create new legal forms
- `POST identifier-types`: Create new identifier type for either legal entities or addresses

---
**NOTE**

While the endpoints to create new legal forms and identifier types currently exist in the Pool API, it is recommended to rely on managing all metadata through database migration scripts.

---

### BPN Request Identifiers

When matching business partner data to existing golden records a refinement service may discover that the business partner data is new and has no BPN yet.
In this case the golden record process intends for the refinement service to assign the business partner an unique BPN request identifier instead.
Once the business partner data reaches the Pool, the Pool will assign that record a new BPN and store the association between BPN and its request identifier.
In order to obtain the information which BPN has been created from the BPN request identifier, the Pool offers an endpoint `POST bpn/request-ids/search`.
This endpoint may be helpful for debugging purposes or to align BPN information for existing refinement services.

### Direct Golden Record Updates

The intended way to create and update golden records is to rely shared business partner information through the golden record process.
An administrator can also manipulate golden record data directly by using the POST and PUT endpoints to create and update legal entities, sites and addresses directly.
The following endpoints are available:

- `POST legal-entities`: Create new legal entities
- `POST sites`: Create new sites for given legal entities by BPNL
- `POST sites/legal-main-sites`: Create new sites for given legal entities whose site main address is the legal address
- `POST addresses`: Create new legal entity or site address given by BPNL or BPNS
- `PUT legal-entities`: Update existing legal entities by BPNL
- `PUT sites`: Update existing sites by BPNS
- `PUT addresses`: Update existing addresses by BPNA

---
**NOTE**

Through the PUT endpoints you are able to change the business partner data.
However, no structural changes are possible.
This means, you are not able to change the parent legal entity of a site.
Likewise, you are not able to change the legal entity or site of an addresses, nor change the address type.
---