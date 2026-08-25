# This feature covers master data changes that originate OUTSIDE the record under test: the golden record
# master data changes without this record being shared or updated, because a DIFFERENT record is refined
# into the same golden record with updated content. Every record that already reflected that golden record
# must then reflect the new master data as well. The companion feature
# "output_reflects_own_shared_master_data.feature" covers the other direction, where the change originates
# from the record's own sharing.
#
# The change is forced the most straightforward way: a second "driver" record is shared and the golden
# record process refines it to the SAME legal entity / site / address label as the record under test, but
# with a new master data seed. Refining to the same label assigns the same BPN request identifier, so the
# Pool matches it to the same golden record and updates that record's master data. The record under test
# is never touched, yet its output must reflect the updated master data.
#
# Most scenarios here drive that change with a second record of the SAME sharing member, which keeps them
# runnable against a single Gate. The last one has the driver record belong to ANOTHER sharing member, the
# case the golden record process exists for: what one member shares becomes visible to the others. It needs
# a second Gate to share through and is skipped when the run has only one sharing member.
#
# "master data" here means the descriptive legal entity, site and address attributes:
# legal name, short name, legal form, site name, address name, address type and postal addresses.
# It deliberately excludes identifiers, states, BPNs, confidence criteria and golden record
# relations, which are covered by dedicated tests.
@CXTPM-1043
Feature: Output Reflects Golden Record Master Data Changes

  #h3. Test Objective:
  #
  #* Verify a record reflects updated legal entity master data when a different record changes the shared golden record, without the record itself being touched.
  #
  #h3. Preconditions:
  #
  #* A record already reflects a legal entity with its master data.
  #
  #h3. Description:
  #
  #* The sharing member shares a second driver record.
  #* The golden record process refines it to the same legal entity with new master data.
  #* Both records' outputs reflect the updated legal entity master data.
  @TEST_CXTPM-1005 @BPDM
  Scenario: Legal Entity Master Data Change Reflected In Output
    Given record "acme-record" reflects legal entity "acme" with master data "acme-content"
    When the sharing member shares record "acme-other-record"
    And the golden record process refines record "acme-other-record" to legal entity "acme" with master data "acme-updated-content"
    Then "acme-record" output reflects legal entity "acme" in its master data
    And "acme-other-record" output reflects legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record reflects updated site master data when a different record changes the shared golden record, without the record itself being touched.
  #
  #h3. Preconditions:
  #
  #* A record already reflects a site of a legal entity with its master data.
  #
  #h3. Description:
  #
  #* The sharing member shares a second driver record.
  #* The golden record process refines it to the same site with new master data.
  #* Both records' outputs reflect the updated site master data.
  @TEST_CXTPM-1006 @BPDM
  Scenario: Site Master Data Change Reflected In Output
    Given record "acme-site-record" reflects site "acme-site" of legal entity "acme" with master data "acme-site-content"
    When the sharing member shares record "acme-site-other-record"
    And the golden record process refines record "acme-site-other-record" to site "acme-site" of legal entity "acme" with master data "acme-site-updated-content"
    Then "acme-site-record" output reflects site "acme-site" of legal entity "acme" in its master data
    And "acme-site-other-record" output reflects site "acme-site" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record reflects updated additional address master data when a different record changes the shared golden record, without the record itself being touched.
  #
  #h3. Preconditions:
  #
  #* A record already reflects an additional address of a legal entity with its master data.
  #
  #h3. Description:
  #
  #* The sharing member shares a second driver record.
  #* The golden record process refines it to the same additional address with new master data.
  #* Both records' outputs reflect the updated additional address master data.
  @TEST_CXTPM-1004 @BPDM
  Scenario: Additional Address Of Legal Entity Master Data Change Reflected In Output
    Given record "acme-address-record" reflects additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    When the sharing member shares record "acme-address-other-record"
    And the golden record process refines record "acme-address-other-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-updated-content"
    Then "acme-address-record" output reflects additional address "acme-branch" of legal entity "acme" in its master data
    And "acme-address-other-record" output reflects additional address "acme-branch" of legal entity "acme" in its master data

  #h3. Test Objective:
  #
  #* Verify a record reflects updated master data of an additional address of a site when a different record changes the shared golden record, without the record itself being touched.
  #
  #h3. Preconditions:
  #
  #* A record already reflects an additional address of a site with its master data.
  #
  #h3. Description:
  #
  #* The sharing member shares a second driver record.
  #* The golden record process refines it to the same additional address with new master data.
  #* Both records' outputs reflect the updated additional address master data.
  @TEST_CXTPM-1003 @BPDM
  Scenario: Additional Address Of Site Master Data Change Reflected In Output
    Given record "acme-site-address-record" reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-content"
    When the sharing member shares record "acme-site-address-other-record"
    And the golden record process refines record "acme-site-address-other-record" to additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-updated-content"
    Then "acme-site-address-record" output reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" in its master data
    And "acme-site-address-other-record" output reflects additional address "acme-dock" of site "acme-site" of legal entity "acme" in its master data

  # -- The change originates at another sharing member --

  #h3. Test Objective:
  #
  #* Verify a record reflects updated legal entity master data when the golden record is changed by a record of a different sharing member.
  #
  #h3. Preconditions:
  #
  #* A record of the first sharing member already reflects a legal entity with its master data.
  #
  #h3. Description:
  #
  #* The second sharing member shares a record of its own.
  #* The golden record process refines it to the same legal entity with new master data.
  #* Both sharing members' outputs reflect the updated legal entity master data.
  @TwoSharingMembers @BPDM
  Scenario: Legal Entity Master Data Change By Another Sharing Member Reflected In Output
    Given record "acme-record" of the first sharing member reflects legal entity "acme" with master data "acme-content"
    When the second sharing member shares record "acme-other-record"
    And the golden record process refines record "acme-other-record" to legal entity "acme" with master data "acme-updated-content"
    Then "acme-record" output reflects legal entity "acme" in its master data
    And "acme-other-record" output reflects legal entity "acme" in its master data
