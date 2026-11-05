/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner

/**
 * Business partner search naming a non-empty BPN or a legal name of the minimum length, neither starting with
 * whitespace, and including at least one kind of business partner.
 */
data class BusinessPartnerSearchParsed(
    val bpn: String?,
    val legalName: String?,
    val streetName: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
    val includesLegalEntities: Boolean,
    val includesSites: Boolean,
    val includesAdditionalAddresses: Boolean
)
