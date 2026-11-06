/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.raw.participation

import org.eclipse.tractusx.bpdm.common.logging.PersistedChange
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeSubject
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType

data class SharingMemberRecordChangeRaw(
    override val type: PersistedChangeType,
    val recordId: String,
    val addressBpn: String,
    val aspect: Aspect
) : PersistedChange {

    override val subject = SUBJECT

    override fun identifierForLog() = "$recordId@$addressBpn"

    override fun aspectForLog() = aspect.logName

    enum class Aspect(val logName: String) {
        GoldenRecordCounted("golden record counted")
    }

    private companion object {
        val SUBJECT = PersistedChangeSubject("sharing member record", "sharing member records")
    }
}
