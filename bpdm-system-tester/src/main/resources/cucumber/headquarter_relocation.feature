Feature: Headquarter Relocation

  Scenario: Legal entity's headquarters moves to a previously shared address
    Given a sharing member has a Gate record "old-hq" that matches to the legal address "addr-old-hq" of legal entity "acme"
    And a Gate record "new-hq" that matches to the additional address "addr-new-hq" of legal entity "acme"
    When the sharing member shares a relation "head-reloc" where "new-hq" replaces "old-hq" effective immediately
    And the cleaning service provider accepts relation "head-reloc" as submitted
    Then relation "head-reloc" reaches sharing success
    And "head-reloc" matches to a golden record address relation where "addr-new-hq" replaces "addr-old-hq"
    And "old-hq" matches to "addr-old-hq" as an additional address of "acme"
    And "new-hq" matches to "addr-new-hq" as the legal address of "acme"
    And "old-hq" and "new-hq" hold a golden record address relation where "addr-new-hq" replaces "addr-old-hq"