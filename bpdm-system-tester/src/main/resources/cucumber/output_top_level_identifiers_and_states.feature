################################################################################
# Copyright (c) 2026 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
################################################################################

# A Gate output record carries two top-level fields - identifiers and states - that do NOT simply mirror
# the input. Instead they surface the identifiers and states of the golden record entity the record was
# refined to, and which entity that is depends on what the record reflects:
#
#   - legal entity record    -> the LEGAL ENTITY's identifiers and states
#   - site record            -> NO identifiers at all, and the SITE's states
#   - additional address     -> the ADDRESS's identifiers and states
#
# This is a peculiar, easy-to-miss rule (note especially that a site record surfaces no top-level
# identifiers), so it gets its own feature rather than being folded into the master data assertions. The
# master data features ("output_reflects_own_shared_master_data.feature" and
# "output_reflects_golden_record_master_data_changes.feature") deliberately IGNORE identifiers and states;
# this feature is their counterpart and asserts ONLY the top-level identifiers and states, ignoring the
# descriptive master data.
@CXTPM-1093
Feature: Output Top-Level Identifiers And States Reflect The Refined Entity

  #h3. Test Objective:
  #
  #* Verify a record refined to a legal entity surfaces the legal entity's identifiers and states as top-level fields.
  #
  #h3. Description:
  #
  #* The sharing member shares a record.
  #* The golden record process refines it to a legal entity.
  #* The record's top-level identifiers and states reflect the legal entity.
  @TEST_CXTPM-1034 @BPDM
  Scenario: Legal Entity Record Surfaces The Legal Entity's Identifiers And States
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output top-level identifiers and states reflect legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a record refined to a site surfaces no top-level identifiers and surfaces the site's states.
  #
  #h3. Description:
  #
  #* The sharing member shares a record.
  #* The golden record process refines it to a site of a legal entity.
  #* The record's output has no top-level identifiers and its states reflect the site.
  @TEST_CXTPM-1033 @BPDM
  Scenario: Site Record Surfaces No Identifiers And The Site's States
    When the sharing member shares record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme" with master data "acme-site-content"
    Then "acme-site-record" output has no top-level identifiers and its states reflect site "acme-site" of legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a record refined to an additional address of a legal entity surfaces the address's identifiers and states as top-level fields.
  #
  #h3. Description:
  #
  #* The sharing member shares a record.
  #* The golden record process refines it to an additional address of a legal entity.
  #* The record's top-level identifiers and states reflect the additional address.
  @TEST_CXTPM-1032 @BPDM
  Scenario: Additional Address Of Legal Entity Record Surfaces The Address's Identifiers And States
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    Then "acme-address-record" output top-level identifiers and states reflect additional address "acme-branch" of legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a record refined to an additional address of a site surfaces the address's identifiers and states as top-level fields.
  #
  #h3. Description:
  #
  #* The sharing member shares a record.
  #* The golden record process refines it to an additional address of a site.
  #* The record's top-level identifiers and states reflect the additional address.
  @TEST_CXTPM-1031 @BPDM
  Scenario: Additional Address Of Site Record Surfaces The Address's Identifiers And States
    When the sharing member shares record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-content"
    Then "acme-site-address-record" output top-level identifiers and states reflect additional address "acme-dock" of site "acme-site" of legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a state without validity dates is reported from the earliest to the latest date, since the output always carries both dates.
  #
  #h3. Description:
  #
  #* The sharing member shares a record.
  #* The golden record process refines it to a legal entity whose single state has neither a start nor an end date.
  #* The record's output reports that state from the earliest to the latest date.
  @TEST_CXTPM-1090 @BPDM
  Scenario: Legal Entity State Without Dates Reports The Earliest And Latest Date
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with a state without validity dates
    Then "acme-record" output reports the state from "1900-01-01T00:00:00Z" to "9999-12-31T23:59:59Z"
