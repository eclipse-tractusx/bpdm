# Adoption View

Here you can find documentation on how to access and integrate BPDM APIs.
The main user groups for BPDM are sharing members, cleaning service providers and VAS providers.

This document contains explanations for different use cases for these user groups.
Accompanied by these explanations is a [Postman collection](postman/BPDM%20Tests.postman_collection.json) showcasing these use cases.
Please mind that the requests in this Postman collection are not meant to be executed for automated tests but rather serve as documentation.

## Sharing Member View

A sharing member wants to share business partner data from their own system to the golden record process in order to receive golden record information.

In context of a sharing member, a business partner is a logistic address augmented by additional meta information like names, identifiers and state of
operation.
The business partner data may contain legal entity and/or site information.
For example, the business partner's name may include the name of the legal entity the logistic address belongs to.
For this reason, sharing member business partners are assigned up to three BPNs during the golden record process.
A BPNA refers to the actual logistic address contained in the business partner data.
A BPNL refers to the legal entity that logistic address belongs to.
An optional BPNS exists if the logistic address also belongs to a site of that legal entity.

> Please note that a sharing member business partner is not a golden record even after going through the golden record process.
> It merely points to golden records via the attached BPNs.

In addition to the BPN important information the sharing member receives is the address type of a business partner's logistic address.
The logistic address may be the legal address of the legal entity, the main address of it's site or just an additional address.

The main point of access for a sharing member is the BPDM Gate API.

### Sharing a new business partner

Postman collection request ID: S.CP

The Gate API is divided by two stages: An input and an output stage.
Each stage holds a sharing member's business partner data.
The general idea for sharing business partner data is

1. for a sharing member to upsert business partner data to the input stage
2. set the data to be sharing ready and
3. wait until the golden record process writes the result to the output stage.

On uploading a business partner to the input stage, the sharing member assigns an `externalId` which he can use to identify the business partner across stages.
In fact the `externalId` is the main identifier for querying business partner data in the Gate.

Except the `externalId` there are no required properties for a business partner.
However, if the provided data is too few or of too bad quality the business partner might be rejected by the golden record process.
Querying the sharing state of the business partner will reveal the current state in the golden record process.
Here, the sharing member can also see whether and why the business partner has been rejected.

#### Remarks for the Cleaning Service Dummy

A golden record process which is implemented by using the BPDM Cleaning Service Dummy has some unique restrictions.

- Duplication check:
  The cleaning service dummy determines two business partners referring to the same golden record if the golden records' names are identical.
- Address Type:
  The cleaning service dummy always categorizes the business partner's logistic address as an additional address.
  If you need the output to have a certain address type you must set the type yourself in the input.
  The cleaning service dummy will not override a set address type.

### Sharing a Business Partner With a Site

Postman collection request ID: S.CPS

Currently, the golden record process can not determine on its own whether a logistic address belongs to a site or not.
It is not able to extract site information on its own.

Additionally, the golden record process will only consider site information from business partners who are shared by the owners.
A sharing member is an owner of a business partner if the logistic address is part of the sharing member's company.

For these two reasons in order to share business partner data with site information, a sharing member needs to claim the business partner data by
setting `isOwnCompanyData` to true.
Also, for the golden record process to recognise the site information, the sharing member needs to set the `siteName` field.

If both conditions in the business partner data are fulfilled and the ownership claim is validated by the golden record process, a site golden record will be
created as part of the result.

#### Remarks for the Cleaning Service Dummy

If the golden record process is implemented with the Cleaning Service Dummy every ownership claim will be validated to true.

### Update a Business Partner

Postman collection request ID: S.UP

After a business partner has been created sharing members can use the same approach as when sharing a business partners initially.

Please be aware that when a sharing member updates a business partner and sends it to the golden record process, the output may have different BPNs from before.
This is because the process may determine that the business partner in fact describes different golden records than previously assumed.
The previous golden records remain in the Pool.

Sharing members may only update business partner input if the data is really different.
If the upserted input data is identical to the existing input data, neither a changelog entry is created nor can the data be given to the golden record process.

## Cleaning Service Provider View

A cleaning service provider has responsibility for implementing a specific part of the golden record process like duplication check and/or enriching data.

## VAS Provider View

A VAS provider uses golden records to provide an value-added service to the Catena-X community.

## Access BPDM over EDC

This section details how a sharing member EDC can access an EDC exposing the BPDM API as assets. Before you can access the assets make sure that the BPDM EDC
has been configured to provide assets for your company's BPN (see [Operator View](OPERATOR_VIEW.md)).
This [POSTMAN collection](postman/EDC_BPDM_Usage.postman_collection.json) gives example requests for communicating with a deployed BPDM EDC over a sharing
member EDC.
In general, in order to access API endpoints you need to accept the contract of the corresponding asset first, then require a temporary access token for the
asset and finally send that access token over your own EDC to the BPDM EDC in order to receive the endpoint's response. The following list details these steps:

1. Retrieve asset information you want to access:
   Your own EDC offers endpoints to query the BPDM EDC's catalogue of assets. There you can find a list of assets, their IDs and descriptions. Also, you find
   information under which contract this asset is offered to be made available to you. The following steps require the respective asset and contract ID to
   proceed.

2. Start a negotiation for the asset:
   When you have selected an asset you need to accept its contract. You will typically access the asset over your BPN. This request is asynchronous. You can
   check the request status over the respective GET method with the negotiation ID you received from starting the contract negotiation request. Once the
   negotiation is done and accepted, you will receive an agreement ID in the GET response. You can use this agreement ID together with the asset ID to
   request access tokens for future data transfers. Please be aware that the negotiation agreement is typically temporary and needs to be renewed
   periodically.

3. Start an access token transfer:
   Once you have an agreement ID for accessing one or more assets you can now request to receive data access tokens. When requesting a token you need to provide
   a URL to your service which should receive that token. Please be aware that the transfer request is asynchronous: The sharing member EDC will request a token
   from the BPDM EDC and then send this token directly to the backend service whose URL you provided. You neither receive this token as a direct response to the
   request
   nor is there any way to inquire the token from the EDC after the fact.

4. Invoke the BPDM API endpoint:
   With the EDC's data access token you can now access the actual BPDM API endpoint. The gate EDC provides a URL that acts as a proxy for this
   purpose. You send a request to that URL with the data access token in the headers and attach query parameters as well as body in the same way you would
   invoke BPDM API endpoint directly. The gate EDC's URL can be invoked with the method's GET, POST and PUT. Use the method that matches original BPDM
   API endpoint the accessed asset covers. The response equals the original BPDM API endpoint response.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm