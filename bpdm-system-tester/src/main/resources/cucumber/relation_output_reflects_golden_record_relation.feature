@CXTPM-1039
Feature: Sharing Member Relation Output Reflects Golden Record Relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsOwnedBy golden record relation.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (owner and owned).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsOwnedBy relation from the owned record to the owner record.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @TEST_CXTPM-1035 @BPDM @Smoke
  Scenario: IsOwnedBy Relation Reflected In Sharing Member Relation Output
    Given record "owner-record" reflects legal entity "owner"
    And record "owned-record" reflects legal entity "owned"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "owned-record" to "owner-record"
    And the golden record process establishes relation "ownership"
    Then relation "ownership" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsAlternativeHeadquarterFor golden record relation.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (main and alternative headquarter).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsAlternativeHeadquarterFor relation from the alternative to the main headquarter record.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @TEST_CXTPM-1036 @BPDM
  Scenario: IsAlternativeHeadquarterFor Relation Reflected In Sharing Member Relation Output
    Given record "main-hq-record" reflects legal entity "main-hq"
    And record "alt-hq-record" reflects legal entity "alt-hq"
    When the sharing member shares relation "alternative-headquarter" of type "IsAlternativeHeadquarterFor" from "alt-hq-record" to "main-hq-record"
    And the golden record process establishes relation "alternative-headquarter"
    Then relation "alternative-headquarter" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsManagedBy golden record relation.
  #
  #h3. Preconditions:
  #
  ## Two own company records each reflect a legal entity (manager and managed); the managing entity is a dataspace participant.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsManagedBy relation from the managed to the manager record, with validity starting now and not in the past.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @TEST_CXTPM-1037 @BPDM
  Scenario: IsManagedBy Relation Reflected In Sharing Member Relation Output
    Given own company record "manager-record" reflects legal entity "manager"
    And own company record "managed-record" reflects legal entity "managed"
    When the sharing member shares relation "management" of type "IsManagedBy" from "managed-record" to "manager-record" with validity starting now and not in the past
    And the golden record process establishes relation "management"
    Then relation "management" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation.
  #
  #h3. Preconditions:
  #
  ## A record reflects a legal entity and another record reflects an additional address of that same legal entity.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the legal address to the additional address, effective immediately.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @TEST_CXTPM-1038 @BPDM
  Scenario: IsReplacedBy Relation Reflected In Sharing Member Relation Output
    Given record "legal-address-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-address-record" to "branch-record" effective immediately
    And the golden record process establishes relation "relocation"
    Then relation "relocation" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two legal entities.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the predecessor record to the successor record.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @BPDM
  Scenario: IsReplacedBy Relation Between Legal Entities Reflected In Sharing Member Relation Output
    Given record "predecessor-record" reflects legal entity "predecessor"
    And record "successor-record" reflects legal entity "successor"
    When the sharing member shares relation "succession" of type "IsReplacedBy" from "predecessor-record" to "successor-record"
    And the golden record process establishes relation "succession"
    Then relation "succession" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two sites.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a site of the same legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the predecessor site record to the successor site record, effective immediately.
  ## The golden record process establishes the relation between the two BPNS.
  ## The relation output reflects the established golden record relation.
  @BPDM
  Scenario: IsReplacedBy Relation Between Sites Reflected In Sharing Member Relation Output
    Given record "predecessor-site-record" reflects site "predecessor-site" of legal entity "acme"
    And record "successor-site-record" reflects site "successor-site" of legal entity "acme"
    When the sharing member shares relation "site-succession" of type "IsReplacedBy" from "predecessor-site-record" to "successor-site-record" effective immediately
    And the golden record process establishes relation "site-succession"
    Then relation "site-succession" output reflects the established golden record relation