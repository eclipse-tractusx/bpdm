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

@Schema(description = "Business partner data in full representation, consisting of generic data as well as its L/S/A representation.")
data class BusinessPartnerFull(
    @get:Schema(description = "The business partner data in generic representation", required = true)
    val generic: BusinessPartnerGeneric,
    @get:Schema(description = "The legal entity part of this business partner data")
    val legalEntity: LegalEntity? = null,
    @get:Schema(description = "The site part of this business partner data")
    val site: Site? = null,
    @get:Schema(description = "The address part of this business partner data")
    val address: LogisticAddress? = null
)
