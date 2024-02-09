# BPDM Pool

The BPDM Pool is the single source of truth in Eclipse Tractus-X for business partner base information such as addresses and official identifiers.
Each record in the Pool has a unique identifier with which it can be referenced across the entire Eclipse Tractus-X landscape, the business partner number.
Business partner records are divided into legal entities, sites and partner addresses.
Self-explanatory, a legal entity record represents the legal entity information about a business partner.
A site may represent legal entity's plant or campus which is big enough to contain several contact/delivery addresses.
Finally, an address partner is a location of legal entity or site with a single contact/delivery address.
A legal entity may have several sites and address partner.
Further, a site may have several address partners.

The Pool offers an [API](../bpdm-pool-api) to query these business partner records by BPN, other identifier or by text search.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
