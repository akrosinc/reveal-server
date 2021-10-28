# revealserver

Core server for the Reveal platform

## Static code analysis

This will scan the system for code using SonarQube

```bash
./gradlew --stacktrace sonarqube -Dsonar.login=d498159b2f7dc035c905f8b810823bf2c295dd33 -Dsonar.host.url=https://sonar-ops.akros.online -Dsonar.dependencyCheck.htmlReportPath=build/reports/dependency-check-report.html
```

## Jacoco

```bash

```

## Dependany Checks

This will generate a report in `build/reports/dependency-check-report.html`

```bash
./gradlew dependencyCheckAnalyze --info
```
