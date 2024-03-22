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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.LocalDateTime


@Entity
@Table(name = "sharing_states")
class SharingStateDb(

    @Column(name = "external_id", nullable = false)
    var externalId: String,

    @Column(name = "associated_owner_bpnl", nullable = true)
    var associatedOwnerBpnl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_state_type", nullable = false)
    var sharingStateType: SharingStateType,

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_error_code", nullable = true)
    var sharingErrorCode: BusinessPartnerSharingError?,

    @Column(name = "sharing_error_message", nullable = true)
    var sharingErrorMessage: String? = null,

    @Column(name = "sharing_process_started", nullable = true)
    var sharingProcessStarted: LocalDateTime? = null,

    @Column(name = "task_id", nullable = true)
    var taskId: String? = null

) : BaseEntity()



