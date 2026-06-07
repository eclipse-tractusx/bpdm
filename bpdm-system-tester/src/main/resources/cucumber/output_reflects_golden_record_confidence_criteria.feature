# This feature covers how a record's output reflects the confidence criteria of the golden record it was
# refined to. Confidence criteria reflect two independent signals: whether the data is owner-shared
# (OwnerShared signal) and whether the golden record process provider verified it against an external data
# source (Verified signal, set by refining with vs. without external verification).
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

Feature: Output Reflects Golden Record Confidence Criteria

  # -- Legal entity matched to legal address --

  #h3. Test Objective:
  #
  #* Verify a record refined to a legal entity reflects NoConfidence on its legal entity and legal address when neither the OwnerShared nor the Verified signal is set.
  #
  #h3. Description:
  #
  ## The sharing member shares a third-party record.
  ## The golden record process refines it to a legal entity without external verification.
  ## The output reflects NoConfidence for both the legal entity and the legal address.
  Scenario: Legal Entity And Legal Address Reflect NoConfidence When Neither Signal Is Set
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
  ## The sharing member shares an own company record.
  ## The golden record process refines it to a legal entity without external verification.
  ## The output reflects OwnerShared confidence for both the legal entity and the legal address.
  Scenario: Legal Entity And Legal Address Reflect OwnerShared When Shared As Own Company Data
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
  ## The sharing member shares a third-party record.
  ## The golden record process refines it to a legal entity with external verification.
  ## The output reflects Verified confidence for both the legal entity and the legal address.
  Scenario: Legal Entity And Legal Address Reflect Verified When Externally Verified
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
  ## The sharing member shares an own company record.
  ## The golden record process refines it to a legal entity with external verification.
  ## The output reflects VerifiedOwnerShared confidence for both the legal entity and the legal address.
  Scenario: Legal Entity And Legal Address Reflect VerifiedOwnerShared When Both Signals Are Set
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
  ## The sharing member shares an own company record.
  ## The golden record process refines it to an additional address of a legal entity without external verification.
  ## The output reflects NoConfidence for the legal entity and OwnerShared confidence for the additional address.
  Scenario: Legal Entity Reflects NoConfidence When The Matched Address Is An Additional Address
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
  ## The sharing member shares an own company record.
  ## The golden record process refines it to an additional address of a legal entity with external verification.
  ## The output reflects NoConfidence for the legal entity and VerifiedOwnerShared confidence for the additional address.
  Scenario: Additional Address Confidence Is Independent Of The Legal Entity When Both Signals Are Set
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
  ## The sharing member shares a third-party record.
  ## The golden record process refines it to a site.
  ## The output reflects OwnerShared confidence for the site.
  Scenario: Site Always Reflects OwnerShared Confidence Regardless Of The Owner Signal
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
  ## The sharing member shares a third-party record.
  ## The golden record process refines it to a site-based legal entity without external verification.
  ## The output reflects OwnerShared confidence for the site and NoConfidence for the legal entity and legal address.
  Scenario: Site Reflects OwnerShared Even When Legal Entity And Legal Address Reflect NoConfidence
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
  ## The sharing member shares an own company record.
  ## The golden record process refines it to an additional address of a site without external verification.
  ## The output reflects OwnerShared confidence for the site and the additional address, and NoConfidence for the legal entity.
  Scenario: Legal Entity Reflects NoConfidence And Site Reflects OwnerShared When The Matched Address Is An Additional Address Of A Site
    When the sharing member shares own company record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to an additional address of a site without external verification
    Then "acme-site-address-record" output reflects OwnerShared confidence for its site
    And "acme-site-address-record" output reflects NoConfidence for its legal entity
    And "acme-site-address-record" output reflects OwnerShared confidence for its additional address
