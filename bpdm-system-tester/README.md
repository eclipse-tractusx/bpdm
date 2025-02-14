# System Tester

This application performs automated end-to-end tests on an existing golden record process.
For this it needs access to a BPDM Gate application in order to share business partner data and compare the resulting golden records with the expected result.

In order to use this application you first need to [install the BPDM applications](../INSTALL.md).
For a local execution you can follow the following steps:

1. Create a JAR file of the bpdm system tester (Execute from project root)
```bash
mvn -B -U clean package -pl bpdm-system-tester -am -DskipTests
```
2. Install and run the BPDM applications locally with authentication
3. Run the JAR file so the tests will be executed:
```bash
java -jar bpdm-system-tester/target/bpdm-system-tester.jar
```