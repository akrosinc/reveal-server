# revealserver

Core server for the Reveal platform

## Static code analysis

This will scan the system for code using SonarQube.

```bash
./gradlew --stacktrace sonarqube -Dsonar.login=??????????????????????????????? -Dsonar.host.url=https://sonar-ops.akros.online -Dsonar.dependencyCheck.htmlReportPath=build/reports/dependency-check-report.html
```

## Jacoco

```bash

```

## Dependany Checks

This will generate a report in `build/reports/dependency-check-report.html`

```bash
./gradlew dependencyCheckAnalyze --info
```
## Swagger
```text
/swagger-ui/index.html?configUrl=/api-docs/swagger-config
```

## Elasticsearch
```
docker run -d --name es762 -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.6.2
```