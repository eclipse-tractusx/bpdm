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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import java.time.LocalDateTime

class LegalEntityRepresentationInputV7Builder(seed: String) {

    private var legalEntityBpn: String = "BPNL $seed"
    private var legalName: String = "Legal Name $seed"
    private var shortName: String = "Short Name $seed"
    private var legalForm: String = "Legal Form $seed"
    private var states: Collection<BusinessPartnerStateDto> = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            validTo = LocalDateTime.of(2025, 1, 1, 0, 0),
            type = BusinessStateType.ACTIVE
        )
    )

    fun withLegalEntityBpn(legalEntityBpn: String) = apply { this.legalEntityBpn = legalEntityBpn }
    fun withLegalName(legalName: String) = apply { this.legalName = legalName }
    fun withShortName(shortName: String) = apply { this.shortName = shortName }
    fun withLegalForm(legalForm: String) = apply { this.legalForm = legalForm }
    fun withStates(states: Collection<BusinessPartnerStateDto>) = apply { this.states = states }

    fun build(): LegalEntityRepresentationInputDto = LegalEntityRepresentationInputDto(
        legalEntityBpn = legalEntityBpn,
        legalName = legalName,
        shortName = shortName,
        legalForm = legalForm,
        states = states
    )
}
