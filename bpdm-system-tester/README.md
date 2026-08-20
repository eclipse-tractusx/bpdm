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
3. Run the JAR file so the tests will be executed:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

To run against the BPDM deployment on the Catena-X association INT environment, activate the checked-in `int`
profile and supply the credentials of a technical user that has Pool, Gate and Orchestrator permissions:

```bash
SPRING_PROFILES_ACTIVE=int BPDM_INT_CLIENT_ID=<id> BPDM_INT_CLIENT_SECRET=<secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```

Note that the profile has to come from the environment — `--spring.profiles.active` is forwarded to the
Cucumber CLI, not to Spring, and aborts the run. The full release procedure is in the
[end-to-end testing guide](../docs/maintainer/e2e-testing.md).

To run only the fast round-trip smoke scenarios (as the daily CI does), filter by tag:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar --tags @Smoke
```

The JSON report plugin is not enabled by default, because the CI helm-test pod runs on a read-only
filesystem and cannot write it. For a full E2E run at the end of a release cycle, enable the report
explicitly:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar --plugin json:target/cucumber-report.json
```