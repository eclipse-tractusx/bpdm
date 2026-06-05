Feature: IsOwnedBy Sharing

  Scenario: Sharing member registers an ownership relation between two legal entities
    Given a sharing member has a Gate record "owner-hq" that matches to the legal address "addr-acme" of legal entity "acme"
    And a Gate record "owned-hq" that matches to the legal address "addr-globex" of legal entity "globex"
    When the sharing member shares a relation "ownership-rel" where "owned-hq" is owned by "owner-hq"
    And the cleaning service provider accepts relation "ownership-rel" as submitted
    Then relation "ownership-rel" reaches sharing success
    And "ownership-rel" matches to a golden record legal entity relation where "acme" owns "globex"
    And "owner-hq" and "owned-hq" hold a golden record legal entity relation where "acme" owns "globex"
