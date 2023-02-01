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

package org.eclipse.tractusx.bpdm.common.dto.cdq

data class AddressCdq(
    val id: String? = "0",
    val externalId: String? = null,
    val cdqId: String? = null,
    val version: AddressVersionCdq? = null,
    val identifyingName: WrappedValueCdq? = null,
    val careOf: WrappedValueCdq? = null,
    val contexts: Collection<WrappedValueCdq> = emptyList(),
    val country: CountryCdq? = null,
    val administrativeAreas: Collection<AdministrativeAreaCdq> = emptyList(),
    val postCodes: Collection<PostCodeCdq> = emptyList(),
    val localities: Collection<LocalityCdq> = emptyList(),
    val thoroughfares: Collection<ThoroughfareCdq> = emptyList(),
    val premises: Collection<PremiseCdq> = emptyList(),
    val postalDeliveryPoints: Collection<PostalDeliveryPointCdq> = emptyList(),
    val geographicCoordinates: GeoCoordinatesCdq? = null,
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val metadataCdq: AddressMetadataCdq? = null
)
