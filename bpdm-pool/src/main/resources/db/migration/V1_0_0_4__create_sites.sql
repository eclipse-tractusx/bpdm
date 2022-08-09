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

create table sites
(
    id         bigint       not null,
    bpn        varchar(255) not null,
    created_at timestamp    not null,
    updated_at timestamp    not null,
    uuid       uuid         not null,
    name       varchar(255) not null,
    partner_id bigint       not null,
    primary key (id)
);

alter table sites
    add constraint uc_sites_bpn unique (bpn);

alter table sites
    add constraint uc_sites_uuid unique (uuid);

alter table sites
    add constraint fk_sites_on_business_partners foreign key (partner_id) references business_partners;

alter table addresses
    add site_id bigint;

alter table addresses
    alter column partner_id drop not null;

alter table addresses
    add constraint fk_addresses_on_sites foreign key (site_id) references sites;

alter table addresses
    add bpn varchar(255) not null;

alter table addresses
    add constraint uc_addresses_bpn unique (bpn);

update configuration_entries
set key='bpn-l-counter'
where key = 'bpn-counter'
