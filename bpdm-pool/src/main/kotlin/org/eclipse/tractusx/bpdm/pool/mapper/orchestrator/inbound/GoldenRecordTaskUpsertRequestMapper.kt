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


package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.AdditionalSite as TaskAdditionalSite
import org.eclipse.tractusx.orchestrator.api.model.BpnReference as TaskBpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType as TaskBpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as TaskLegalEntity
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants as TaskAddress
import org.eclipse.tractusx.orchestrator.api.model.Site as TaskSite
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto as TaskEntry

/**
 * Maps a reserved golden record task into the loose [GoldenRecordUpsertRequest], delegating each partner's content to
 * the mapper that owns it.
 *
 * The task model states several things by omission, and each becomes a shape here: a site without a main address is a
 * [SiteUpsertRequest.WithLegalAddressAsMain], and `hasChanged` is the [UpsertIntent] it actually expresses - it
 * suppresses an update, never a create. What the task states only implicitly and cannot be decided before its
 * references are resolved - create against update, a membership entry naming an existing site against a new one - is
 * left to the parser.
 */
@Component
class GoldenRecordTaskUpsertRequestMapper(
    private val legalEntityRequestMapper: GoldenRecordTaskLegalEntityRequestMapper,
    private val siteRequestMapper: GoldenRecordTaskSiteRequestMapper,
    private val addressRequestMapper: GoldenRecordTaskAddressRequestMapper
) {

    /** The upsert the task asks for, with every partner addressed by the reference the task gave it. */
    fun toRequest(taskEntry: TaskEntry): GoldenRecordUpsertRequest {
        val businessPartner = taskEntry.businessPartner
        return GoldenRecordUpsertRequest(
            sharingMemberRecordId = taskEntry.recordId,
            legalEntity = toLegalEntityRequest(businessPartner.legalEntity),
            site = businessPartner.site?.let { toSiteRequest(it) },
            additionalAddress = businessPartner.additionalAddress?.let { toAddressRequest(it) },
            addressSiteMembership = businessPartner.additionalSites.map { toSiteReferenceRequest(it) }
        )
    }

    /** The Pool-side form of a task's BPN reference. */
    fun toReference(reference: TaskBpnReference): BpnReferenceRequest =
        BpnReferenceRequest(
            value = reference.referenceValue,
            type = reference.referenceType?.let { toReferenceKind(it) }
        )

    private fun toLegalEntityRequest(legalEntity: TaskLegalEntity): LegalEntityUpsertRequest =
        LegalEntityUpsertRequest(
            reference = toReference(legalEntity.bpnReference),
            header = legalEntityRequestMapper.toHeaderRequest(legalEntity),
            legalAddress = AddressUpsertRequest(
                reference = toReference(legalEntity.legalAddress.bpnReference),
                content = legalEntityRequestMapper.toLegalAddressRequest(legalEntity)
            ),
            intent = toIntent(legalEntity.hasChanged)
        )

    private fun toSiteRequest(site: TaskSite): SiteUpsertRequest {
        val mainAddress = site.siteMainAddress
            ?: return SiteUpsertRequest.WithLegalAddressAsMain(
                reference = toReference(site.bpnReference),
                header = siteRequestMapper.toHeaderRequest(site),
                intent = toIntent(site.hasChanged)
            )

        return SiteUpsertRequest.WithOwnMainAddress(
            reference = toReference(site.bpnReference),
            header = siteRequestMapper.toHeaderRequest(site),
            intent = toIntent(site.hasChanged),
            mainAddress = AddressUpsertRequest(
                reference = toReference(mainAddress.bpnReference),
                content = siteRequestMapper.toMainAddressRequest(site, mainAddress)
            )
        )
    }

    private fun toAddressRequest(address: TaskAddress): AddressUpsertRequest =
        AddressUpsertRequest(
            reference = toReference(address.bpnReference),
            content = addressRequestMapper.toContentRequest(address)
        )

    private fun toSiteReferenceRequest(additionalSite: TaskAdditionalSite): SiteReferenceRequest =
        SiteReferenceRequest(reference = toReference(additionalSite.bpnReference), name = additionalSite.siteName)

    private fun toIntent(hasChanged: Boolean?): UpsertIntent =
        if (hasChanged == false) UpsertIntent.WriteOnlyIfAbsent else UpsertIntent.AlwaysWrite

    private fun toReferenceKind(referenceType: TaskBpnReferenceType): BpnReferenceKind =
        when (referenceType) {
            TaskBpnReferenceType.Bpn -> BpnReferenceKind.Bpn
            TaskBpnReferenceType.BpnRequestIdentifier -> BpnReferenceKind.RequestIdentifier
        }
}
