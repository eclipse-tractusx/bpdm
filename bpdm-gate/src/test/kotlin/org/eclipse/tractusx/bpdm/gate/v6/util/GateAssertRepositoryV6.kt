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

package org.eclipse.tractusx.bpdm.gate.v6.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Offers util functionalities for comparing complex Gate API V6 objects under test
 */
@Component
class GateAssertRepositoryV6 {

    private val localDatetimeSecondsComparator = LocalDatetimeSecondsComparator(InstantSecondsComparator())


    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputDto>, expected: Collection<BusinessPartnerInputDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                BusinessPartnerInputDto::createdAt.name,
                BusinessPartnerInputDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputDto>, expected: PageDto<BusinessPartnerInputDto>){
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }


    fun assertBusinessPartnerOutput(actual: Collection<BusinessPartnerOutputDto>, expected: Collection<BusinessPartnerOutputDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                BusinessPartnerOutputDto::createdAt.name,
                BusinessPartnerOutputDto::updatedAt.name
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertBusinessPartnerOutput(actual: PageDto<BusinessPartnerOutputDto>, expected: PageDto<BusinessPartnerOutputDto>){
        assertPageHeader(actual, expected)
        assertBusinessPartnerOutput(actual.content, expected.content)
    }

    fun assertSharingStates(actual: PageDto<SharingStateDto>, expected: PageDto<SharingStateDto>){
        assertPageHeader(actual, expected)
        assertSharingStates(actual.content, expected.content)
    }

    fun assertSharingStates(actual: Collection<SharingStateDto>, expected: Collection<SharingStateDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(SharingStateDto::sharingProcessStarted.name)
            .isEqualTo(expected)
    }

    fun assertSharingStateStats(actual: StatsSharingStatesResponse, expected: StatsSharingStatesResponse){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    fun assertChangelog(actual: PageChangeLogDto<ChangelogGateDto>, expected: PageChangeLogDto<ChangelogGateDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                PageDto<*>::content.name
            )
            .isEqualTo(expected)

        Assertions.assertThat(actual.content)
            .usingRecursiveComparison()
            .ignoringFields(
                ChangelogGateDto::timestamp.name
            )
            .isEqualTo(expected.content)
    }

    fun assertRelation(actual: RelationDto, expected: RelationDto){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: PageDto<RelationDto>, expected: PageDto<RelationDto>){
        assertPageHeader(actual, expected)
        assertRelations(actual.content, expected.content)
    }

    fun assertRelations(actual: Collection<RelationDto>, expected: Collection<RelationDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    private fun assertPageHeader(actual: PageDto<*>, expected: PageDto<*>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                PageDto<*>::content.name
            )
            .isEqualTo(expected)
    }

}