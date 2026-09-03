# Dataspace Participant Guide

This guide is for a company that participates in the dataspace and reads business partner data from
the BPDM Pool - to resolve the BPNs it receives from other participants, and to keep its own systems
aligned with the golden records behind them.
It explains what a golden record is, what the Pool tells you about one, and what to call for the
cases that come up.

It does not cover sharing data with the golden record process to obtain BPNs of your own
([see the Sharing Member Guide](sharing-member-guide.md)), the EDC negotiation that gets you to the
Pool in the first place ([see Access BPDM over EDC](README.md#access-bpdm-over-edc)), or how to
operate a refinement service ([see the Refinement Service Provider Guide](refinement-service-guide.md)).
The endpoints named here are described in full in [pool.yaml](pool.yaml); this guide says which ones
to call and how to read what comes back.
The network-wide contract the Pool API implements is the
[BPDM Pool API standard](https://catenax-ev.github.io/docs/standards/CX-0012-BusinessPartnerDataPoolAPI).

<!-- TOC -->
* [Dataspace Participant Guide](#dataspace-participant-guide)
  * [The Golden Record](#the-golden-record)
    * [The Three Levels](#the-three-levels)
    * [Where An Address Belongs](#where-an-address-belongs)
    * [Metadata](#metadata)
  * [Getting Access](#getting-access)
  * [What The Pool Shows You](#what-the-pool-shows-you)
  * [Use Cases](#use-cases)
    * [Resolving A BPN You Received](#resolving-a-bpn-you-received)
    * [Finding A Company By Name](#finding-a-company-by-name)
    * [Listing The Sites And Addresses Of A Legal Entity](#listing-the-sites-and-addresses-of-a-legal-entity)
    * [Keeping Your Own Copy In Sync](#keeping-your-own-copy-in-sync)
    * [Resolving The Technical Keys In A Golden Record](#resolving-the-technical-keys-in-a-golden-record)
    * [Checking Whether A Business Partner Is Still In Use](#checking-whether-a-business-partner-is-still-in-use)
    * [Judging How Well Confirmed A Golden Record Is](#judging-how-well-confirmed-a-golden-record-is)
  * [Nice To Know](#nice-to-know)
    * [A Filter That Matches Nothing Is Not An Error](#a-filter-that-matches-nothing-is-not-an-error)
    * [What The Pool Does Not Tell You](#what-the-pool-does-not-tell-you)
  * [NOTICE](#notice)
<!-- TOC -->

## The Golden Record

A golden record is the network's single agreed description of one real-world business partner.
It exists once, no matter how many companies share data about that partner, and it is maintained by
the golden record process out of everything those companies shared.
Its identity is a Business Partner Number (BPN), and that BPN is what a dataspace participant
exchanges with other participants instead of a name and an address.

The Pool is where golden records live.
It is the single source of truth for them, it issues their BPNs, and for a dataspace participant it
is read-only: you resolve BPNs and read the data behind them, you do not write it.
Data reaches the Pool only through the golden record process, which is what the
[Sharing Member Guide](sharing-member-guide.md) covers.

### The Three Levels

A golden record is not one flat record.
A business partner is described at up to three levels, each with a BPN of its own:

| Level        | BPN  | What it describes                                                          |
|--------------|------|----------------------------------------------------------------------------|
| Legal entity | BPNL | a legal person as a register holds it - name, legal form, identifiers      |
| Site         | BPNS | a location or unit of that legal entity                                    |
| Address      | BPNA | a postal address                                                           |

Every legal entity has exactly one **legal address**, its registered address.
Every site has exactly one **site main address**.
Beyond those, a legal entity or a site may have any number of **additional addresses**.

Sites are optional.
A legal entity may have none at all, and many have none: a company that operates from its registered
address alone is fully described by a legal entity and its legal address.
Where sites exist, they are the company's own statement about its internal structure - a plant, a
warehouse, a distribution centre, but equally a more abstract business unit that the company treats
as one location.
Only the company that owns a site shares it, so sites are described where the company describes
itself and nowhere else.

### Where An Address Belongs

An address belongs either **directly to a legal entity** or **to a site** of that legal entity, and
which of the two it is follows from the address itself:

* `bpnLegalEntity` names the legal entity - every address belongs to one, including the addresses of
  its sites.
* `bpnSite` is set only where the address belongs to a site.
* `addressType` names the role the address plays: `LegalAddress`, `SiteMainAddress`,
  `LegalAndSiteMainAddress` or `AdditionalAddress`.

`LegalAndSiteMainAddress` is one address serving as both the registered address of a legal entity and
the main address of one of its sites; a legal entity has at most one such site.

One address can be a location of more than one site.
The sites of such an address are reported as one `bpnSite` plus `additionalSites`, which lists the
BPNS of the rest; the split between them is the order the sites were attached, not a ranking, so read
the two together as the set of sites at that address.
`addressType` describes the address's role for its legal entity and its sites as a whole: it reads
`SiteMainAddress` where the address is the main address of any one of them.

### Metadata

Golden records do not spell out legal forms, identifier types or administrative areas in free text.
They reference them by a technical key, and metadata is the set of lists those keys come from.
The golden record process provider maintains them and the Pool publishes them, so a key in a golden
record is only readable once you have the list it belongs to:

| Endpoint                             | Resolves                                                                    |
|--------------------------------------|-----------------------------------------------------------------------------|
| `GET /v7/legal-forms`                | `legalForm` of a legal entity                                               |
| `GET /v7/identifier-types`           | the `type` of a legal entity or address identifier                          |
| `GET /v7/administrative-areas-level1`| `administrativeAreaLevel1` of a postal address                              |
| `GET /v7/script-codes`               | the `scriptCode` of a script variant                                        |
| `GET /v7/reason-codes`               | the `reasonCode` of a relation                                              |

Two of them carry more than a name.
An identifier type states per country whether an identifier of that type is mandatory there, and
`GET /v7/identifier-types` is filtered by `businessPartnerType` - `LEGAL_ENTITY` or `ADDRESS`, since
sites carry no identifier types - and optionally by `country`.
A legal form states the country and administrative area it belongs to, and whether it is still
active.

`GET /v7/field-quality-rules/` is not a vocabulary but the country's expectations of the data itself:
per country and field, whether the golden record process treats that field as `MANDATORY`, `OPTIONAL`
or `FORBIDDEN`.
It is what tells you why a golden record from one country carries a field that one from another never
does.

## Getting Access

You do not reach the Pool directly.
The golden record process provider exposes it through an EDC as offers, one per purpose defined in
the BPDM framework agreement, and the offer you negotiated is what decides which endpoints you may
call - a dataspace participant needs exactly one:

| Asset                                   | Lets you                                                  |
|-----------------------------------------|-----------------------------------------------------------|
| `ReadAccessPoolForDataSpaceParticipant` | read participant golden records, their changelog and the metadata |

That is the same surface [What The Pool Shows You](#what-the-pool-shows-you) describes, and the asset
comes with an Open-API document holding exactly those endpoints - the quickest way to see what it is
good for without reading further.
[Access BPDM over EDC](README.md#access-bpdm-over-edc) has the negotiation itself.

## What The Pool Shows You

As a dataspace participant you read the golden records **of dataspace participants**, and the
metadata.
Golden records of business partners that are not dataspace participants are not part of your view at
all - they are neither returned nor reported as withheld, so a BPN that resolves to nothing may
either not exist or belong to a company outside the dataspace.

That is the whole of your surface:

| Endpoint                                      | Returns                                                        |
|-----------------------------------------------|----------------------------------------------------------------|
| `POST /v7/participants/legal-entities/search` | legal entities with their legal address                        |
| `POST /v7/participants/sites/search`          | sites with their main address                                  |
| `POST /v7/participants/addresses/search`      | addresses                                                      |
| `POST /v7/participants/changelog/search`      | when a golden record was created or changed                     |
| the metadata endpoints above                  | the lists a golden record's technical keys come from            |

All four searches are `POST` with a filter body and take `page` and `size` as query parameters, with
`size` capped at 100.
Every filter is optional and an omitted one is ignored, so an empty body pages through everything you
may see.

Participation itself is not a filter and not a field you act on.
Every record these endpoints return belongs to a dataspace participant, which is what the
`isParticipantData` flag on it says; whether a given company is one is not otherwise yours to query.

## Use Cases

### Resolving A BPN You Received

The ordinary case: a BPN arrives in a dataspace exchange and you need the business partner behind it.
Which endpoint you call follows from the BPN's prefix:

* a **BPNL** - `POST /v7/participants/legal-entities/search` with the BPN in `bpnLs`.
  You get the legal entity and its legal address in one response.
* a **BPNS** - `POST /v7/participants/sites/search` with the BPN in `siteBpns`.
  You get the site, its main address and the `bpnLegalEntity` of the legal entity it belongs to.
* a **BPNA** - `POST /v7/participants/addresses/search` with the BPN in `addressBpns`.
  You get the address, its `addressType`, and the `bpnLegalEntity` and `bpnSite` it hangs off.

Each filter takes a list, so a batch of BPNs is one request rather than one per BPN.
A BPN resolves at its own level only: a BPNA is not found by the legal entity search, and looking up
the legal entity behind an address is the second call, on the `bpnLegalEntity` the address reports.

### Finding A Company By Name

Where you hold a name but no BPN, `POST /v7/participants/legal-entities/search` also filters by
`legalName`, and the sites and addresses searches filter by `name` in the same way.

The match is case-insensitive and matches anywhere in the name, so it is a lookup aid rather than an
identification: several legal entities can share a name, and the same company may be registered
under a name your own systems do not use.
Treat the result as candidates, confirm one against its identifiers or its legal address, and store
the BPN.
From then on, resolve by BPN.

### Listing The Sites And Addresses Of A Legal Entity

Where you hold a BPNL and want the structure below it, both lower-level searches filter by parent:

* `POST /v7/participants/sites/search` with the BPNL in `legalEntityBpns` returns that legal entity's
  sites.
* `POST /v7/participants/addresses/search` with the BPNL in `legalEntityBpns` returns **all** of its
  addresses, including those belonging to its sites.
  Add `siteBpns` instead to get the addresses of one site.

A legal entity that returns no sites has none, and its addresses hang off it directly.
Both filters take lists, so the addresses of a set of sites are one request.

### Keeping Your Own Copy In Sync

A golden record changes without you doing anything - another company shares better data, or the
golden record process refines it.
`POST /v7/participants/changelog/search` is what tells you, and it answers for a point in time you
pass in `timestampAfter`: everything that happened since.
Narrow it by `bpns` to the business partners you hold, and by `businessPartnerTypes` -
`LEGAL_ENTITY`, `SITE` or `ADDRESS` - to the levels you care about.

An entry names the `bpn`, its `businessPartnerType`, the `timestamp` and whether the record was
created or updated.
It never says *what* changed, so the changelog tells you which BPNs to fetch again and the searches
above give you the new content.

Store the timestamp of the newest entry you processed and pass it back next time.
The `bpns` filter is capped by the operator, at 100 values by default, so a large portfolio is either
several requests or - simpler - no `bpns` filter at all and the entries you do not hold ignored.

### Resolving The Technical Keys In A Golden Record

Read the metadata lists once and keep them, rather than resolving a key per record.
They change rarely and they are the same for every golden record, so each is a handful of paginated
requests and not a per-lookup cost.
`GET /v7/field-quality-rules/` is the exception in shape: it takes a `country` and returns that
country's rules as a whole, so it is one request per country you deal with.

Re-read them when a key you do not know turns up.
A new legal form or identifier type appears in golden records as soon as the golden record process
provider adds it, and nothing announces it.

### Checking Whether A Business Partner Is Still In Use

Two things on a golden record say this, and they answer different questions.

The `states` say whether it is in use.
A state is a period with a `validFrom` and an open or closed `validTo` and a type of `ACTIVE` or
`INACTIVE`, and the levels state it separately: a legal entity that stopped trading, a site that
closed and an address that is no longer used are three different statements.
Read the state that covers today, not the last one in the list.

The `relations` say whether something took its place.
An `IsReplacedBy` relation whose source is your BPN names, in its target, the business partner that
replaced it - a merger at legal entity level, a plant that moved at site level, a relocation at
address level.
The other types describe the company's position rather than its succession: `IsOwnedBy` and
`IsManagedBy` name the legal entity that owns it or maintains its data, and
`IsAlternativeHeadquarterFor` names another legal entity representing the same company.
`relations` reports every relation the record takes part in at either end, so check which of
`businessPartnerSourceBpnl` and `businessPartnerTargetBpnl` is yours before reading it, and mind the
`validityPeriods`: a relation with a closed period ended.

A legal entity additionally reports where it sits in an ownership structure: `ownershipUltimate`
marks it as the top of one, and `ultimateOwnerBpnl` names that top for everything below it.
The Pool derives `ultimateOwnerBpnl` from the `IsOwnedBy` relations, so it is a conclusion already
drawn for you rather than a chain to walk yourself.

What follows from a succession is your decision.
No data is carried from one golden record to the other, both keep their own BPNs, and nothing in your
own systems is repointed for you.

### Judging How Well Confirmed A Golden Record Is

Every level of a golden record carries `confidenceCriteria`, which says how well confirmed that
record is rather than how complete it looks:

* `sharedByOwner` - the company the record describes shared it itself.
  The strongest of the criteria; a record shared only by third parties is a description from outside.
* `checkedByExternalDataSource` - the data was confirmed against an external source.
* `numberOfSharingMembers` - how many companies shared data about this business partner.
* `confidenceLevel` - the golden record process's own summary of the above.
* `lastConfidenceCheckAt` and `nextConfidenceCheckAt` - when the record was last confirmed and when
  it is due again.

A legal entity also carries `currentness`, the date its data was last indicated to be still current,
and every level carries `createdAt` and `updatedAt`.
Use the confidence criteria to decide how much to lean on a record - whether to accept an address for
a delivery, whether to ask the partner to confirm it - and not as a filter, since a low confidence
level is a statement about how well the record is confirmed and not about whether it is right.

## Nice To Know

Behaviour that is worth knowing but that you do not act on.

### A Filter That Matches Nothing Is Not An Error

The searches reject nothing in a filter.
A malformed BPN, a BPN that does not exist, a BPN of a company outside the dataspace and a name
nobody carries all behave the same way: they match nothing, and the response is an empty page rather
than a bad request.
So a request for ten BPNs that returns eight records has silently dropped two, and it is the returned
BPNs you compare against what you asked for.
BPNs are read case-insensitively, and blank filter values are ignored.

### What The Pool Does Not Tell You

* **Who shared a business partner.** A golden record is the network's record, and the companies behind
  it are deliberately not part of it. `numberOfSharingMembers` is the only trace, and it is a count.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 SAP SE
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
