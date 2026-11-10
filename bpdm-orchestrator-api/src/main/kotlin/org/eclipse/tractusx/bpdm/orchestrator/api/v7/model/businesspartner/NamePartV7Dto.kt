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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A categorized name part for this business partner data")
data class NamePartV7Dto(
    @Schema(description = "The name part value")
    val name: String,
    @get:Schema(description = "NamePartType:\n" +
            "* `LegalName` - Value is part of the legal entities' legal name\n" +
            "* `ShortName` - Value is part of the legal entities' short name\n" +
            "* `LegalForm` - Value is part of the legal entities' legal form name\n" +
            "* `SiteName` - Value is part of the site's name\n" +
            "* `AddressName` - Value is part of the address name\n")
    val type: NamePartTypeV7
)
