# Eclipse Data Connector

This documentation contains information on how to expose and use the BPDM API over
the [Eclipse Data Connector](https://github.com/eclipse-tractusx/tractusx-edc).

## EDC Provider

This section details how to configure an EDC that provides access to the BPDM API for a company to share and query business partner data. This documentation
assumes that you already have running BPDM and EDC deployments. How to deploy BPDM refer to the [INSTALL.md](../../INSTALL.md). For deploying an EDC please
consult the documentation on the [EDC repository](https://github.com/eclipse-tractusx/tractusx-edc).

The geneneral idea for using the EDC as a provider for BPDM data is to expose the BPDM Gate Endpoints each as an EDC asset. A new asset needs to be created for
each company and endpoint. As an example you can refer to the provided [POSTMAN collection](EDC_BPDM_Setup.postman_collection.json). The collection shows
examples on how to create endpoints as assets, a company policy and contract definition.

1. Asset Creation:

   ```json
   {
    "asset": {
        "properties": {
            "asset:prop:id": "GET_BPL_INPUT_{{COMPANY_ID}}",
            "asset:prop:description": "GET Legal Entity Input ({{BPDM_GATE_URL}}/ui/swagger-ui/index.html#/legal-entity-controller/getLegalEntities)",
            "asset:prop:company": "{{COMPANY_ID}}"
        }
    },
    "dataAddress": {
        "properties": {
            "type": "HttpData",
            "baseUrl": "{{BPDM_GATE_URL}}/api/catena/input/legal-entities",
            "oauth2:tokenUrl": "{{ASSET_TOKEN_URL}}",
            "oauth2:clientId": "{{ASSET_CLIENT_ID}}",
            "oauth2:clientSecret": "{{ASSET_CLIENT_SECRET}}",
            "proxyQueryParams": "true"
        }
    }
   }
   ```
   In the BPDM view an EDC asset is a company-scoped endpoint address. Next to the id and description, the asset should have a company id property which is
   handy to bundle assets in a contract definition (see point 3). In addition to the address the asset should contain information on how the EDC can
   authenticate against the BPDM API. Finally, some endpoints may expect query parameters and/or bodies to be provided when accessing the asset. As a result,
   the assets should contain which of these additional resources are allowed as well as the actual method type of the endpoint.

2. Policy Creation:

   ```json
   {
       "id": "BPN_ACCESS_{{COMPANY_ID}}",
       "policy": {
           "prohibitions": [],
           "obligations": [],
           "permissions": [
               {
                   "edctype": "dataspaceconnector:permission",
                   "action": {
                       "type": "USE"
                   },
                   "constraints": [
                       {
                           "edctype": "AtomicConstraint",
                           "leftExpression": {
                               "edctype": "dataspaceconnector:literalexpression",
                               "value": "BusinessPartnerNumber"
                           },
                           "rightExpression": {
                               "edctype": "dataspaceconnector:literalexpression",
                               "value": "{{POLICY_BPN}}"
                           },
                           "operator": "EQ"
                       }
                   ]
               }
           ]
       }
   }
   ``` 
   For each company that should be allowed to access the BPDM Gate the EDC needs to contain a policy that requires a consuming EDC to have the company's BPN.

3. Contract Definition Creation:

   ```json
   {
       "id": "BPN_ACCESS_{{COMPANY_ID}}",
       "criteria": [
           {
               "operandLeft": "asset:prop:company",
               "operator": "=",
               "operandRight": "{{COMPANY_ID}}"
           }
       ],
       "accessPolicyId": "BPN_ACCESS_{{COMPANY_ID}}",
       "contractPolicyId": "BPN_ACCESS_{{COMPANY_ID}}"
   }
   ```  
   For each company the EDC should have a contract definition assigning the company's access policy to the company's assets. The company's assets are identified
   by the 'company-id' property. The actual company id can be assigned freely but should be unique between company assets.

After all assets, policies and contract definitions are configured a sharing company's EDC now can query its available assets and the contract under which they
are exposed.

## EDC Consumer

This section details how a sharing member EDC can access an EDC exposing the BPDM API as assets. Before you can access the assets make sure that the BPDM EDC
has been configured to provide assets for your company's BPN (see [previous sesction](#edc-provider)).
This [POSTMAN collection](EDC_BPDM_Usage.postman_collection.json) gives example requests for communicating with a deployed BPDM EDC over a sharing member EDC.
In general, in order to access API endpoints you need to accept the contract of the corresponding asset first, then require a temporary access token for the
asset and finally send that access token over your own EDC to the BPDM EDC in order to receive the endpoint's response. The following list details these steps:

1. Retrieve asset information you want to access:

   ```bash
   curl {{SHARING_MEMBER_EDC_URL}}/data/catalog?providerUrl={{GATE_EDC_URL}}/api/v1/ids/data&size=50
   ```
   Your own EDC offers endpoints to query the BPDM EDC's catalogue of assets. There you can find a list of assets, their IDs and descriptions. Also, you find
   information under which contract this asset is offered to be made available to you. The following steps require the respective asset and contract ID to
   proceed.

   2. Start a negotiation for the asset:

      ```json
      {
          "connectorId": "foo",
          "connectorAddress": "{{GATE_EDC_URL}}/api/v1/ids/data",
          "offer": {
              "offerId": "{{CONTRACT_ID}}:foo",
              "assetId": "{{ASSET_ID}}",
              "policy": {
                  "prohibitions": [],
                  "obligations": [],
                  "permissions": [
                      {
                          "edctype": "dataspaceconnector:permission",
                          "action": {
                              "type": "USE"
                          },
                          "target": "{{ASSET_ID}}",
                          "constraints": [
                              {
                                  "edctype": "AtomicConstraint",
                                  "leftExpression": {
                                      "edctype": "dataspaceconnector:literalexpression",
                                      "value": "BusinessPartnerNumber"
                                  },
                                  "rightExpression": {
                                      "edctype": "dataspaceconnector:literalexpression",
                                      "value": "{{SHARING_MEMBER_BPN}}"
                                  },
                                  "operator": "EQ"
                              }
                          ]
                      }
                  ]
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
       "id": "{{TRANSFER_ID}}",
       "connectorId": "txdc-catalog",
       "connectorAddress": "{{GATE_EDC_URL}}/api/v1/ids/data",
       "contractId": "{{AGREEMENT_ID}}",
       "assetId": "{{ASSET_ID}}",
       "managedResources": "false",
       "dataDestination": {
           "type": "HttpProxy"
       },
       "properties": {
           "receiver.http.endpoint": "{{BACKEND_EDC_URL}}"
      }
   }
   ```  
   Once you have an agreement ID for accessing one or more assets you can now request to receive data access tokens. When requesting a token you need to provide
   a URL to your service which should receive that token. Please be aware that the transfer request is asynchronous: The sharing member EDC will request a token
   from the BPDM EDC and then send this token directly to the service whose URL you provided. You neither receive this token as a direct response to the request
   nor is there any way to inquire the token from the EDC after the fact.

4. Invoke the BPDM API endpoint:

   ```bash
   curl {{SHARING_MEMBER_EDC_URL}}/api/public
   ```
   With the EDC's data access token you can now access the actual BPDM API endpoint. The sharing member EDC provides a URL that acts as a proxy for this
   purpose. You send a request to that URL with the data access token in the headers and attach query parameters as well as body in the same way you would
   invoke BPDM API endpoint directly. The sharing member EDC's URL can be invoked with the method's GET, POST and PUT. Use the method that matches original BPDM
   API endpoint the accessed asset covers. The response equals the original BPDM API endpoint response.

