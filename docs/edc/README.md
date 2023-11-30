# Eclipse Data Connector

This documentation contains information on how to expose and use the BPDM API over
the [Eclipse Data Connector](https://github.com/eclipse-tractusx/tractusx-edc).

## EDC Provider

This section details how to configure an EDC that provides access to the BPDM API for a company to share and query business partner data. This documentation
assumes that you already have running BPDM and EDC deployments. How to deploy BPDM refer to the [INSTALL.md](../../INSTALL.md). For deploying an EDC please
consult the documentation on the [EDC repository](https://github.com/eclipse-tractusx/tractusx-edc).

The general idea for using the EDC as a provider for BPDM data is to expose the BPDM Gate Endpoints each as an EDC asset. A new asset needs to be created for
each company and endpoint. As an example you can refer to the provided [POSTMAN collection](EDC_BPDM_Setup.postman_collection.json). The collection shows
examples on how to create endpoints as assets, a company policy and contract definition.

1. Asset Creation:

   ```json
   {
       "@context": {},
       "asset": {
           "@type": "Asset",
           "@id": "{{ASSET_GET_BPL_INPUT}}",
           "properties": {
               "description": "GET Legal Entity Input ({{BPDM_GATE_URL}}/ui/swagger-ui/index.html#/legal-entity-controller/getLegalEntities)",
               "company": "{{COMPANY_ID}}"
           }
       },
       "dataAddress": {
           "@type": "DataAddress",
           "type": "HttpData",
           "baseUrl": "{{BPDM_GATE_URL}}/api/catena/input/legal-entities",
           "oauth2:tokenUrl": "{{ASSET_TOKEN_URL}}",
           "oauth2:clientId": "{{ASSET_CLIENT_ID}}",
           "oauth2:clientSecretKey": "{{ASSET_CLIENT_SECRET}}",
           "proxyQueryParams": "true"
       }
   }
   ```
   In the BPDM view an EDC asset is a company-scoped endpoint address. Next to the id and description, the asset should have a company id property which is
   handy to quickly identify bundles of assets in case your EDC wants to expose assets for several BPDM Gate APIs at once. In addition to the address the asset
   should contain information on how the EDC can
   authenticate against the BPDM API. Finally, some endpoints may expect query parameters and/or bodies to be provided when accessing the asset. As a result,
   the assets should contain which of these additional resources are allowed as well as the actual method type of the endpoint.

2. Policy Creation:

   ```json
      {
       "@context": {
           "odrl": "http://www.w3.org/ns/odrl/2/"
       },
       "@type": "PolicyDefinitionRequestDto",
       "@id": "{{POLICY_ID}}",
       "policy": {
           "@type": "Policy",
           "odrl:permission" : [{
               "odrl:action" : "USE",
               "odrl:constraint" : {
                   "@type": "LogicalConstraint",
                   "odrl:or" : [{
                       "@type" : "Constraint",
                       "odrl:leftOperand" : "BusinessPartnerNumber",
                       "odrl:operator" : {
                           "@id": "odrl:eq"
                       },
                       "odrl:rightOperand" : "{{POLICY_BPN}}"
                   }]
               }
           }]
       }
      }
   ``` 

   For each company that should be allowed to access the BPDM Gate the EDC needs to contain a policy that requires a consuming EDC to have the company's BPN.

3. Contract Definition Creation:

   ```json
   {
    "@context": {},
    "@id": "COMPANY_TEST_SHARING_MEMBER",
    "@type": "ContractDefinition",
    "accessPolicyId": "{{ACCESS_POLICY_ID}}",
    "contractPolicyId": "{{CONTRACT_POLICY_ID}}",
    "assetsSelector": {
        "@type": "CriterionDto",
        "operandLeft": "{{EDC_NAMESPACE}}id",
        "operator": "in",
        "operandRight": [
            "{{ASSET_GET_SHARING_STATE}}",
            "{{ASSET_GET_BPL_INPUT}}",
            "{{ASSET_PUT_BPL_INPUT}}",
            "{{ASSET_POST_BPL_INPUT_SEARCH}}",
            "{{ASSET_GET_BPS_INPUT}}",
            "{{ASSET_PUT_BPS_INPUT}}",
            "{{ASSET_POST_BPS_INPUT_SEARCH}}",
            "{{ASSET_GET_BPA_INPUT}}",
            "{{ASSET_PUT_BPA_INPUT}}",
            "{{ASSET_POST_BPA_INPUT_SEARCH}}",
            "{{ASSET_POST_BPL_OUTPUT_SEARCH}}",
            "{{ASSET_POST_BPS_OUTPUT_SEARCH}}",
            "{{ASSET_POST_BPA_OUTPUT_SEARCH}}",
            "{{ASSET_POST_INPUT_CHANGELOG_SEARCH}}",
            "{{ASSET_POST_OUTPUT_CHANGELOG_SEARCH}}",
            "{{ASSET_GET_LEGAL_FORMS}}",
            "{{ASSET_GET_IDENTIFIER_TYPES}}",
            "{{ASSET_GET_FIELD_QUALITY_RULES}}",
            "{{ASSET_GET_BPL_POOL}}",
            "{{ASSET_POST_BPL_POOL_SEARCH}}",
            "{{ASSET_GET_BPS_POOL}}",
            "{{ASSET_POST_BPS_POOL_SEARCH}}",
            "{{ASSET_GET_BPA_POOL}}",
            "{{ASSET_POST_BPA_POOL_SEARCH}}",
            "{{ASSET_POST_CHANGELOG_POOL_SEARCH}}"
        ]
    }
   }
   ```  
   For each company the EDC should have a contract definition assigning the company's access policy to the company's assets. The company's assets are identified
   by the ids of the assets.

After all assets, policies and contract definitions are configured a sharing company's EDC now can query its available assets and the contract under which they
are exposed.

## EDC Consumer

This section details how a sharing member EDC can access an EDC exposing the BPDM API as assets. Before you can access the assets make sure that the BPDM EDC
has been configured to provide assets for your company's BPN (see [previous sesction](#edc-provider)).
This [POSTMAN collection](EDC_BPDM_Usage.postman_collection.json) gives example requests for communicating with a deployed BPDM EDC over a sharing member EDC.
In general, in order to access API endpoints you need to accept the contract of the corresponding asset first, then require a temporary access token for the
asset and finally send that access token over your own EDC to the BPDM EDC in order to receive the endpoint's response. The following list details these steps:

1. Retrieve asset information you want to access:

   ```json
    {
       "@context": {},
       "protocol": "dataspace-protocol-http",
       "providerUrl": "{{PROVIDER_PROTOCOL_URL}}",
       "querySpec": {
           "offset": 0,
           "limit": 100,
           "filter": "",
           "range": {
               "from": 0,
               "to": 100
           },
           "criterion": ""
       }
   }
   ```
   Your own EDC offers endpoints to query the BPDM EDC's catalogue of assets. There you can find a list of assets, their IDs and descriptions. Also, you find
   information under which contract this asset is offered to be made available to you. The following steps require the respective asset and contract ID to
   proceed.

2. Start a negotiation for the asset:

   ```json
   {
	  "@context": {
		  "odrl": "http://www.w3.org/ns/odrl/2/"
	  },
	  "@type": "NegotiationInitiateRequestDto",
	  "connectorAddress": "{{PROVIDER_PROTOCOL_URL}}",
	  "protocol": "dataspace-protocol-http",
	  "connectorId": "{{PROVIDER_ID}}",
	  "providerId": "{{PROVIDER_ID}}",
	  "offer": {
		  "offerId": "{{CONTRACT_DEFINITION_ID}}:{{ASSET_ID}}:ZDM4Nzk3NmUtZjA0Ny00ZmNjLWFhNWItYjQwYmVkMDBhZGYy1",
		  "assetId": "{{ASSET_ID}}",
		  "policy": {
			  "@type": "odrl:Set",
			  "odrl:permission": {
				"odrl:target": "{{ASSET_ID}}",
				"odrl:action": {
					"odrl:type": "USE"
				},
				"odrl:constraint": {
					"odrl:or": {
						"odrl:leftOperand": "BusinessPartnerNumber",
						"odrl:operator": {
                            "@id": "odrl:eq"
                        },
						"odrl:rightOperand": "{{POLICY_BPN}}"
					}
				}
			},
	      "odrl:prohibition": [],
	      "odrl:obligation": [],
          "odrl:target": "{{ASSET_ID}}"
          }
      }
   }
   ``` 
   When you have selected an asset you need to accept its contract. You will typically access the asset over your BPN. This request is asynchronous. You can
   check the request status over the respective GET method with the negotiation ID you received from starting the contract negotiation request. Once the
   negotiation is done and accepted, you will receive an agreement ID in the GET response. You can use this agreement ID together with the asset ID to
   request access tokens for future data transfers. Please be aware that the negotiation agreement is typically temporary and needs to be renewed
   periodically.

3. Start an access token transfer:

    ```json
    {
          "@context": {
              "odrl": "http://www.w3.org/ns/odrl/2/"
          },
          "assetId": "{{ASSET_ID}}",
          "connectorAddress": "{{PROVIDER_PROTOCOL_URL}}",
          "connectorId": "{{PROVIDER_ID}}",
          "contractId": "{{AGREEMENT_ID}}",
          "dataDestination": {
              "type": "HttpProxy"
          },
          "managedResources": false,
          "privateProperties": {
              "receiverHttpEndpoint": "{{BACKEND_SERVICE}}"
          },
          "protocol": "dataspace-protocol-http",
          "transferType": {
              "contentType": "application/octet-stream",
              "isFinite": true
          }
   }
   ```  
   Once you have an agreement ID for accessing one or more assets you can now request to receive data access tokens. When requesting a token you need to provide
   a URL to your service which should receive that token. Please be aware that the transfer request is asynchronous: The sharing member EDC will request a token
   from the BPDM EDC and then send this token directly to the backend service whose URL you provided. You neither receive this token as a direct response to the
   request
   nor is there any way to inquire the token from the EDC after the fact.

4. Invoke the BPDM API endpoint:

   ```bash
   curl https://gate-edc-host/api/public
   ```
   With the EDC's data access token you can now access the actual BPDM API endpoint. The gate EDC provides a URL that acts as a proxy for this
   purpose. You send a request to that URL with the data access token in the headers and attach query parameters as well as body in the same way you would
   invoke BPDM API endpoint directly. The gate EDC's URL can be invoked with the method's GET, POST and PUT. Use the method that matches original BPDM
   API endpoint the accessed asset covers. The response equals the original BPDM API endpoint response.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm


