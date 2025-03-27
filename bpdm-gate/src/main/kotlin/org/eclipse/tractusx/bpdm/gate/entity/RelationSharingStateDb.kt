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

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import java.time.Instant

@Embeddable
data class RelationSharingStateDb (
    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_state_type", nullable = true)
    var sharingStateType: RelationSharingStateType,

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_error_code", nullable = true)
    var sharingErrorCode: RelationSharingStateErrorCode?,

    @Column(name = "sharing_error_message", nullable = true)
    var sharingErrorMessage: String?,

    @Column(name = "sharing_state_updated_at", nullable = true)
    var updatedAt: Instant
)