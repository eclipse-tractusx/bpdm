/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.cdq

data class ReferenceDataLookupRequestCdq(
    val matchingThreshold: Float,
    val pageSize: Int,
    val page: Int,
    val maxCandidates: Int,
    val dataSources: Collection<String> = emptyList(),
    val featuresOff: Collection<LookupFeatures> = emptyList(),
    val featuresOn: Collection<LookupFeatures> = emptyList(),
    val businessPartner: BusinessPartnerLookupCdq
)

data class BusinessPartnerLookupCdq(
    val names: Collection<ValueLookupCdq>,
    val identifiers: Collection<IdentifierLookupCdq>,
    val legalForm: ValueLookupCdq?,
    val address: Collection<AddressLookupCdq>
)


data class IdentifierLookupCdq(
    val value: String?,
    val type: TechnicalKeyLookupCdq?
)

data class AddressLookupCdq(
    val administrativeAreas: Collection<ValueLookupCdq>,
    val country: NameLookupCdq?,
    val localities: Collection<ValueLookupCdq>,
    val postCodes: Collection<ValueLookupCdq>,
    val thoroughfares: Collection<ThoroughfareLookupCdq>
)

data class ValueLookupCdq(
    val value: String
)

data class TechnicalKeyLookupCdq(
    val technicalKey: String
)

data class NameLookupCdq(
    val shortName: String
)

data class ThoroughfareLookupCdq(
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
