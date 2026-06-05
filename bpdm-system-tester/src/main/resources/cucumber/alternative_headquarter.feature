Feature: Alternative Headquarter

  Scenario: Sharing member registers an alternative headquarter for their legal entity
    Given a sharing member has a Gate record "main-hq" that matches to the legal address "addr-acme" of legal entity "acme"
    And a Gate record "alt-hq" that matches to the legal address "addr-globex" of legal entity "globex"
    When the sharing member shares a relation "alt-hq-rel" where "alt-hq" is an alternative headquarter for "main-hq"
    And the cleaning service provider accepts relation "alt-hq-rel" as submitted
    Then relation "alt-hq-rel" reaches sharing success
    And "alt-hq-rel" matches to a golden record alternative headquarter relation between "acme" to "globex" in any order
    And "main-hq" and "alt-hq" hold a golden record alternative headquarter relation between "acme" and "globex"
