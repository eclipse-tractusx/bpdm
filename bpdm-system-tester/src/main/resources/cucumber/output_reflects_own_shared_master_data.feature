# This feature covers master data changes that originate from the record's OWN sharing: the sharing
# member shares a record (or updates it) and the record's output then reflects the golden record master
# data produced for that record. The companion feature
# "output_reflects_golden_record_master_data_changes.feature" covers the other direction, where the
# golden record master data changes without this record being touched, because a different record is
# refined into the same golden record.
#
# "master data" here means the descriptive legal entity, site and address attributes:
# legal name, short name, legal form, site name, address name, address type and postal addresses.
# It deliberately excludes identifiers, states, BPNs, confidence criteria and golden record
# relations, which are covered by dedicated tests.
@CXTPM-1039
Feature: Output Reflects Own Shared Master Data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the legal entity master data produced for it.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to a legal entity with master data.
  ## The record's output reflects the legal entity master data.
  @TEST_CXTPM-1012 @BPDM
  Scenario: Legal Entity Master Data In Output
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated legal entity master data after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects a legal entity with its master data.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same legal entity with new master data.
  ## The record's output reflects the updated legal entity master data.
  @TEST_CXTPM-1017 @BPDM
  Scenario: Updated Legal Entity Master Data In Output
    Given record "acme-record" reflects legal entity "acme" with master data "acme-content"
    When the sharing member updates record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-updated-content"
    Then "acme-record" output reflects legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the site master data produced for it.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to a site of a legal entity with master data.
  ## The record's output reflects the site master data.
  @TEST_CXTPM-1013 @BPDM
  Scenario: Site Master Data In Output
    When the sharing member shares record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme" with master data "acme-site-content"
    Then "acme-site-record" output reflects site "acme-site" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated site master data after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects a site of a legal entity with its master data.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same site with new master data.
  ## The record's output reflects the updated site master data.
  @TEST_CXTPM-1014 @BPDM
  Scenario: Updated Site Master Data In Output
    Given record "acme-site-record" reflects site "acme-site" of legal entity "acme" with master data "acme-site-content"
    When the sharing member updates record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme" with master data "acme-site-updated-content"
    Then "acme-site-record" output reflects site "acme-site" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the additional address master data produced for it.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to an additional address of a legal entity with master data.
  ## The record's output reflects the additional address master data.
  @TEST_CXTPM-1019 @BPDM
  Scenario: Additional Address Of Legal Entity Master Data In Output
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    Then "acme-address-record" output reflects additional address "acme-branch" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated additional address master data after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects an additional address of a legal entity with its master data.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same additional address with new master data.
  ## The record's output reflects the updated additional address master data.
  @TEST_CXTPM-1016 @BPDM
  Scenario: Updated Additional Address Of Legal Entity Master Data In Output
    Given record "acme-address-record" reflects additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    When the sharing member updates record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-updated-content"
    Then "acme-address-record" output reflects additional address "acme-branch" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the master data of an additional address of a site produced for it.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to an additional address of a site with master data.
  ## The record's output reflects the additional address master data.
  @TEST_CXTPM-1015 @BPDM
  Scenario: Additional Address Of Site Master Data In Output
    When the sharing member shares record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-content"
    Then "acme-site-address-record" output reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated master data of an additional address of a site after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects an additional address of a site with its master data.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same additional address with new master data.
  ## The record's output reflects the updated additional address master data.
  @TEST_CXTPM-1018 @BPDM
  Scenario: Updated Additional Address Of Site Master Data In Output
    Given record "acme-site-address-record" reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-content"
    When the sharing member updates record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-updated-content"
    Then "acme-site-address-record" output reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" in its master data
