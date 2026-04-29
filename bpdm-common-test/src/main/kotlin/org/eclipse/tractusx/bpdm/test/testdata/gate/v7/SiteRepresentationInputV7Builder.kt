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
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import java.time.LocalDateTime

class SiteRepresentationInputV7Builder(seed: String) {

    private var siteBpn: String = "BPNS $seed"
    private var name: String = "Site Name $seed"
    private var states: Collection<BusinessPartnerStateDto> = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            validTo = LocalDateTime.of(2025, 1, 1, 0, 0),
            type = BusinessStateType.ACTIVE
        )
    )

    fun withSiteBpn(siteBpn: String) = apply { this.siteBpn = siteBpn }
    fun withName(name: String) = apply { this.name = name }
    fun withStates(states: Collection<BusinessPartnerStateDto>) = apply { this.states = states }

    fun build(): SiteRepresentationInputDto = SiteRepresentationInputDto(
        siteBpn = siteBpn,
        name = name,
        states = states
    )
}
