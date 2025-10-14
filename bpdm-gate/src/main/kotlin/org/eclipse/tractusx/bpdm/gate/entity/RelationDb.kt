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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb.Companion.COLUMN_EXTERNAL_ID
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb.Companion.COLUMN_TENANT


@Entity
@Table(name = "business_partner_relations",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_external_id_tenant", columnNames = [COLUMN_EXTERNAL_ID, COLUMN_TENANT])
    ]
)
class RelationDb(
    @Column(name = COLUMN_EXTERNAL_ID, unique = true, nullable = false)
    var externalId: String,
    @Column(name = COLUMN_TENANT, nullable = false)
    var tenantBpnL: String,
    @Embedded
    var sharingState: RelationSharingStateDb?,
    @Embedded
    var output: RelationOutputDb?
) : BaseEntity(){
    companion object{
        const val COLUMN_EXTERNAL_ID = "external_id"
        const val COLUMN_TENANT = "tenant_bpnl"
    }
}