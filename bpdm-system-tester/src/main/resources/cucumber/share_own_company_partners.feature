Feature: Share own company business partner data

  A sharing member submits records flagged as their own company data.
  The sharing pipeline routes each record through the cleaning service,
  which classifies and enriches it. These scenarios verify the expected
  output for each classification outcome and that re-uploading a record
  replaces its previous content entirely.

  Scenario: Share a new legal entity

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service refines "BP1" as a legal entity
    Then record "BP1" reaches sharing success
    And "BP1" output is a legal entity reflecting the refinement and submitted data

  Scenario: Share a new site-based legal entity

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service refines "BP1" as a site-based legal entity
    Then record "BP1" reaches sharing success
    And "BP1" output is a site-based legal entity reflecting the refinement and submitted data

  Scenario: Share a new site

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service refines "BP1" as a site
    Then record "BP1" reaches sharing success
    And "BP1" output is a site reflecting the refinement and submitted data

  Scenario: Share a new additional address of a site

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service refines "BP1" as an additional address of a site
    Then record "BP1" reaches sharing success
    And "BP1" output is an additional address of a site reflecting the refinement and submitted data

  Scenario: Share a new additional address of a legal entity

    When a sharing member uploads their own company data as record "BP1"
    And the cleaning service refines "BP1" as an additional address of a legal entity
    Then record "BP1" reaches sharing success
    And "BP1" output is an additional address of a legal entity reflecting the refinement and submitted data

  Scenario: Re-uploading own company data replaces its previous content

    Given "BP1" has already been shared as a legal entity
    When a sharing member re-uploads "BP1" with different content
    And the cleaning service refines "BP1" as a legal entity
    Then record "BP1" reaches sharing success
    And "BP1" output is a legal entity reflecting the new refinement and the new submission

  Scenario: Headquarter relocation reclassifies addresses while preserving their data

    Given "BP1" has already been shared as a legal entity
    And "BP2" has already been shared as an additional address of "BP1"'s legal entity
    When a sharing member submits an IsReplacedBy relation "R1" from "BP1"'s address to "BP2"'s address
    And the cleaning service accepts relation "R1" as submitted
    Then relation "R1" reaches sharing success
    And relation "R1" output is the accepted relation with the two addresses linked
    And "BP1" is reclassified to an additional address with its address data unchanged
    And "BP2" is reclassified to the legal entity's headquarters with its address data unchanged
