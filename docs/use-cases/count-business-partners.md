# Count Business Partners

## Problem Description

I as a sharing member want to compare the number of business partners I shared with the total number of business partners in the BPDM Pool.

## Solution

The BPDM APIs offer endpoints to query business partners by type Legal Entity, Site and Address.
These endpoints offer pagination over the whole data sets in respect to the business partner type.
The Pool and the Gate API both offer such endpoints.
The response objects of these pagination endpoints contain next to the data entries also the total number of existing elements.
With this information you easily get the total number legal entities, sites and addresses.
You can add this information up to get the overall total number of business partners.

## Examples

### Count Site Input Data in the Gate

Get all site inputs in a Gate:

```bash
curl -X 'POST' \
  'https://base-url/api/catena/input/sites/search?page=0&size=10' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '[]'
```

Response:

```json
{
  "totalElements": 3000,
  "totalPages": 300,
  "page": 0,
  "contentSize": 10,
  "content": [
    {
      ...
    }
  ]
}
```

The total number of sites in the Gate is in the 'totalElements' field and is 3000.

`Hint: The BPDM Gate offers analogous endpoints for calculating the total number of output data per L/S/A type.`

### Count Sites in the Pool

```bash
curl -X 'GET' \
  'https://base-url/api/catena/sites?page=0&size=10' \
  -H 'accept: application/json'
```

Response:

```json
{
  "totalElements": 12000,
  "totalPages": 1200,
  "page": 0,
  "contentSize": 10,
  "content": [
    {
      ...
    }
  ]
}
```

The total number of sites in the Pool is in the 'totalElements' field and is 12000.

### Count Total Business Partners

Usually, there is no endpoint to directly get the total number of all business partners irrespective of type.
In order to get the total number of all business partners you need add up the total numbers of legal entities, sites and addresses.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm



