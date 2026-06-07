Feature: Output Reflects Golden Record Relations

  # This feature covers how a record's output reflects the golden record business partner relations of the
  # golden record(s) it is matched to. For any relation between two golden records, every record matched to
  # one of those golden records must surface that relation in its output.
  #
  # Relations are shown on two levels, independent of the record's own golden record type:
  #   - legal entity relations (IsOwnedBy, IsManagedBy, IsAlternativeHeadquarterFor) surface on the output's
  #     legal entity,
  #   - address relations (IsReplacedBy) surface on the output's address.
  # All levels are shown regardless of what the record itself was refined to. In particular, a record refined
  # as an additional address still shows the relations of its parent legal entity, even though the record is
  # technically the additional address and the legal entity is only its parent.
  #
  # Each relation type has requirements the scenarios make explicit so the relations can be shared
  # successfully:
  #   - IsOwnedBy and IsAlternativeHeadquarterFor are relations between two legal entities.
  #   - IsManagedBy is between two legal entities; the managing entity must be a dataspace participant (own
  #     company data) and the validity must not start in the past.
  #   - IsReplacedBy is between a legal address and an additional address of the SAME legal entity and must be
  #     currently valid. The Pool expects the legal address as the relation source and the additional address
  #     as the target, and reclassifies (swaps) the two addresses; this feature only asserts that the relation
  #     is reflected and accepts the swap.

  Scenario: IsOwnedBy Relation Reflected In Legal Entity Outputs
    Given record "owner-record" reflects legal entity "owner"
    And record "owned-record" reflects legal entity "owned"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "owned-record" to "owner-record"
    And the golden record process establishes relation "ownership"
    Then "owned-record" output reflects the legal entity golden record relation "ownership"
    And "owner-record" output reflects the legal entity golden record relation "ownership"

  Scenario: IsAlternativeHeadquarterFor Relation Reflected In Legal Entity Outputs
    Given record "main-hq-record" reflects legal entity "main-hq"
    And record "alt-hq-record" reflects legal entity "alt-hq"
    When the sharing member shares relation "alternative-headquarter" of type "IsAlternativeHeadquarterFor" from "alt-hq-record" to "main-hq-record"
    And the golden record process establishes relation "alternative-headquarter"
    Then "alt-hq-record" output reflects the legal entity golden record relation "alternative-headquarter"
    And "main-hq-record" output reflects the legal entity golden record relation "alternative-headquarter"

  Scenario: IsManagedBy Relation Reflected In Legal Entity Outputs
    Given own company record "manager-record" reflects legal entity "manager"
    And own company record "managed-record" reflects legal entity "managed"
    When the sharing member shares relation "management" of type "IsManagedBy" from "managed-record" to "manager-record" with validity starting now and not in the past
    And the golden record process establishes relation "management"
    Then "managed-record" output reflects the legal entity golden record relation "management"
    And "manager-record" output reflects the legal entity golden record relation "management"

  Scenario: IsReplacedBy Relation Reflected In Address Outputs
    Given record "legal-address-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-address-record" to "branch-record" effective immediately
    And the golden record process establishes relation "relocation"
    Then "branch-record" output reflects the address golden record relation "relocation"
    And "legal-address-record" output reflects the address golden record relation "relocation"

  Scenario: Additional Address Record Reflects Its Parent Legal Entity Relation
    Given record "legal-entity-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    And record "owned-record" reflects legal entity "owned"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "owned-record" to "legal-entity-record"
    And the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-entity-record" to "branch-record" effective immediately
    And the golden record process establishes relation "ownership"
    And the golden record process establishes relation "relocation"
    Then "branch-record" output reflects the legal entity golden record relation "ownership"
    And "branch-record" output reflects the address golden record relation "relocation"
