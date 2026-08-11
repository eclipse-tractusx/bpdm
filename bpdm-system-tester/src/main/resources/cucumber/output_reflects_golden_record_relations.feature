# This feature covers how a record's output reflects the golden record business partner relations of the
# golden record(s) it is matched to. For any relation between two golden records, every record matched to
# one of those golden records must surface that relation in its output.
#
# Relations are shown on two levels, independent of the record's own golden record type:
#   - legal entity relations (IsOwnedBy, IsManagedBy, IsAlternativeHeadquarterFor) surface on the output's
#     legal entity,
#   - address relations (IsReplacedBy between addresses) surface on the output's address.
# IsReplacedBy also exists between two legal entities, where it surfaces on the legal entity; that variant is
# covered in legal_entity_succession.feature.
# All levels are shown regardless of what the record itself was refined to. In particular, a record refined
# as an additional address still shows the relations of its parent legal entity, even though the record is
# technically the additional address and the legal entity is only its parent.
#
# Each relation type has requirements the scenarios make explicit so the relations can be shared
# successfully:
#   - IsOwnedBy and IsAlternativeHeadquarterFor are relations between two legal entities.
#   - IsManagedBy is between two legal entities; the managing entity must be a dataspace participant (own
#     company data) and the validity must not start in the past.
#   - IsReplacedBy at address level is between a legal address and an additional address of the SAME legal entity and must be
#     currently valid. The Pool expects the legal address as the relation source and the additional address
#     as the target, and reclassifies (swaps) the two addresses; this feature only asserts that the relation
#     is reflected and accepts the swap.
@CXTPM-1039
Feature: Output Reflects Golden Record Relations

  #h3. Test Objective:
  #
  #* Verify an established IsOwnedBy relation surfaces on the legal entity output of both involved records.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (owner and owned).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsOwnedBy relation from the owned record to the owner record.
  ## The golden record process establishes the relation.
  ## Both records' outputs reflect the relation on their legal entity.
  @TEST_CXTPM-1008 @BPDM
  Scenario: IsOwnedBy Relation Reflected In Legal Entity Outputs
    Given record "owner-record" reflects legal entity "owner"
    And record "owned-record" reflects legal entity "owned"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "owned-record" to "owner-record"
    And the golden record process establishes relation "ownership"
    Then "owned-record" output reflects the legal entity golden record relation "ownership"
    And "owner-record" output reflects the legal entity golden record relation "ownership"

  #h3. Test Objective:
  #
  #* Verify an established IsAlternativeHeadquarterFor relation surfaces on the legal entity output of both involved records.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (main and alternative headquarter).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsAlternativeHeadquarterFor relation from the alternative to the main headquarter record.
  ## The golden record process establishes the relation.
  ## Both records' outputs reflect the relation on their legal entity.
  @TEST_CXTPM-1010 @BPDM
  Scenario: IsAlternativeHeadquarterFor Relation Reflected In Legal Entity Outputs
    Given record "main-hq-record" reflects legal entity "main-hq"
    And record "alt-hq-record" reflects legal entity "alt-hq"
    When the sharing member shares relation "alternative-headquarter" of type "IsAlternativeHeadquarterFor" from "alt-hq-record" to "main-hq-record"
    And the golden record process establishes relation "alternative-headquarter"
    Then "alt-hq-record" output reflects the legal entity golden record relation "alternative-headquarter"
    And "main-hq-record" output reflects the legal entity golden record relation "alternative-headquarter"

  #h3. Test Objective:
  #
  #* Verify an established IsManagedBy relation surfaces on the legal entity output of both involved records.
  #
  #h3. Preconditions:
  #
  ## Two own company records each reflect a legal entity (manager and managed); the managing entity is a dataspace participant.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsManagedBy relation from the managed to the manager record, with validity starting now and not in the past.
  ## The golden record process establishes the relation.
  ## Both records' outputs reflect the relation on their legal entity.
  @TEST_CXTPM-1009 @BPDM
  Scenario: IsManagedBy Relation Reflected In Legal Entity Outputs
    Given own company record "manager-record" reflects legal entity "manager"
    And own company record "managed-record" reflects legal entity "managed"
    When the sharing member shares relation "management" of type "IsManagedBy" from "managed-record" to "manager-record" with validity starting now and not in the past
    And the golden record process establishes relation "management"
    Then "managed-record" output reflects the legal entity golden record relation "management"
    And "manager-record" output reflects the legal entity golden record relation "management"

  #h3. Test Objective:
  #
  #* Verify an established IsReplacedBy relation surfaces on the address output of both involved records.
  #
  #h3. Preconditions:
  #
  ## A record reflects a legal entity and another record reflects an additional address of that same legal entity.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the legal address to the additional address, effective immediately.
  ## The golden record process establishes the relation (the Pool swaps the two addresses).
  ## Both records' outputs reflect the relation on their address.
  @TEST_CXTPM-1007 @BPDM
  Scenario: IsReplacedBy Relation Reflected In Address Outputs
    Given record "legal-address-record" reflects legal entity "acme"
    And record "branch-record" reflects additional address "acme-branch" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "legal-address-record" to "branch-record" effective immediately
    And the golden record process establishes relation "relocation"
    Then "branch-record" output reflects the address golden record relation "relocation"
    And "legal-address-record" output reflects the address golden record relation "relocation"

  #h3. Test Objective:
  #
  #* Verify a record refined as an additional address surfaces both its parent legal entity relation and its own address relation.
  #
  #h3. Preconditions:
  #
  ## Records reflect a legal entity, an additional address of that legal entity, and a second owning legal entity.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsOwnedBy relation onto the legal entity and an IsReplacedBy relation onto its additional address.
  ## The golden record process establishes both relations.
  ## The additional address record's output reflects the parent legal entity relation and the address relation.
  @TEST_CXTPM-1011 @BPDM
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
