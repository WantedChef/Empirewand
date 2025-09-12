# Repository Guidelines

## Project Structure & Modules
- Source: `src/main/java/nl/wantedchef/empirewand/**`
- Resources: `src/main/resources/` (e.g., `plugin.yml`, `config.yml`, `messages*.properties`, `spells.yml`)
- Tests: `src/test/java/**` (JUnit 5 + Mockito)
- Build config: `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`
- Static analysis: `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`

## Build, Test, and Development
- Build shaded plugin jar: `./gradlew shadowJar` (outputs `build/libs/*-all.jar`)
- Full build + checks: `./gradlew build` (runs tests, shading)
- Unit tests: `./gradlew test`
- Lint/analysis: `./gradlew checkstyleMain spotbugsMain`
- Coverage report: `./gradlew jacocoTestReport` (HTML in `build/reports/jacoco/test/html`)
- Quick local run on Windows: use `gradlew.bat` equivalents.

## Approved CLI Commands (Session)
- `./gradlew.bat check`, `./gradlew.bat build`, `./gradlew.bat test`
- Targeted tests: `./gradlew.bat test --tests "*EnergyShieldTest" --tests "*SummonSwarmTest" --tests "*AsyncCommandExecutorTest"`
- Coverage: `./gradlew.bat jacocoTestReport`
- Static analysis: `./gradlew.bat checkstyleMain spotbugsMain` (temporarily skip via `-x spotbugsMain` when focusing on gameplay fixes)

## Coding Style & Naming
- Java 21; 4‑space indentation; no tabs.
- Packages under `nl.wantedchef.empirewand`. Public class name must match filename (e.g., `KajCloud.java`).
- Classes: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.
- Keep methods small, side‑effect clear; prefer constructor or DI for services.
- Run `checkstyle` and `spotbugs` before pushing.

## Testing Guidelines
- Frameworks: JUnit 5 (`@Test`), Mockito (inline). Place tests under mirrored packages, e.g., `src/test/java/.../framework/service/FxServiceTest.java`.
- Name tests `*Test.java`; use descriptive method names.
- Aim for ≥80% coverage on changed code; verify via `jacocoTestReport`.

## Commit & Pull Requests
- Prefer Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `ci:` (seen in history). Example: `fix(spell): prevent NPE in Teleport`.
- PRs must:
  - Describe motivation and behavior change; link issues.
  - Include tests or rationale for test gaps.
  - Update resources/configs (`plugin.yml`, messages) when behavior changes.
  - Pass `./gradlew build checkstyleMain spotbugsMain`.

## Security & Configuration
- Do not commit server tokens or proprietary configs.
- `plugin.yml` placeholders are expanded during `processResources`; keep `name` and `version` managed by Gradle.
- Keep Paper API as `compileOnly`; do not embed server jars.

## Agent Notes
- Touch only related files; preserve resource keys and message formats.
- Avoid changing group/version in Gradle.
- Keep APIs stable under `nl.wantedchef.empirewand.api` unless a major version bump is intended.
