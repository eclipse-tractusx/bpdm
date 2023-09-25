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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.*


data class BusinessPartnerGeneric(
    override val nameParts: List<String> = emptyList(),
    override val shortName: String? = null,
    override val identifiers: Collection<BusinessPartnerIdentifierDto> = emptyList(),
    override val legalForm: String? = null,
    override val states: Collection<BusinessPartnerStateDto> = emptyList(),
    override val classifications: Collection<ClassificationDto> = emptyList(),
    override val roles: Collection<BusinessPartnerRole> = emptyList(),
    override val postalAddress: PostalAddressDto = PostalAddressDto(),
    override val bpnL: String? = null,
    override val bpnS: String? = null,
    override val bpnA: String? = null,
    @get:Schema(description = "The BPNL of the company sharing and claiming this business partner as its own")
    val ownerBpnL: String? = null
) : IBaseBusinessPartnerDto
