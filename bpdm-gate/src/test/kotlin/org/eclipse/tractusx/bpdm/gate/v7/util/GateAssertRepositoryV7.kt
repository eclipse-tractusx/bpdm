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

package org.eclipse.tractusx.bpdm.gate.v7.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import java.time.Instant
import java.time.LocalDateTime

class GateAssertRepositoryV7(
    private val instantSecondsComparator: InstantSecondsComparator,
    private val localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {

    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputDto>, expected: Collection<BusinessPartnerInputDto>) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison()
            .ignoringFields(
                BusinessPartnerInputDto::createdAt.name,
                BusinessPartnerInputDto::updatedAt.name
            )
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputDto>, expected: PageDto<BusinessPartnerInputDto>) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }

    fun assertBusinessPartnerOutput(actual: Collection<BusinessPartnerOutputDto>, expected: Collection<BusinessPartnerOutputDto>) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison()
            .ignoringFields(
                BusinessPartnerOutputDto::createdAt.name,
                BusinessPartnerOutputDto::updatedAt.name
            )
            .withComparatorForType(instantSecondsComparator, Instant::class.java)
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerOutput(actual: PageDto<BusinessPartnerOutputDto>, expected: PageDto<BusinessPartnerOutputDto>) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerOutput(actual.content, expected.content)
    }

    fun assertSharingStates(actual: PageDto<SharingStateDto>, expected: PageDto<SharingStateDto>) {
        assertPageHeader(actual, expected)
        assertSharingStates(actual.content, expected.content)
    }

    fun assertSharingStates(actual: Collection<SharingStateDto>, expected: Collection<SharingStateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                SharingStateDto::updatedAt.name,
                SharingStateDto::sharingProcessStarted.name
            )
            .isEqualTo(expected)
    }

    fun assertSharingStateStats(actual: StatsSharingStatesResponse, expected: StatsSharingStatesResponse) {
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    fun assertRelation(actual: RelationDto, expected: RelationDto) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: Collection<RelationDto>, expected: Collection<RelationDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: PageDto<RelationDto>, expected: PageDto<RelationDto>) {
        assertPageHeader(actual, expected)
        assertRelations(actual.content, expected.content)
    }

    fun assertRelationOutput(actual: Collection<RelationOutputDto>, expected: Collection<RelationOutputDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(RelationOutputDto::updatedAt.name)
            .isEqualTo(expected)
    }

    fun assertRelationOutput(actual: PageDto<RelationOutputDto>, expected: PageDto<RelationOutputDto>) {
        assertPageHeader(actual, expected)
        assertRelationOutput(actual.content, expected.content)
    }

    fun assertRelationPageMetadata(actual: PageDto<RelationDto>, totalElements: Long, totalPages: Int, page: Int, contentSize: Int) {
        Assertions.assertThat(actual.totalElements).isEqualTo(totalElements)
        Assertions.assertThat(actual.totalPages).isEqualTo(totalPages)
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.contentSize).isEqualTo(contentSize)
    }

    fun assertChangelog(actual: PageChangeLogDto<ChangelogGateDto>, expected: PageChangeLogDto<ChangelogGateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageChangeLogDto<*>::content.name)
            .isEqualTo(expected)

        Assertions.assertThat(actual.content)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(ChangelogGateDto::timestamp.name)
            .isEqualTo(expected.content)
    }

    private fun assertPageHeader(actual: PageDto<*>, expected: PageDto<*>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageDto<*>::content.name)
            .isEqualTo(expected)
    }

    private fun BusinessPartnerInputDto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            legalEntity = legalEntity.sortContent(),
            site = site.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun SiteRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun AddressRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun BusinessPartnerOutputDto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            legalEntity = legalEntity.sortContent(),
            site = site?.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationOutputDto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )

    private fun SiteRepresentationOutputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun AddressComponentOutputDto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            identifiers = identifiers.sortedBy { it.value },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )
}