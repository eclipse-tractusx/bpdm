Feature: Confidence Criteria Calculation

  Confidence criteria reflect two independent signals on a golden record:
  whether the owner submitted the data as their own company data (OwnerShared signal)
  and whether the golden record process provider verified it against an external data
  source (Verified signal). The following named levels are used throughout this feature
  (timestamps are excluded from comparison):

    NoConfidence         — neither signal is set
    OwnerShared          — owner submitted as own company data; not externally verified
    Verified             — externally verified; not submitted as own company data
    VerifiedOwnerShared  — both signals are set

  Sites always carry OwnerShared confidence. For legal entities and addresses the
  level depends on the two signals and on whether the matched address is a legal
  address or an additional address. When the matched address is a legal address, the
  legal entity and the address share the same confidence level. When the matched
  address is an additional address, the legal entity always carries NoConfidence and
  the address is evaluated independently.

  # -- Legal entity matched to legal address --

  Scenario: Legal entity and legal address have NoConfidence when neither criterion applies

    When a sharing member uploads a business partner record "BP1"
    And the cleaning service provider refines "BP1" as a legal entity without external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has NoConfidence
    And the legal address of "BP1" has NoConfidence

  Scenario: Legal entity and legal address have OwnerShared confidence when shared as own company data

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service provider refines "BP1" as a legal entity without external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has OwnerShared confidence
    And the legal address of "BP1" has OwnerShared confidence

  Scenario: Legal entity and legal address have Verified confidence when marked by external data source

    When a sharing member uploads a business partner record "BP1"
    And the cleaning service provider refines "BP1" as a legal entity with external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has Verified confidence
    And the legal address of "BP1" has Verified confidence

  Scenario: Legal entity and legal address have VerifiedOwnerShared confidence when both criteria apply

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service provider refines "BP1" as a legal entity with external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has VerifiedOwnerShared confidence
    And the legal address of "BP1" has VerifiedOwnerShared confidence

  # -- Additional address of legal entity --

  Scenario: Legal entity has NoConfidence when matched address is an additional address

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service provider refines "BP1" as an additional address of a legal entity without external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has NoConfidence
    And the additional address of "BP1" has OwnerShared confidence

  Scenario: Additional address confidence is independent of the legal entity when both criteria apply

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service provider refines "BP1" as an additional address of a legal entity with external verification
    Then record "BP1" reaches sharing success
    And the legal entity of "BP1" has NoConfidence
    And the additional address of "BP1" has VerifiedOwnerShared confidence

  # -- Site --

  Scenario: Site always has OwnerShared confidence regardless of the owner signal

    When a sharing member uploads a business partner record "BP1"
    And the cleaning service provider refines "BP1" as a site
    Then record "BP1" reaches sharing success
    And the site of "BP1" has OwnerShared confidence

  # -- Site-based legal entity --

  Scenario: Site always has OwnerShared confidence even when legal entity and legal address have NoConfidence

    When a sharing member uploads a business partner record "BP1"
    And the cleaning service provider refines "BP1" as a site-based legal entity without external verification
    Then record "BP1" reaches sharing success
    And the site of "BP1" has OwnerShared confidence
    And the legal entity of "BP1" has NoConfidence
    And the legal address of "BP1" has NoConfidence

  # -- Additional address of site --

  Scenario: Legal entity has NoConfidence and site has OwnerShared when matched address is an additional address of a site

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service provider refines "BP1" as an additional address of a site without external verification
    Then record "BP1" reaches sharing success
    And the site of "BP1" has OwnerShared confidence
    And the legal entity of "BP1" has NoConfidence
    And the additional address of "BP1" has OwnerShared confidence
