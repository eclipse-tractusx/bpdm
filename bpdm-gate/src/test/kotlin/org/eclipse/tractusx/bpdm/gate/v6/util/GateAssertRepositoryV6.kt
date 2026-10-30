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
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.BusinessPartnerOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.SharingStateV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.changelog.ChangelogGateV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.changelog.PageChangeLogV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.error.GateErrorV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsSharingStatesV6Response
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


    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputV6Dto>, expected: Collection<BusinessPartnerInputV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                BusinessPartnerInputV6Dto::createdAt.name,
                BusinessPartnerInputV6Dto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputV6Dto>, expected: PageDto<BusinessPartnerInputV6Dto>){
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }


    fun assertBusinessPartnerOutput(actual: Collection<BusinessPartnerOutputV6Dto>, expected: Collection<BusinessPartnerOutputV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                BusinessPartnerOutputV6Dto::createdAt.name,
                BusinessPartnerOutputV6Dto::updatedAt.name
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertBusinessPartnerOutput(actual: PageDto<BusinessPartnerOutputV6Dto>, expected: PageDto<BusinessPartnerOutputV6Dto>){
        assertPageHeader(actual, expected)
        assertBusinessPartnerOutput(actual.content, expected.content)
    }

    fun assertSharingStates(actual: PageDto<SharingStateV6Dto>, expected: PageDto<SharingStateV6Dto>){
        assertPageHeader(actual, expected)
        assertSharingStates(actual.content, expected.content)
    }

    fun assertSharingStates(actual: Collection<SharingStateV6Dto>, expected: Collection<SharingStateV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(SharingStateV6Dto::sharingProcessStarted.name)
            .isEqualTo(expected)
    }

    fun assertSharingStateStats(actual: StatsSharingStatesV6Response, expected: StatsSharingStatesV6Response){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    fun assertChangelog(actual: PageChangeLogV6Dto<ChangelogGateV6Dto>, expected: PageChangeLogV6Dto<ChangelogGateV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                PageDto<*>::content.name
            )
            .isEqualTo(expected)

        Assertions.assertThat(actual.content)
            .usingRecursiveComparison()
            .ignoringFields(
                ChangelogGateV6Dto::timestamp.name
            )
            .isEqualTo(expected.content)
    }

    fun assertRelation(actual: RelationV6Dto, expected: RelationV6Dto){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationV6Dto::createdAt.name,
                RelationV6Dto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: PageDto<RelationV6Dto>, expected: PageDto<RelationV6Dto>){
        assertPageHeader(actual, expected)
        assertRelations(actual.content, expected.content)
    }

    fun assertRelations(actual: Collection<RelationV6Dto>, expected: Collection<RelationV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationV6Dto::createdAt.name,
                RelationV6Dto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertErrorPerExternalId(actual: GateErrorV6Response, externalIds: Collection<String>) {
        Assertions.assertThat(actual.error).hasSameSizeAs(externalIds)
        externalIds.forEach { externalId ->
            Assertions.assertThat(actual.error).anySatisfy { Assertions.assertThat(it).contains("'$externalId'") }
        }
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