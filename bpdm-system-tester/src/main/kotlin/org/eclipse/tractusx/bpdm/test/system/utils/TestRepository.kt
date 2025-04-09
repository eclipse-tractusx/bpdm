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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared test context between all scenarios
 */
@Component
class TestRepository {
    /**
     * Task-IDs are on-the-fly by the golden record process.
     * Additionally, as a refinement service we can't reserve a specific task for processing from the golden record process.
     * Therefore, we just reserve everything and store it for later use by the Cucumber steps here
     */
    val reservedTasksById: ConcurrentHashMap<String, TaskRelationsStepReservationEntryDto> = ConcurrentHashMap()

}