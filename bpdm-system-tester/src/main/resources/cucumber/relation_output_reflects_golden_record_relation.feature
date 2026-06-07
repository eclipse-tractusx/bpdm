Feature: Sharing Member Relation Output Reflects Golden Record Relation

  Scenario: IsOwnedBy Relation Reflected In Sharing Member Relation Output
    Given record "owner-record" reflects legal entity "owner"
    And record "owned-record" reflects legal entity "owned"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "owned-record" to "owner-record"
    And the golden record process establishes relation "ownership"
    Then relation "ownership" output reflects the established golden record relation

  Scenario: IsAlternativeHeadquarterFor Relation Reflected In Sharing Member Relation Output
    Given record "main-hq-record" reflects legal entity "main-hq"
    And record "alt-hq-record" reflects legal entity "alt-hq"
    When the sharing member shares relation "alternative-headquarter" of type "IsAlternativeHeadquarterFor" from "alt-hq-record" to "main-hq-record"
    And the golden record process establishes relation "alternative-headquarter"
    Then relation "alternative-headquarter" output reflects the established golden record relation

  Scenario: IsManagedBy Relation Reflected In Sharing Member Relation Output
    Given own company record "manager-record" reflects legal entity "manager"
    And own company record "managed-record" reflects legal entity "managed"
    When the sharing member shares relation "management" of type "IsManagedBy" from "managed-record" to "manager-record" with validity starting now and not in the past
    And the golden record process establishes relation "management"
    Then relation "management" output reflects the established golden record relation

  Scenario: IsReplacedBy Relation Reflected In Sharing Member Relation Output
    Given record "legal-address-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-address-record" to "branch-record" effective immediately
    And the golden record process establishes relation "relocation"
    Then relation "relocation" output reflects the established golden record relation