@CXTPM-1043
Feature: Sharing Member Relation Output Reflects Golden Record Relation

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsOwnedBy golden record relation.
  #
  #h3. Preconditions:
  #
  #* Two records each reflect a legal entity (owner and owned).
  #
  #h3. Description:
  #
  #* The sharing member shares an IsOwnedBy relation from the owned record to the owner record.
  #* The golden record process establishes the relation.
  #* The relation output reflects the established golden record relation.
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
  #* Two records each reflect a legal entity (main and alternative headquarter).
  #
  #h3. Description:
  #
  #* The sharing member shares an IsAlternativeHeadquarterFor relation from the alternative to the main headquarter record.
  #* The golden record process establishes the relation.
  #* The relation output reflects the established golden record relation.
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
  #* Two own company records each reflect a legal entity (manager and managed); the managing entity is a dataspace participant.
  #
  #h3. Description:
  #
  #* The sharing member shares an IsManagedBy relation from the managed to the manager record, with validity starting now and not in the past.
  #* The golden record process establishes the relation.
  #* The relation output reflects the established golden record relation.
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
  #* A record reflects a legal entity and another record reflects an additional address of that same legal entity.
  #
  #h3. Description:
  #
  #* The sharing member shares an IsReplacedBy relation from the legal address to the additional address, effective immediately.
  #* The golden record process establishes the relation.
  #* The relation output reflects the established golden record relation.
  @TEST_CXTPM-1038 @BPDM
  Scenario: IsReplacedBy Relation Reflected In Sharing Member Relation Output
    Given record "legal-address-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-address-record" to "branch-record" effective immediately
    And the golden record process establishes relation "relocation"
    Then relation "relocation" output reflects the established golden record relation
