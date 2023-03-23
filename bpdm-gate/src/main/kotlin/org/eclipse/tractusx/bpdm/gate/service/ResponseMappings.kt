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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.service.SaasMappings.toDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.gate.entity.AddressGate
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

//fun AddressGate.toAddressGateInputRequest(): AddressGateInputRequest {
//
//    return AddressGateInputRequest(
//        AddressGate.toDto(),
//        this.externalId,
//        this.legalEntityExternalId,
//        this.siteExternalId,
//        this.bpn
//    )
//}
//fun AddressGateInputRequest.toAddressGate(): AddressGate {
//
//
//    return AddressGate(
//        this.address,
//        this.externalId,
//        this.legalEntityExternalId,
//        this.siteExternalId,
//        this.bpn
//    )
//}
//
//fun LegalEntityGate.LegalEntityGateInputRequest(): LegalEntityGateInputRequest {
//
//    return LegalEntityGateInputRequest(
//        LegalEntityDto.toDto(),
//        this.bpn,
//    )
//
//}
//
//fun LegalEntityGateInputRequest.toLegalEntityGate(): LegalEntityGate {
//
//    return LegalEntityGate(
//        this.bpn,
//        this.legalEntity,
//        this.externalId,
//    )
//
//}

fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}


