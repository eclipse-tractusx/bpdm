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

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.JpaMermaidCreator
import org.eclipse.tractusx.bpdm.common.util.JpaMetaModelReader
import org.springframework.stereotype.Service

/**
 * Service to generate documentation
 */
@Service
class DocumentationService(
    val entityManager: EntityManager
) {

    private val logger = KotlinLogging.logger { }

    fun getMermaidGatePersistence(): String {
        logger.info { "Executing getMermaidGatePersistence()" }
        val allClassInfos = JpaMetaModelReader().createJpaClassInfos(this.entityManager.metamodel)
        return JpaMermaidCreator().getMermaid(allClassInfos, "Gate persistence")
    }

}