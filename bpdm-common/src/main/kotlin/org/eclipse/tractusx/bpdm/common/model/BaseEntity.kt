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

/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpdm_sequence")
    @SequenceGenerator(name = "bpdm_sequence", sequenceName = "bpdm_sequence", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false, insertable = false)
    val id: Long = 0,

    @Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(updatable = false, nullable = false, name = "CREATED_AT")
    @CreationTimestamp
    val createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS),

    @Column(nullable = false, name = "UPDATED_AT")
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
)