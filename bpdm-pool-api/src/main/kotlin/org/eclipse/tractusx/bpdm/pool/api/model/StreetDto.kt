/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IBaseStreetDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.StreetDescription

@Schema(description = StreetDescription.header)
data class StreetDto(

    override val name: String? = null,
    override val houseNumber: String? = null,
    override val houseNumberSupplement: String? = null,
    override val milestone: String? = null,
    override val direction: String? = null,
    override val namePrefix: String? = null,
    override val additionalNamePrefix: String? = null,
    override val nameSuffix: String? = null,
    override val additionalNameSuffix: String? = null,

    ) : IBaseStreetDto
