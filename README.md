# revealserver

Core server for the Reveal platform

## Static code analysis

This will scan the system for code using SonarQube.

```bash
./gradlew --stacktrace sonarqube -Dsonar.login=743e90ec3404e42d67132b12648d40c5f896461d -Dsonar.host.url=https://sonar-ops.akros.online -Dsonar.dependencyCheck.htmlReportPath=build/reports/dependency-check-report.html
```

## Jacoco

```bash

```

## Dependany Checks

This will generate a report in `build/reports/dependency-check-report.html`

```bash
./gradlew dependencyCheckAnalyze --info
```
