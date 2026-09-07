# This feature covers how a record's output reflects the confidence criteria of the golden record it was
# refined to. Confidence criteria reflect two independent signals: whether the data is owner-shared
# (OwnerShared signal) and whether the golden record process provider verified it against an external data
# source (Verified signal, set by refining with vs. without external verification).
#
# A third criterion, the number of sharing members, is not a signal a refinement carries: the Pool counts
# the sharing members that share a golden record. The count is held on the address and mirrored onto the legal
# entity whose legal address it is, which is why both are asserted. It only counts towards the confidence
# LEVEL from three sharing members on, so the scenarios at the end of this feature cover it in three steps:
# the count of a single member, its rise to two, and the level rising with the third. Each of the latter two
# needs a Gate per sharing member to share through and is skipped when the run has fewer of them.
#
# The OwnerShared signal is only *requested* by the sharing member (by sharing as own vs. third-party data);
# it is the golden record process refinement step that *finally assigns* it. The owner can signal intent, but
# the refinement determines what the record actually is, and it assigns the signal only to the entity the
# record is refined to (the resulting business partner) - never to a parent entity the process itself
# determined. So when a record shared as own company data is refined to an additional address, only that
# address becomes OwnerShared; its determined parent legal entity carries NoConfidence.
#
# The following named levels are used throughout (timestamps are excluded from comparison):
#
#   NoConfidence         - neither signal is set
#   OwnerShared          - owner submitted as own company data; not externally verified
#   Verified             - externally verified; not submitted as own company data
#   VerifiedOwnerShared  - both signals are set
#
# Sites always carry OwnerShared confidence. For legal entities and addresses the level depends on the two
# signals and on whether the matched address is a legal address or an additional address. When the matched
# address is a legal address, the legal entity and the address share the same confidence level. When the
# matched address is an additional address, the legal entity always carries NoConfidence and the address is
# evaluated independently.
@CXTPM-1043
Feature: Output Reflects Golden Record Confidence Criteria

  # -- Legal entity matched to legal address --

  #h3. Test Objective:
  #
  #* Verify a record refined to a legal entity reflects NoConfidence on its legal entity and legal address when neither the OwnerShared nor the Verified signal is set.
  #
  #h3. Description:
  #
  #* The sharing member shares a third-party record.
  #* The golden record process refines it to a legal entity without external verification.
  #* The output reflects NoConfidence for both the legal entity and the legal address.
  @TEST_CXTPM-997 @BPDM
  Scenario: Legal Entity No Confidence
    When the sharing member shares third-party record "acme-record"
    And the golden record process refines record "acme-record" to a legal entity without external verification
    Then "acme-record" output reflects NoConfidence for its legal entity
    And "acme-record" output reflects NoConfidence for its legal address

  #h3. Test Objective:
  #
  #* Verify a record shared as own company data reflects OwnerShared confidence on its legal entity and legal address.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to a legal entity without external verification.
  #* The output reflects OwnerShared confidence for both the legal entity and the legal address.
  @TEST_CXTPM-994 @BPDM
  Scenario: Legal Entity Shared By Owner
    When the sharing member shares own company record "acme-record"
    And the golden record process refines record "acme-record" to a legal entity without external verification
    Then "acme-record" output reflects OwnerShared confidence for its legal entity
    And "acme-record" output reflects OwnerShared confidence for its legal address

  #h3. Test Objective:
  #
  #* Verify a record verified against an external data source reflects Verified confidence on its legal entity and legal address.
  #
  #h3. Description:
  #
  #* The sharing member shares a third-party record.
  #* The golden record process refines it to a legal entity with external verification.
  #* The output reflects Verified confidence for both the legal entity and the legal address.
  @TEST_CXTPM-1001 @BPDM
  Scenario: Legal Entity Verified By External Datasource
    When the sharing member shares third-party record "acme-record"
    And the golden record process refines record "acme-record" to a legal entity with external verification
    Then "acme-record" output reflects Verified confidence for its legal entity
    And "acme-record" output reflects Verified confidence for its legal address

  #h3. Test Objective:
  #
  #* Verify a record that is both owner-shared and externally verified reflects VerifiedOwnerShared confidence on its legal entity and legal address.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to a legal entity with external verification.
  #* The output reflects VerifiedOwnerShared confidence for both the legal entity and the legal address.
  @TEST_CXTPM-998 @BPDM
  Scenario: Legal Entity Shared And Verified By External Datasource
    When the sharing member shares own company record "acme-record"
    And the golden record process refines record "acme-record" to a legal entity with external verification
    Then "acme-record" output reflects VerifiedOwnerShared confidence for its legal entity
    And "acme-record" output reflects VerifiedOwnerShared confidence for its legal address

  # -- Additional address of legal entity --

  #h3. Test Objective:
  #
  #* Verify that when an own-shared record is refined to an additional address, only the address becomes OwnerShared while its determined parent legal entity carries NoConfidence.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to an additional address of a legal entity without external verification.
  #* The output reflects NoConfidence for the legal entity and OwnerShared confidence for the additional address.
  @TEST_CXTPM-1000 @BPDM
  Scenario: Additional Address Shared By Owner
    When the sharing member shares own company record "acme-address-record"
    And the golden record process refines record "acme-address-record" to an additional address of a legal entity without external verification
    Then "acme-address-record" output reflects NoConfidence for its legal entity
    And "acme-address-record" output reflects OwnerShared confidence for its additional address

  #h3. Test Objective:
  #
  #* Verify an additional address evaluates its confidence independently of its parent legal entity, which stays NoConfidence even when both signals are set.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to an additional address of a legal entity with external verification.
  #* The output reflects NoConfidence for the legal entity and VerifiedOwnerShared confidence for the additional address.
  @TEST_CXTPM-996 @BPDM
  Scenario: Additional Address Shared And Verified By External Datasource
    When the sharing member shares own company record "acme-address-record"
    And the golden record process refines record "acme-address-record" to an additional address of a legal entity with external verification
    Then "acme-address-record" output reflects NoConfidence for its legal entity
    And "acme-address-record" output reflects VerifiedOwnerShared confidence for its additional address

  # -- Site --

  #h3. Test Objective:
  #
  #* Verify a site always reflects OwnerShared confidence, even when shared as third-party data.
  #
  #h3. Description:
  #
  #* The sharing member shares a third-party record.
  #* The golden record process refines it to a site.
  #* The output reflects OwnerShared confidence for the site.
  @TEST_CXTPM-1002 @BPDM
  Scenario: Site Confidence
    When the sharing member shares third-party record "acme-site-record"
    And the golden record process refines record "acme-site-record" to a site
    Then "acme-site-record" output reflects OwnerShared confidence for its site

  # -- Site-based legal entity --

  #h3. Test Objective:
  #
  #* Verify a site-based legal entity reflects OwnerShared on the site while its legal entity and legal address stay NoConfidence.
  #
  #h3. Description:
  #
  #* The sharing member shares a third-party record.
  #* The golden record process refines it to a site-based legal entity without external verification.
  #* The output reflects OwnerShared confidence for the site and NoConfidence for the legal entity and legal address.
  @TEST_CXTPM-999 @BPDM
  Scenario: Site Under No Confidence Legal Entity
    When the sharing member shares third-party record "acme-site-record"
    And the golden record process refines record "acme-site-record" to a site-based legal entity without external verification
    Then "acme-site-record" output reflects OwnerShared confidence for its site
    And "acme-site-record" output reflects NoConfidence for its legal entity
    And "acme-site-record" output reflects NoConfidence for its legal address

  # -- Additional address of site --

  #h3. Test Objective:
  #
  #* Verify that for an additional address of a site, the site and the address reflect OwnerShared while the legal entity stays NoConfidence.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to an additional address of a site without external verification.
  #* The output reflects OwnerShared confidence for the site and the additional address, and NoConfidence for the legal entity.
  @TEST_CXTPM-995 @BPDM
  Scenario: Additional Address Of Site Shared By Owner
    When the sharing member shares own company record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to an additional address of a site without external verification
    Then "acme-site-address-record" output reflects OwnerShared confidence for its site
    And "acme-site-address-record" output reflects NoConfidence for its legal entity
    And "acme-site-address-record" output reflects OwnerShared confidence for its additional address

  # -- Sharing member count --

  #h3. Test Objective:
  #
  #* Verify the sharing member count of a golden record shared by a single sharing member is one.
  #
  #h3. Description:
  #
  #* The sharing member shares an own company record.
  #* The golden record process refines it to a legal entity.
  #* The output reflects a sharing member count of one for the legal entity and the legal address.
  @TEST_CXTPM-1063 @BPDM
  Scenario: Legal Entity Shared By One Sharing Member
    When the sharing member shares own company record "acme-record"
    And the golden record process refines record "acme-record" to a legal entity without external verification
    Then "acme-record" output reflects a sharing member count of 1 for its legal entity
    And "acme-record" output reflects a sharing member count of 1 for its legal address

  #h3. Test Objective:
  #
  #* Verify the sharing member count of a golden record rises when a second sharing member shares the same golden record, for both members.
  #
  #h3. Preconditions:
  #
  #* A record of the first sharing member reflects a legal entity with a sharing member count of one.
  #
  #h3. Description:
  #
  #* The second sharing member shares an own company record.
  #* The golden record process refines it to the same legal entity.
  #* Both sharing members' outputs reflect a sharing member count of two for the legal entity and the legal address.
  @TEST_CXTPM-1064 @BPDM @TwoSharingMembers
  Scenario: Legal Entity Shared By Two Sharing Members
    Given record "acme-record" of the first sharing member reflects legal entity "acme" with master data "acme-content"
    And "acme-record" output reflects a sharing member count of 1 for its legal entity
    When the second sharing member shares own company record "acme-other-record"
    And the golden record process refines record "acme-other-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects a sharing member count of 2 for its legal entity
    And "acme-record" output reflects a sharing member count of 2 for its legal address
    And "acme-other-record" output reflects a sharing member count of 2 for its legal entity
    And "acme-other-record" output reflects a sharing member count of 2 for its legal address

  #h3. Test Objective:
  #
  #* Verify the confidence level of a golden record rises once a third sharing member shares it.
  #
  #h3. Description:
  #
  #* Three sharing members each share a third-party record of their own.
  #* The golden record process refines both to the same legal entity.
  #* Only from the third sharing member on does the sharing member count raise the confidence level, from zero to one.
  @TEST_CXTPM-1065 @BPDM @ThreeSharingMembers
  Scenario: Legal Entity Shared By Three Sharing Members
    When the first sharing member shares third-party record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects a sharing member count of 1 for its legal entity
    And "acme-record" output reflects a confidence level of 0 for its legal entity
    When the second sharing member shares third-party record "acme-second-record"
    And the golden record process refines record "acme-second-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects a sharing member count of 2 for its legal entity
    And "acme-record" output reflects a confidence level of 0 for its legal entity
    When the third sharing member shares third-party record "acme-third-record"
    And the golden record process refines record "acme-third-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects a sharing member count of 3 for its legal entity
    And "acme-record" output reflects a confidence level of 1 for its legal entity
    And "acme-third-record" output reflects a confidence level of 1 for its legal entity
