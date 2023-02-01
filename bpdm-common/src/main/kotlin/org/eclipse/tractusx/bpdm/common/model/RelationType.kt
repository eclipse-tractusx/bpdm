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

enum class RelationType(private val typeName: String) : NamedType {
    CX_LEGAL_SUCCESSOR_OF("Start is legal successor of End"),
    CX_LEGAL_PREDECESSOR_OF("Start is legal predecessor of End"),
    CX_ADDRESS_OF("Start is legally registered at End"),
    CX_SITE_OF("Start operates at site of End"),
    CX_OWNED_BY("Start is legally owned by End"),
    DIRECT_LEGAL_RELATION("Start is legally owned by End"),
    COMMERCIAL_ULTIMATE("End is highest commercial organization in hierarchy of Start"),
    DOMESTIC_BRANCH_RELATION("Start is domestic branch of End"),
    INTERNATIONAL_BRANCH_RELATION("Start is international branch of End"),
    DOMESTIC_LEGAL_ULTIMATE_RELATION("End is highest domestic organization in hierarchy of Start"),
    GLOBAL_LEGAL_ULTIMATE_RELATION("End is globally highest organization in hierarchy of Start"),
    LEGAL_PREDECESSOR("Start is legal predecessor of End"),
    LEGAL_SUCCESSOR("Start is legal successor of End"),
    DNB_PARENT("Start legally owns End"),
    DNB_HEADQUARTER("Start is legal headquarter of End"),
    DNB_DOMESTIC_ULTIMATE("End is highest domestic organization in hierarchy of Start"),
    DNB_GLOBAL_ULTIMATE("End is globally highest organization in hierarchy of Start"),
    LEI_DIRECT_PARENT("Start legally owns End"),
    LEI_INTERNATIONAL_BRANCH("Start is international branch of End"),
    LEI_ULTIMATE_PARENT("End is globally highest organization in hierarchy of Start");

    override fun getTypeName(): String {
        return typeName
    }
}