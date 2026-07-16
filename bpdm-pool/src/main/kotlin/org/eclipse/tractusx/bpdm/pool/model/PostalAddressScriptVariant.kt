/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model

/**
 * Shared script-variant address content, reused as-is across the loose request ([org.eclipse.tractusx.bpdm.pool.model.request.AddressScriptVariant])
 * and resolved ([org.eclipse.tractusx.bpdm.pool.model.parsed.AddressScriptVariantParsed]) stages: it carries no stage-specific metadata.
 */
data class PostalAddressScriptVariant(
    val addressName: String? = null,
    val physicalAddress: PhysicalAddressScriptVariant = PhysicalAddressScriptVariant(),
    val alternativeAddress: AlternativeAddressScriptVariant? = null
)

data class PhysicalAddressScriptVariant(
    val postalCode: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: Street? = null,
    val companyPostalCode: String? = null,
    val industrialZone: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val door: String? = null,
    val taxJurisdictionCode: String? = null
)

data class AlternativeAddressScriptVariant(
    val postalCode: String? = null,
    val city: String? = null,
    val deliveryServiceQualifier: String? = null,
    val deliveryServiceNumber: String? = null
)
