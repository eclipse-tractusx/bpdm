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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
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
            val entities: String = LegalEntityPartnerCreateResponseWrapperV6::entities.name
            val errors: String = LegalEntityPartnerCreateResponseWrapperV6::errors.name
            val legalEntity: String = "$entities.${LegalEntityPartnerCreateVerboseDtoV6::legalEntity.name}"
            val legalAddress: String = "$entities.${LegalEntityPartnerCreateVerboseDtoV6::legalAddress.name}"
            val legalEntityCreatedAt: String = "${legalEntity}.${LegalEntityVerboseDtoV6::createdAt.name}"
            val legalEntityUpdatedAt: String = "${legalEntity}.${LegalEntityVerboseDtoV6::updatedAt.name}"
            val legalAddressCreatedAt: String = "${legalAddress}.${LegalEntityVerboseDtoV6::createdAt.name}"
            val legalAddressUpdatedAt: String = "${legalAddress}.${LegalEntityVerboseDtoV6::updatedAt.name}"
            val bpnL: String = "${legalEntity}.${LegalEntityVerboseDtoV6::bpnl.name}"
            val currentness: String = "${legalEntity}.${LegalEntityVerboseDtoV6::currentness.name}"
            val bpnA: String = "${legalAddress}.${LogisticAddressVerboseDtoV6::bpna.name}"
            val bpnLegalEntity: String = "${legalAddress}.${LogisticAddressVerboseDtoV6::bpnLegalEntity.name}"
            val errorMessage: String = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object LegalEntityWithLegalAddressVerboseDtoPaths{
            val content = PageDto<*>::content.name
            val legalEntity = "${content}.${LegalEntityWithLegalAddressVerboseDtoV6::legalEntity.name}"
            val legalAddress =  "${content}.${LegalEntityWithLegalAddressVerboseDtoV6::legalAddress.name}"
            val legalEntityCreatedAt =  "${legalEntity}.${LegalEntityVerboseDtoV6::createdAt.name}"
            val legalEntityUpdatedAt =  "${legalEntity}.${LegalEntityVerboseDtoV6::updatedAt.name}"
            val legalAddressCreatedAt =  "${legalAddress}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val legalAddressUpdatedAt =  "${legalAddress}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
        }

        private object LegalEntityGetPaths{
            private val legalEntity = LegalEntityWithLegalAddressVerboseDtoV6::legalEntity.name
            private val  legalAddress = LegalEntityWithLegalAddressVerboseDtoV6::legalAddress.name
            val legalEntityCreatedAt =  "${legalEntity}.${LegalEntityVerboseDtoV6::createdAt.name}"
            val legalEntityUpdatedAt =  "${legalEntity}.${LegalEntityVerboseDtoV6::updatedAt.name}"
            val legalAddressCreatedAt =  "${legalAddress}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val legalAddressUpdatedAt =  "${legalAddress}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
        }

        private object SiteUpsertResponsePaths{
            val entities: String = SitePartnerCreateResponseWrapperV6::entities.name
            val errors: String = SitePartnerCreateResponseWrapperV6::errors.name
            val site: String = "$entities.${SitePartnerCreateVerboseDtoV6::site.name}"
            val mainAddress: String = "$entities.${SitePartnerCreateVerboseDtoV6::mainAddress.name}"
            val siteCreatedAt: String = "${site}.${SiteVerboseDtoV6::createdAt.name}"
            val siteUpdatedAt: String = "${site}.${SiteVerboseDtoV6::updatedAt.name}"
            val mainAddressCreatedAt: String = "${mainAddress}.${SiteVerboseDtoV6::createdAt.name}"
            val mainAddressUpdatedAt: String = "${mainAddress}.${SiteVerboseDtoV6::updatedAt.name}"
            val bpnS: String = "${site}.${SiteVerboseDtoV6::bpns.name}"
            val bpnA: String = "${mainAddress}.${LogisticAddressVerboseDtoV6::bpna.name}"
            val bpnSite: String = "${mainAddress}.${LogisticAddressVerboseDtoV6::bpnSite.name}"
            val errorMessage: String = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object SiteSearchResponsePaths{
            val content = PageDto<*>::content.name
            val site = "${content}.${SiteWithMainAddressVerboseDtoV6::site.name}"
            val mainAddress =  "${content}.${SiteWithMainAddressVerboseDtoV6::mainAddress.name}"
            val siteCreatedAt =  "${site}.${SiteVerboseDtoV6::createdAt.name}"
            val siteUpdatedAt =  "${site}.${SiteVerboseDtoV6::updatedAt.name}"
            val mainAddressCreatedAt =  "${mainAddress}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val mainAddressUpdatedAt =  "${mainAddress}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
        }

        private object SiteGetResponsePaths{
            private val site = SiteWithMainAddressVerboseDtoV6::site.name
            private val mainAddress =  SiteWithMainAddressVerboseDtoV6::mainAddress.name
            val siteCreatedAt =  "${site}.${SiteVerboseDtoV6::createdAt.name}"
            val siteUpdatedAt =  "${site}.${SiteVerboseDtoV6::updatedAt.name}"
            val mainAddressCreatedAt =  "${mainAddress}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val mainAddressUpdatedAt =  "${mainAddress}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
        }

        private object AdditionalAddressUpsertPaths{
            private val entities = AddressPartnerCreateResponseWrapperV6::entities.name
            private val errors =  AddressPartnerCreateResponseWrapperV6::errors.name
            private val address =  "${entities}.${AddressPartnerCreateVerboseDtoV6::address.name}"
            val bpnA = "${address}.${LogisticAddressVerboseDtoV6::bpna.name}"
            val createdAt = "${address}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val updatedAt = "${address}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
            val errorMessage = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object AddressUpdatePaths{
            private val entities = AddressPartnerUpdateResponseWrapperV6::entities.name
            private val errors =  AddressPartnerUpdateResponseWrapperV6::errors.name
            val createdAt = "${entities}.${LogisticAddressVerboseDtoV6::createdAt.name}"
            val updatedAt = "${entities}.${LogisticAddressVerboseDtoV6::updatedAt.name}"
            val errorMessage = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

    }


    fun assertLegalEntityCreate(actual: LegalEntityPartnerCreateResponseWrapperV6, expected: LegalEntityPartnerCreateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntityUpdate(actual: LegalEntityPartnerUpdateResponseWrapperV6, expected: LegalEntityPartnerUpdateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntitySearch(actual: PageDto<LegalEntityWithLegalAddressVerboseDtoV6>, expected: PageDto<LegalEntityWithLegalAddressVerboseDtoV6>) {
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

    fun assertLegalEntityGet(actual: LegalEntityWithLegalAddressVerboseDtoV6, expected: LegalEntityWithLegalAddressVerboseDtoV6) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityGetPaths.legalEntityCreatedAt,
                LegalEntityGetPaths.legalEntityUpdatedAt,
                LegalEntityGetPaths.legalAddressCreatedAt,
                LegalEntityGetPaths.legalAddressUpdatedAt
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteCreate(actual: SitePartnerCreateResponseWrapperV6, expected: SitePartnerCreateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnS,
                SiteUpsertResponsePaths.bpnA,
                SiteUpsertResponsePaths.bpnSite,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteUpdate(actual: SitePartnerUpdateResponseWrapperV6, expected: SitePartnerUpdateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnA,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertLegalAddressSiteCreate(actual: SitePartnerCreateResponseWrapperV6, expected: SitePartnerCreateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnS,
                SiteUpsertResponsePaths.bpnSite,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteSearch(actual: PageDto<SiteWithMainAddressVerboseDtoV6>, expected: PageDto<SiteWithMainAddressVerboseDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteSearchResponsePaths.siteCreatedAt,
                SiteSearchResponsePaths.siteUpdatedAt,
                SiteSearchResponsePaths.mainAddressCreatedAt,
                SiteSearchResponsePaths.mainAddressUpdatedAt,
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteVerbose(actual: PageDto<SiteVerboseDtoV6>, expected: PageDto<SiteVerboseDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                "${PageDto<*>::content.name}.${SiteVerboseDtoV6::createdAt.name}",
                "${PageDto<*>::content.name}.${SiteVerboseDtoV6::updatedAt.name}",
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteGet(actual: SiteWithMainAddressVerboseDtoV6, expected: SiteWithMainAddressVerboseDtoV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteGetResponsePaths.siteCreatedAt,
                SiteGetResponsePaths.siteUpdatedAt,
                SiteGetResponsePaths.mainAddressCreatedAt,
                SiteGetResponsePaths.mainAddressUpdatedAt,
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAdditionalAddressCreate(actual: AddressPartnerCreateResponseWrapperV6, expected: AddressPartnerCreateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                AdditionalAddressUpsertPaths.bpnA,
                AdditionalAddressUpsertPaths.createdAt,
                AdditionalAddressUpsertPaths.updatedAt,
                AdditionalAddressUpsertPaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }
    fun assertAddressUpdate(actual: AddressPartnerUpdateResponseWrapperV6, expected: AddressPartnerUpdateResponseWrapperV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                AddressUpdatePaths.createdAt,
                AddressUpdatePaths.updatedAt,
                AddressUpdatePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAddressSearch(actual: PageDto<LogisticAddressVerboseDtoV6>, expected: PageDto<LogisticAddressVerboseDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                "${PageDto<*>::content.name}.${LogisticAddressVerboseDtoV6::createdAt.name}",
                "${PageDto<*>::content.name}.${LogisticAddressVerboseDtoV6::updatedAt.name}",
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAddressGet(actual: LogisticAddressVerboseDtoV6, expected: LogisticAddressVerboseDtoV6){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                 LogisticAddressVerboseDtoV6::createdAt.name,
                 LogisticAddressVerboseDtoV6::updatedAt.name
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