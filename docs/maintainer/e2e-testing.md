# End-to-End Testing

Every BPDM release is validated by running the end-to-end test suite against a real deployment on the association's INT environment and reporting the result into Jira, where the Catena-X test management team tracks it.

This guide covers the maintainer's three duties around that: keeping the test definitions in Jira current, running the suite against the deployed environment, and uploading the test execution.

<!-- TOC -->
* [End-to-End Testing](#end-to-end-testing)
  * [The Test Suite](#the-test-suite)
  * [Jira and Xray](#jira-and-xray)
  * [How Tests Map to Jira](#how-tests-map-to-jira)
  * [1. Upsert the Tests, Then Tag the New Ones](#1-upsert-the-tests-then-tag-the-new-ones)
  * [2. Run the Suite Against INT](#2-run-the-suite-against-int)
  * [3. Upload the Test Execution](#3-upload-the-test-execution)
  * [Relation to the CI Smoke Run](#relation-to-the-ci-smoke-run)
  * [Xray Reference](#xray-reference)
<!-- TOC -->

## The Test Suite

The end-to-end suite is the [system tester](../../bpdm-system-tester/README.md): a Cucumber suite packaged as an executable JAR that shares business partner data through a Gate, waits for the golden record process to complete, and asserts the output.
It knows nothing of the deployment beyond the three API base URLs and a set of client credentials.

The scenarios live as Gherkin feature files under `bpdm-system-tester/src/main/resources/cucumber/`.
**They are the source of truth.** The Jira Test issues are generated from them, not the other way round.

## Jira and Xray

| | |
|---|---|
| Jira project | [CXTPM](https://catena-x.atlassian.net/jira/software/c/projects/CXTPM/list) on `catena-x.atlassian.net` |
| Test management | Xray Cloud |
| API base | `https://xray.cloud.getxray.app/api/v2` |

Both API calls below need a bearer token, obtained from an Xray Cloud API key (client id and secret, issued in the Xray settings of the Jira instance).
The token is valid for 24 hours:

```bash
XRAY_TOKEN=$(curl -s -H "Content-Type: application/json" -X POST \
  --data "{\"client_id\":\"$XRAY_CLIENT_ID\",\"client_secret\":\"$XRAY_CLIENT_SECRET\"}" \
  https://xray.cloud.getxray.app/api/v2/authenticate | tr -d '"')
```

## How Tests Map to Jira

The mapping is carried entirely by tags in the feature files, which Xray reads on both the import and the upload direction.

```gherkin
@CXTPM-1039
Feature: Output Reflects Own Shared Master Data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the legal entity master data produced for it.

  @TEST_CXTPM-1012 @BPDM @Smoke
  Scenario: Legal Entity Master Data In Output
```

| Element                 | Meaning                                                                                                                  |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `@CXTPM-1039` on Feature| The **Test Execution** issue the results are reported into. A bare issue key on the Feature is how Xray routes a Cucumber import into an existing Test Execution instead of creating a new one. Every feature file carries the same key, and it is [replaced once per release](#the-releases-test-execution). |
| `@TEST_CXTPM-<n>`       | The **Test** issue this scenario is. `TEST_` is the Xray Cucumber tag prefix.                                              |
| `@BPDM`                 | Product marker, on every scenario. Tags also become labels on the Test issue.                                              |
| `@Smoke`                | Part of the fast round-trip subset the daily CI run executes.                                                              |
| `#h3. …` comments       | The Test issue's description in Jira wiki markup, kept next to the scenario it documents.                                  |

To find the key in use, read the feature-level tag of any feature file.
The [feature import](#1-upsert-the-tests-then-tag-the-new-ones) ignores it: requirement linking needs an explicit `@REQ_` prefix.

## 1. Upsert the Tests, Then Tag the New Ones

**Any release that added or changed an end-to-end scenario starts here, before the suite is run.**
Every scenario has to resolve to a real Jira Test issue. Push the feature files to Xray to achieve that — never write Tests by hand in the Jira UI.

The push is a **two-pass loop**: pass 1 creates the missing Tests, and their keys must be written into the feature files before pass 2.
Skip the write-back and the next upsert creates *another* Test for the same scenario, leaving the upload nothing to match against — the run goes unreported.

### Pass 1 — upsert

Clear any placeholder tags first.
A tag like `@TEST_CXTPM-XXXX` is worse than no tag: Xray tries to resolve `CXTPM-XXXX`, fails to find it, and errors instead of creating the Test.

```bash
cd bpdm-system-tester/src/main/resources
grep -rl "@TEST_CXTPM-XXXX" cucumber            # find them
# remove those tags, then:
zip -r /tmp/features.zip cucumber

curl -H "Authorization: Bearer $XRAY_TOKEN" \
  -F "file=@/tmp/features.zip" \
  -X POST "https://xray.cloud.getxray.app/api/v2/import/feature?projectKey=CXTPM"
```

Per scenario, Xray then either:

- **Tagged with `@TEST_CXTPM-<n>`** — updates that Test issue with the current specification. This is how a changed scenario propagates: edit the feature file, upsert, done.
- **Untagged** — creates a new Test issue, taking the scenario name as its summary.

### Pass 2 — assign the new keys back to their scenarios

The response carries no mapping: it lists the Tests as bare issue keys, with created and updated mixed together and no scenario names. Read it only to confirm `errors` is empty and that it created as many Tests as there were untagged scenarios.

Recover it yourself, for the scenarios that went in untagged:

1. List the CXTPM Tests newest first — in the Jira UI, or without leaving the terminal:

   ```bash
   curl -s -H "Authorization: Bearer $XRAY_TOKEN" -H "Content-Type: application/json" \
     -X POST https://xray.cloud.getxray.app/api/v2/graphql \
     -d '{"query":"{getTests(jql:\"project=CXTPM ORDER BY created DESC\",limit:20){results{jira(fields:[\"key\",\"summary\"])}}}"}' \
     | jq '.data.getTests.results[].jira'
   ```

   The Tests the upsert created are at the top, each summary being the scenario name it was taken from.
   This is Xray's GraphQL API, so the bearer token from above works — no Jira credential needed. `limit` accepts at most 100.

2. Tag each scenario with its key, on the same line as the `@BPDM` marker:

   ```gherkin
     @TEST_CXTPM-1234 @BPDM
     Scenario: Address Succession Reflected In Output
   ```

3. Commit the feature files; the tags are the mapping.

Two traps, both from Xray's fallback matching for untagged scenarios:

- An untagged scenario whose name matches an existing Test in the project binds to **that** Test instead of creating a new one. Convenient when intended, silently wrong when two scenarios share a name.
- Renaming a scenario that has no tag makes it a new scenario as far as the upsert is concerned, and it gets a second Test issue. Tag first, rename later.

### Verify the loop closed

Run the upsert a second time.
It should create nothing — every scenario now resolves to a Test and only updates it.
Anything still being created means a scenario is untagged, and that scenario would not be reported.

Removing a scenario is the one direction the upsert does not cover: close or deprecate the orphaned Test issue in Jira by hand.

## 2. Run the Suite Against INT

### Build the JAR

```bash
mvn -B -U clean package -pl bpdm-system-tester -am -DskipTests
```

### Run

The INT topology is checked in as the `int` Spring profile (`bpdm-system-tester/src/main/resources/application-int.yml`): the three base URLs and the Central-IDP issuer, with the credentials left to the environment.

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<client id> \
BPDM_INT_CLIENT_SECRET=<client secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar \
  --plugin json:target/cucumber-report.json
```

The credentials are a Central-IDP technical user of the `Cx-Operator` company holding the Pool, Gate and Orchestrator permissions — one user serves all three clients. See [operator API access](environments.md#operator-api-access) for how to create one.

Two things about this invocation are easy to get wrong:

- **Activate the profile through the environment.** `--spring.profiles.active=int` does *not* work: `main` forwards its arguments to the Cucumber CLI, which rejects the unknown option, prints its usage text and runs nothing — a run that looks like it did something and reports no scenarios.
- **Pass the JSON report plugin explicitly.** It is off by default, because the CI helm-test pod cannot write to its read-only filesystem. It is the artifact Xray consumes, so a release run without it produces nothing to upload.

Missing credentials do not fail fast: Spring passes the unresolved placeholder through as a literal and the run dies on the first token request with `401 invalid_client`.

The suite runs on 32 threads unless `--threads <n>` says otherwise, which puts a full run in the twenty-minute range.

### Testing another deployment

For a deployment other than INT — a sharing member's own Gate, a feature branch deployment, STABLE — copy the `int` profile's shape into `application-developer.yml`. That filename is gitignored, so it takes credentials inline without risking them reaching the repository; build the JAR afterwards so the file is packaged, and activate it with `SPRING_PROFILES_ACTIVE=developer`.

Base URLs always follow the ingress paths of that deployment's [`values.yaml`](https://github.com/eclipse-tractusx/bpdm/tree/environments).

### Test data is not cleaned up

Each run leaves its business partner data in the INT deployment permanently — there is no cleanup step and no reset before a release run.
This is accepted: the generator produces records that do not collide, so runs interfere neither with each other nor with data already there.

## 3. Upload the Test Execution

### The release's Test Execution

**Every release gets its own Test Execution issue**, so the evidence for a version stays self-contained.

Before the run:

1. Create a Test Execution issue in [CXTPM](https://catena-x.atlassian.net/jira/software/c/projects/CXTPM/list), named after the version being validated.
2. Replace the feature-level tag in every feature file with the new key, since that tag is what routes the upload:

   ```bash
   cd bpdm-system-tester/src/main/resources/cucumber
   sed -i '' 's/^@CXTPM-[0-9]*$/@CXTPM-<new key>/' *.feature
   ```

3. Commit that change as part of the release cycle.

Each release's execution is recorded by [linking it in that release's check issue](#link-the-execution-in-the-release-check-issue), which is where the history of them lives.

Re-running the suite for the same release adds a further run to each Test rather than overwriting the previous one.

### Upload

```bash
curl -H "Content-Type: application/json" \
     -H "Authorization: Bearer $XRAY_TOKEN" \
     -X POST --data @bpdm-system-tester/target/cucumber-report.json \
  https://xray.cloud.getxray.app/api/v2/import/execution/cucumber
```

Xray matches each scenario to its Test issue by the `@TEST_…` tag and records the runs against the Test Execution named by the feature-level tag.

If a run additionally needs fields set on the execution — a test environment, a fix version, a summary — use the multipart variant, which takes the results alongside an `info.json` in Jira's issue-create format:

```bash
curl -H "Authorization: Bearer $XRAY_TOKEN" \
     -F info=@info.json -F results=@bpdm-system-tester/target/cucumber-report.json \
  -X POST https://xray.cloud.getxray.app/api/v2/import/execution/cucumber/multipart
```

After the import, check in Jira that every scenario in the run mapped to a Test issue.
Scenarios whose tag does not resolve show up unmatched, which is the symptom of a skipped step 1.

### Link the execution in the release check issue

Link the Test Execution in the [release check issue](release-process.md#the-two-tracking-issues) under its testing section, where it backs the end-to-end and regression test items.
It is the evidence the release is judged on, and the release management team signs it off with the issue as a whole — the maintainer does not.

### What the execution records in Jira

Xray does not store a bare pass or fail. The Cucumber JSON carries per-step data and Jira surfaces all of it:

| Data | Shown as |
|---|---|
| Step status | Passed / Failed / Pending / Skipped, per `Given` / `When` / `Then` |
| Error message and stack trace | Inline on the failing step |
| Step duration | Execution time per step |
| Evidence | Anything attached at runtime, as step-level attachments |

The suite exploits the last row: the step definitions attach **every API call** — `uri`, request body and response body as pretty-printed JSON, named `<METHOD> <path>` — via `scenario.attach`, so a failed run shows the exact HTTP exchanges that led to the assertion.

When reading a failure, note that evidence attached from an `@After` hook lands on the step *after* the one that threw.

### Automating the upload

The upload is manual. Moving it into CI has to work around the plain Cucumber endpoint accepting no `testExecKey` or `testEnvironment` parameter: route through the feature-level tag, through the multipart endpoint's `info.json`, or by converting the report to Xray JSON, which takes `testExecutionKey` and `testEnvironments` as fields.

For GitHub Actions, [`mikepenz/xray-action`](https://github.com/mikepenz/xray-action) wraps the import and takes `testExecKey` and `testEnvironments` as workflow inputs; [`xray-maven-plugin`](https://github.com/Xray-App/xray-maven-plugin) covers the conversion route.

## Relation to the CI Smoke Run

The chart ships the system tester as a Helm test hook, and the daily CI run executes only the `@Smoke` subset against the freshly deployed chart.
That is a deployment check, not the release validation: an ephemeral CI deployment, a handful of scenarios, no JSON report.
The release validation is the manual full run described above, against the INT deployment of the release candidate.

## Xray Reference

- [Importing Cucumber Tests — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Importing+Cucumber+Tests+-+REST+v2)
- [Import Execution Results — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Import+Execution+Results+-+REST+v2)
- [Authentication — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Authentication+-+REST+v2)
- [GraphQL API](https://docs.getxray.app/display/XRAYCLOUD/GraphQL+API) and its [schema reference](https://us.xray.cloud.getxray.app/doc/graphql/)
- [Testing using Cucumber in Java](https://docs.getxray.app/display/XRAY/Testing+using+Cucumber+in+Java)

The `TEST_` tag prefix is configurable per Xray instance (**Xray → Settings → Cucumber**).
To confirm the prefix in use, export an existing Test issue to a `.feature` file from the Xray UI and read the tag it writes.

Read only the Xray **Cloud** documentation — it uses the `/api/v2/…` paths of this guide, while Server/DC uses `/rest/raven/1.0/…` and its examples fail against this instance.

---

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2026 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2026 SAP SE
- SPDX-FileCopyrightText: 2023,2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2026 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2026 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2026 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2026 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
