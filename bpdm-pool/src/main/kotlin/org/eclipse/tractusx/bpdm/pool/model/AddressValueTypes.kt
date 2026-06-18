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

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import java.time.Instant

/**
 * Bounded address value types that carry no stage-specific metadata, so they are shared across the loose request
 * ([LogisticAddressRequest]) and resolved ([LogisticAddressParsed]) stages without per-stage variants.
 */

data class Street(
    val name: String? = null,
    val houseNumber: String? = null,
    val houseNumberSupplement: String? = null,
    val milestone: String? = null,
    val direction: String? = null,
    val namePrefix: String? = null,
    val additionalNamePrefix: String? = null,
    val nameSuffix: String? = null,
    val additionalNameSuffix: String? = null
)

data class AddressState(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType
)

data class GeoCoordinate(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double? = null
)
