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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressResponse

open class EntitiesWithErrors<ENTITY, out ERROR : ErrorCode>(

    @Schema(description = "Successfully created entities")
    open val entities: Collection<ENTITY>,
    @Schema(description = "Errors for not created entities")
    open val errors: Collection<ErrorInfo<ERROR>>
) {
    val entityCount: Int
        get() = entities.size
    val errorCount: Int
        get() = errors.size
}

@Schema(
    name = "LegalEntityCreateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class LegalEntityPartnerCreateResponseWrapper(
    override val entities: Collection<LegalEntityPartnerCreateResponse>,
    override val errors: Collection<ErrorInfo<LegalEntityCreateError>>
) : EntitiesWithErrors<LegalEntityPartnerCreateResponse, LegalEntityCreateError>(entities, errors)

@Schema(
    name = "LegalEntityUpdateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class LegalEntityPartnerUpdateResponseWrapper(
    override val entities: Collection<LegalEntityPartnerCreateResponse>,
    override val errors: Collection<ErrorInfo<LegalEntityUpdateError>>
) : EntitiesWithErrors<LegalEntityPartnerCreateResponse, LegalEntityUpdateError>(entities, errors)

@Schema(
    name = "SiteCreateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class SitePartnerCreateResponseWrapper(
    override val entities: Collection<SitePartnerCreateResponse>,
    override val errors: Collection<ErrorInfo<SiteCreateError>>
) : EntitiesWithErrors<SitePartnerCreateResponse, SiteCreateError>(entities, errors)

@Schema(
    name = "SiteUpdateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class SitePartnerUpdateResponseWrapper(
    override val entities: Collection<SitePartnerCreateResponse>,
    override val errors: Collection<ErrorInfo<SiteUpdateError>>
) : EntitiesWithErrors<SitePartnerCreateResponse, SiteUpdateError>(entities, errors)

@Schema(
    name = "AddressCreateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class AddressPartnerCreateResponseWrapper(
    override val entities: Collection<AddressPartnerCreateResponse>,
    override val errors: Collection<ErrorInfo<AddressCreateError>>
) : EntitiesWithErrors<AddressPartnerCreateResponse, AddressCreateError>(entities, errors)

@Schema(
    name = "AddressUpdateWrapper",
    description = "Holds information about successfully and failed entities after the creating/updating of several objects"
)
data class AddressPartnerUpdateResponseWrapper(
    override val entities: Collection<LogisticAddressResponse>,
    override val errors: Collection<ErrorInfo<AddressUpdateError>>
) : EntitiesWithErrors<LogisticAddressResponse, AddressUpdateError>(entities, errors)



