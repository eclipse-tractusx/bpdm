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

package org.eclipse.tractusx.bpdm.pool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "bpdm.pool-security")
data class PoolSecurityConfigProperties(
    var readPoolPartnerData: String = "read_pool_partner_data",
    var changePoolPartnerData: String = "change_pool_partner_data",
    var readMetaData: String = "read_meta_data",
    var changeMetaData: String = "change_meta_data",
    var manageOpensearch: String = "manage_opensearch"
) {

    fun getReadPoolPartnerDataAsRole(): String {
        return "ROLE_$readPoolPartnerData"
    }

    fun getChangePoolPartnerDataAsRole(): String {
        return "ROLE_$changePoolPartnerData"
    }

    fun getReadMetaDataAsRole(): String {
        return "ROLE_$readMetaData"
    }

    fun getChangeMetaDataAsRole(): String {
        return "ROLE_$changeMetaData"
    }

    fun getManageOpensearchAsRole(): String {
        return "ROLE_$manageOpensearch"
    }
}
