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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import java.time.LocalDateTime

/**
 * Offers util functionalities for comparing complex API V6 objects under test
 */
class AssertRepositoryV6(
    private val localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {
    companion object{
        private object LegalEntityUpsertResponsePaths{
            val entities: String = LegalEntityPartnerCreateResponseWrapper::entities.name
            val errors: String = LegalEntityPartnerCreateResponseWrapper::errors.name
            val legalEntity: String = "$entities.${LegalEntityPartnerCreateVerboseDto::legalEntity.name}"
            val legalAddress: String = "$entities.${LegalEntityPartnerCreateVerboseDto::legalAddress.name}"
            val legalEntityCreatedAt: String = "${legalEntity}.${LegalEntityVerboseDto::createdAt.name}"
            val legalEntityUpdatedAt: String = "${legalEntity}.${LegalEntityVerboseDto::updatedAt.name}"
            val legalAddressCreatedAt: String = "${legalAddress}.${LegalEntityVerboseDto::createdAt.name}"
            val legalAddressUpdatedAt: String = "${legalAddress}.${LegalEntityVerboseDto::updatedAt.name}"
            val bpnL: String = "${legalEntity}.${LegalEntityVerboseDto::bpnl.name}"
            val currentness: String = "${legalEntity}.${LegalEntityVerboseDto::currentness.name}"
            val bpnA: String = "${legalAddress}.${LogisticAddressVerboseDto::bpna.name}"
            val bpnLegalEntity: String = "${legalAddress}.${LogisticAddressVerboseDto::bpnLegalEntity.name}"
            val errorMessage: String = "${errors}.${ErrorInfo<*>::message.name}"
        }

        private object LegalEntityWithLegalAddressVerboseDtoPaths{
            val content = PageDto<*>::content.name
            val legalEntity = "${content}.${LegalEntityWithLegalAddressVerboseDto::legalEntity.name}"
            val legalAddress =  "${content}.${LegalEntityWithLegalAddressVerboseDto::legalAddress.name}"
            val legalEntityCreatedAt =  "${legalEntity}.${LegalEntityVerboseDto::createdAt.name}"
            val legalEntityUpdatedAt =  "${legalEntity}.${LegalEntityVerboseDto::updatedAt.name}"
            val legalAddressCreatedAt =  "${legalAddress}.${LogisticAddressVerboseDto::createdAt.name}"
            val legalAddressUpdatedAt =  "${legalAddress}.${LogisticAddressVerboseDto::updatedAt.name}"
        }

    }


    fun assertLegalEntityCreate(actual: LegalEntityPartnerCreateResponseWrapper, expected: LegalEntityPartnerCreateResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntityUpdate(actual: LegalEntityPartnerUpdateResponseWrapper, expected: LegalEntityPartnerUpdateResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntitySearch(actual: PageDto<LegalEntityWithLegalAddressVerboseDto>, expected: PageDto<LegalEntityWithLegalAddressVerboseDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityWithLegalAddressVerboseDtoPaths.legalEntityCreatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalEntityUpdatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalAddressCreatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalAddressUpdatedAt
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    private fun ObjectAssert<*>.usingRecursiveLegalEntityUpsertComparison(): RecursiveComparisonAssert<*>{
        return this.usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityUpsertResponsePaths.legalEntityCreatedAt,
                LegalEntityUpsertResponsePaths.legalEntityUpdatedAt,
                LegalEntityUpsertResponsePaths.legalAddressCreatedAt,
                LegalEntityUpsertResponsePaths.legalAddressUpdatedAt,
                LegalEntityUpsertResponsePaths.currentness,
                LegalEntityUpsertResponsePaths.bpnL,
                LegalEntityUpsertResponsePaths.bpnA,
                LegalEntityUpsertResponsePaths.bpnLegalEntity,
                LegalEntityUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
    }
}