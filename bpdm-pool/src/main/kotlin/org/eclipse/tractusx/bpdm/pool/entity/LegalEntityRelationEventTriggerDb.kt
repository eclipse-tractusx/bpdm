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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb.Companion.EVENT_TYPE_COLUMN
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb.Companion.IS_PROCESSED_COLUMN
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb.Companion.RELATION_ID_COLUMN
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb.Companion.TRIGGER_DATE_COLUMN
import java.time.LocalDate

@Entity
@Table(
    name = "legal_entity_relation_event_triggers",
    indexes = [
        Index(name = "idx_le_relation_event_triggers_relation_event_type", columnList = "$RELATION_ID_COLUMN,$EVENT_TYPE_COLUMN"),
        Index(name = "idx_le_relation_event_triggers_is_processed_trigger_date", columnList = "$IS_PROCESSED_COLUMN,$TRIGGER_DATE_COLUMN"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uc_le_relation_event_type_trigger_date", columnNames = [RELATION_ID_COLUMN, EVENT_TYPE_COLUMN, TRIGGER_DATE_COLUMN])
    ]
)
class LegalEntityRelationEventTriggerDb(
    @Column(name = TRIGGER_DATE_COLUMN, nullable = false)
    var triggerDate: LocalDate,
    @Column(name = IS_PROCESSED_COLUMN, nullable = false)
    var isProcessed: Boolean,
    @Column(name = EVENT_TYPE_COLUMN, nullable = false)
    @Enumerated(EnumType.STRING)
    var eventType: TriggerEventType,
    @ManyToOne
    @JoinColumn(name = RELATION_ID_COLUMN, nullable = false)
    var relation: RelationDb
) : BaseEntity() {
    companion object {
        const val TRIGGER_DATE_COLUMN = "trigger_date"
        const val EVENT_TYPE_COLUMN = "event_type"
        const val RELATION_ID_COLUMN = "relation_id"
        const val IS_PROCESSED_COLUMN = "is_processed"
    }
}

