/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.common.dto.saas

data class ReferenceDataLookupRequestSaas(
    val matchingThreshold: Float,
    val pageSize: Int,
    val page: Int,
    val maxCandidates: Int,
    val dataSources: Collection<String> = emptyList(),
    val featuresOff: Collection<LookupFeatures> = emptyList(),
    val featuresOn: Collection<LookupFeatures> = emptyList(),
    val businessPartner: BusinessPartnerLookupSaas
)

data class BusinessPartnerLookupSaas(
    val names: Collection<ValueLookupSaas>,
    val identifiers: Collection<IdentifierLookupSaas>,
    val legalForm: ValueLookupSaas?,
    val address: Collection<AddressLookupSaas>
)


data class IdentifierLookupSaas(
    val value: String?,
    val type: TechnicalKeyLookupSaas?
)

data class AddressLookupSaas(
    val administrativeAreas: Collection<ValueLookupSaas>,
    val country: NameLookupSaas?,
    val localities: Collection<ValueLookupSaas>,
    val postCodes: Collection<ValueLookupSaas>,
    val thoroughfares: Collection<ThoroughfareLookupSaas>
)

data class ValueLookupSaas(
    val value: String
)

data class TechnicalKeyLookupSaas(
    val technicalKey: String
)

data class NameLookupSaas(
    val shortName: String
)

data class ThoroughfareLookupSaas(
    val number: String?,
    val value: String?
)

enum class LookupFeatures {
    ACTIVATE_IDENTIFIER_ONLY_MATCH,
    ACTIVATE_DATASOURCE_CDL,
    ACTIVATE_DATASOURCE_CDQ_POOL,
    ACTIVATE_DATASOURCE_BZST,
    ACTIVATE_DATASOURCE_VIES_FOR_ES,
    ACTIVATE_DATASOURCE_GOOGLEPLACES,
    ACTIVATE_DATASOURCE_CDQ_CRAWLED,
    ACTIVATE_DATASOURCE_STORAGES,
    ACTIVATE_DATASOURCE_DNB,
    ACTIVATE_DATASOURCE_NLBR,
    ACTIVATE_DATASOURCE_ZEFIX,
    CURATE_REQUEST,
    ENABLE_IDENTIFIER_DERIVATION,
    FORCE_EXTERNAL_CALL,
    MATCHING_SCORE_CALCULATION,
    SHOW_DEBUG_INFO,
    SHOW_FORMATTED_ADDRESS,
    SHOW_FORMATTED_SAP_RECORD,
    SHOW_GOLDENRECORD_STANDARD,
    SHOW_GOLDENRECORD_QUICK,
    GOLDENRECORD_INLINE,
    SHOW_INCOMPLETE_CANDIDATES,
    SHOW_REGISTERED_INDIVIDUAL,
    SHOW_SUBSCRIPTION_METADATA
}
