# System Tester

This application performs automated end-to-end tests on an existing golden record process.
For this it needs access to a BPDM Gate application in order to share business partner data and compare the resulting golden records with the expected result.

In order to use this application you first need to [install the BPDM applications](../INSTALL.md).
For a local execution you can follow the following steps:

1. Create a JAR file of the bpdm system tester (Execute from project root)
```bash
mvn -B -U clean package -pl bpdm-system-tester -am -DskipTests
```
2. Install and run the BPDM applications, make sure to use the `system-tester` configuration wherever available.
   That includes the [further Gates](../INSTALL.md#further-gates-for-further-sharing-members) of the second and
   third sharing member: a run with no configuration acts for all three, so a stack running only the first Gate
   fails the scenarios that need the others rather than skipping them.
3. Run the JAR file so the tests will be executed:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

To act for fewer sharing members, clear the base-url of the Gates to leave out — that is what marks a member as
absent, and its scenarios are then reported as skipped:
```bash
BPDM_CLIENT_GATE_3_INPUT_BASE_URL= java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

## Running against a deployed environment

Two profiles are checked in: `int` targets the BPDM deployment on the Catena-X association INT environment,
`snapshot` the bpdm-snapshot deployment that runs the development state of `main`. Both sit behind the same
Central-IDP realm and take the same variables — swap the profile name in the commands below.

The tester talks to four clients: Pool, Orchestrator, and one Gate client per side of the Gate API —
`bpdm.client.gate-input` shares input data and drives the sharing process, `bpdm.client.gate-output` reads the
golden record output. The output client defaults to the input client in every setting, so how many credentials
you need depends on the Gate.

A few scenarios need *further* sharing members, each sharing through a Gate of its own. Two of them show what
members see of a golden record they share — the other's master data changes, and the sharing member count
rising. A third is what makes the confidence level itself move, since the count only counts towards that level
from three members on. Each further member takes a pair of clients, `bpdm.client.gate-2-input` /
`gate-2-output` and `bpdm.client.gate-3-input` / `gate-3-output`, configured through the `BPDM_INT_GATE_2_*`
and `BPDM_INT_GATE_3_*` variables below. Naming a Gate is what marks a run as having that member.

Leave them unset and the run acts for a single sharing member: the scenarios tagged `@TwoSharingMembers` and
`@ThreeSharingMembers` are then reported as skipped instead of failing, and the run's exit code stays
unaffected. They are also excluded from the `@Smoke` subset, since each drives two or three golden record round
trips.

**One technical user for everything.** A Central-IDP user holding the Pool, Gate and Orchestrator permissions
serves all four clients; this is how the Gate of the golden record core deployment is tested:

```bash
SPRING_PROFILES_ACTIVE=int BPDM_INT_CLIENT_ID=<id> BPDM_INT_CLIENT_SECRET=<secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

**A user per Gate role.** A Gate whose technical users the Portal manages grants each of them one role only, so
no single credential covers the whole Gate API. Name the two Gate users then; Pool and Orchestrator keep
`BPDM_INT_CLIENT_*`, because the tester writes Pool metadata and acts as the cleaning service against the
Orchestrator, which no sharing member user may do:

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<operator id> BPDM_INT_CLIENT_SECRET=<operator secret> \
BPDM_INT_GATE_INPUT_CLIENT_ID=<input manager id> BPDM_INT_GATE_INPUT_CLIENT_SECRET=<input manager secret> \
BPDM_INT_GATE_OUTPUT_CLIENT_ID=<output consumer id> BPDM_INT_GATE_OUTPUT_CLIENT_SECRET=<output consumer secret> \
BPDM_CLIENT_GATE_INPUT_BASE_URL=https://business-partners.int.catena-x.net/companies/<member> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

The base URL is only needed for a Gate the profile does not already point at, such as a sharing member's own;
the output client follows it. Both Gate users have to belong to the same company — the tester compares the BPNL
in the two tokens before the first scenario and refuses to run on a mismatch, because the Gate would otherwise
answer every output read with an empty page.

**A second sharing member.** Add the Gate and the two users of another sharing member to also run the scenarios
that need two of them. The tester checks before the first scenario that the second member's BPNL differs from
the first's, since two members from one company would neither be told apart by their Gates nor counted as two:

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<operator id> BPDM_INT_CLIENT_SECRET=<operator secret> \
BPDM_INT_GATE_2_BASE_URL=https://business-partners.int.catena-x.net/companies/<other member> \
BPDM_INT_GATE_2_INPUT_CLIENT_ID=<input manager id> BPDM_INT_GATE_2_INPUT_CLIENT_SECRET=<input manager secret> \
BPDM_INT_GATE_2_OUTPUT_CLIENT_ID=<output consumer id> BPDM_INT_GATE_2_OUTPUT_CLIENT_SECRET=<output consumer secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

A third member follows the same shape, with `BPDM_INT_GATE_3_BASE_URL` and the four
`BPDM_INT_GATE_3_*_CLIENT_*` variables; every member has to act for a company of its own.

Locally no such variables are needed: the further Gates of a local installation are checked in as the default
configuration. See [INSTALL.md](../INSTALL.md#further-gates-for-further-sharing-members) for how to run them.

Note that the profile has to come from the environment — `--spring.profiles.active` is forwarded to the
Cucumber CLI, not to Spring, and aborts the run. The full release procedure is in the
[end-to-end testing guide](../docs/maintainer/e2e-testing.md), including
[running through a sharing member's Gate](../docs/maintainer/e2e-testing.md#testing-through-a-sharing-members-gate).

Add the Cucumber JSON report to any of the commands above whose result is to be documented — a release's
end-to-end run is reported into Jira from that file, and a run without it leaves nothing to upload:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar --plugin json:target/cucumber-report.json
```

The plugin is off by default, because the CI helm-test pod runs on a read-only filesystem and cannot write it,
so a run against a deployed environment has to ask for it. The upload is described in
[upload the test execution](../docs/maintainer/e2e-testing.md#3-upload-the-test-execution).

To run only the fast round-trip smoke scenarios (as CI does on a pull request; its nightly run takes the whole suite), filter by tag:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar --tags @Smoke
```