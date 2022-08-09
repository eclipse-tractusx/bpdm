/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

CREATE INDEX idx_0640fc16905367702847f4e50 ON business_partners (legal_form_id);

CREATE INDEX idx_27a3d3e2dc744e624f86c42ac ON thoroughfares (address_id);

CREATE INDEX idx_27e28cee3b3c196d04d92dd16 ON localities (address_id);

CREATE INDEX idx_40c1cd3586bff0d8838b5a42f ON classifications (partner_id);

CREATE INDEX idx_4e7e988fa6c498489ca428ebe ON identifiers (type_id);

CREATE INDEX idx_532f0f3cd0fcc87a073133ebe ON addresses (version_id);

CREATE INDEX idx_68d0ddefca3d78782f306205a ON names (partner_id);

CREATE INDEX idx_68e587d8695feeb07ae0e4306 ON relations (start_node_id);

CREATE INDEX idx_6b9d462c775e3411dd2dfef5c ON administrative_areas (address_id);

CREATE INDEX idx_7773e8f689fcaef3f45fe0255 ON addresses (partner_id);

CREATE INDEX idx_7cd3dbc21ea393a2e3f579844 ON premises (address_id);

CREATE INDEX idx_8f40df2ce03f76bc9923a683a ON bank_accounts (partner_id);

CREATE INDEX idx_94ce64d22fe3bd5d4658efdfa ON post_codes (address_id);

CREATE INDEX idx_9de08b456309ac30a77546592 ON identifiers (partner_id);

CREATE INDEX idx_a91204c0e84c71629e3639ba7 ON relations (end_node_id);

CREATE INDEX idx_aec173db9d054e39c515f547d ON identifiers (issuing_body_id);

CREATE INDEX idx_d5b8f6e8e692c8f2a9f5fec48 ON identifiers (status);

CREATE INDEX idx_e645e8b157b7bbaed58397144 ON business_stati (partner_id);

CREATE INDEX idx_f8a9c5185fbb7e54bdd96edb8 ON postal_delivery_points (address_id);

CREATE INDEX idx_address_contexts_fk ON address_contexts (address_id);

CREATE INDEX idx_address_types_fk ON address_types (address_id);

CREATE INDEX idx_bank_account_trust_scores_fk ON bank_account_trust_scores (account_id);

CREATE INDEX idx_business_partner_types ON bank_account_trust_scores (account_id);

CREATE INDEX idx_legal_forms_legal_categories_fk ON legal_forms_legal_categories (form_id);