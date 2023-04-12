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

import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityStateDto
import org.eclipse.tractusx.bpdm.common.dto.NameDto
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.temporal.ChronoUnit


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}


fun LegalEntityGateInputRequest.toLegalEntity():LegalEntity{

    val legalEntity= LegalEntity(
        bpn = bpn.toString(),
        externalId= externalId,
        currentness = createCurrentnessTimestamp(),
        legalForm = legalEntity.legalForm,
        legalName = legalEntity.legalName.toName()
    )
    legalEntity.identifiers.addAll( this.legalEntity.identifiers.map {toEntityIdentifier(it,legalEntity)})
    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it,legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it,legalEntity) })




    return legalEntity;

}
fun toEntityIdentifier(dto: LegalEntityIdentifierDto, legalEntity: LegalEntity): LegalEntityIdentifier {
    return LegalEntityIdentifier(dto.value, dto.type,dto.issuingBody, legalEntity)
}
fun toEntityState(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
    return LegalEntityState(dto.officialDenotation,dto.validFrom,dto.validTo,dto.type,legalEntity);
}
fun toEntityClassification(dto: ClassificationDto, legalEntity: LegalEntity): Classification {
    return Classification(dto.value,dto.code,dto.type,legalEntity);
}
fun NameDto.toName(): Name{
    return Name(value, shortName)
}
private fun createCurrentnessTimestamp(): Instant {
    return Instant.now().truncatedTo(ChronoUnit.MICROS)
}