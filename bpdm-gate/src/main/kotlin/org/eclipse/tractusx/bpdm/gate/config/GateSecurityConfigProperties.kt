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

package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "bpdm.gate-security")
data class GateSecurityConfigProperties(
    var oauth2ClientRegistration: String = "gate-client",
    var poolSecurityEnabled: Boolean = false,
    var readCompanyInputData: String = "read_company_input_data",
    var changeCompanyInputData: String = "change_company_input_data",
    var readCompanyOutputData: String = "read_company_output_data",
    var changeCompanyOutputData: String = "change_company_output_data"
){

    fun getReadCompanyInputDataAsRole(): String {
        return "ROLE_$readCompanyInputData"
    }

    fun getChangeCompanyInputDataAsRole(): String {
        return "ROLE_$changeCompanyInputData"
    }

    fun getReadCompanyOutputDataAsRole(): String {
        return "ROLE_$readCompanyOutputData"
    }

    fun getChangeCompanyOutputDataAsRole(): String {
        return "ROLE_$changeCompanyOutputData"
    }
}
