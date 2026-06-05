Feature: IsManagedBy Sharing

  Scenario: Sharing member registers a managing relation between their own company legal entities
    Given a sharing member has an own company Gate record "manager-hq" that matches to the legal address "addr-acme" of legal entity "acme"
    And an own company Gate record "managed-hq" that matches to the legal address "addr-globex" of legal entity "globex"
    When the sharing member shares a relation "managed-by-rel" where "managed-hq" is managed by "manager-hq" effective from now
    And the cleaning service provider accepts relation "managed-by-rel" as submitted
    Then relation "managed-by-rel" reaches sharing success
    And "managed-by-rel" matches to a golden record legal entity relation where "acme" manages "globex"
    And "manager-hq" and "managed-hq" hold a golden record legal entity relation where "amce" manages "globex"
