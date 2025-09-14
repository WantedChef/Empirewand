Scope: Binding technical and workflow rules for all agents contributing to nl.wantedchef.empirewand. This supersedes informal guidance and is enforceable in PR review.

1) Coordination & Ownership

Single source of coordination: agents/plugin-dev-orchestra-conductor.md.

Specialist definitions: live under /agents/ and are selected via agents.md.

Task assignment rule: Exactly one specialist per task; conductor manages any follow‑up handoffs.

2) Language, Runtime & APIs

Java: 21 (LTS).

Minecraft API: Paper API is compileOnly and MUST NOT be shaded or embedded in the final JAR.

Packaging: Shaded artifact produced via Shadow; third‑party libs may be relocated as needed; exclude Paper/Spigot.

3) Project Structure

Java: src/main/java/nl/wantedchef/empirewand/**

Resources: src/main/resources/ (contains plugin.yml, config.yml, messages*.properties, spells.yml)

Tests: src/test/java/** (JUnit 5 + Mockito inline)

Build: build.gradle.kts, gradle.properties, settings.gradle.kts

Static Analysis: config/checkstyle/checkstyle.xml, config/spotbugs/exclude.xml

4) Immutable Repo Directives (MUST)

Touch only files required for the assigned task.

Do not change group or version in Gradle.

Preserve all resource keys and message formats in .properties/.yml.

Maintain API stability in nl.wantedchef.empirewand.api (no breaking changes unless task explicitly allows a major bump with migration notes).

5) Build & Verification Workflow

Use the Wrapper: ./gradlew (gradlew.bat on Windows).

Primary tasks

Build shaded JAR: ./gradlew shadowJar → build/libs/*-all.jar

Unit tests: ./gradlew test

Targeted: ./gradlew test --tests "*EnergyShieldTest" --tests "*SummonSwarmTest"

Static analysis:

Checkstyle (mandatory): ./gradlew checkstyleMain

SpotBugs (preferred): ./gradlew spotbugsMain

Temporary exception during rapid gameplay fixes: -x spotbugsMain (document rationale in PR)

Full verification build (PR‑required): ./gradlew build

Coverage report: ./gradlew jacocoTestReport → build/reports/jacoco/test/html

Shadow/Relocation Guidance (normative)

Do not relocate or shade Minecraft/Paper artifacts.

Consider relocation for conflicting third‑party libraries to avoid classpath clashes.

6) Code Style & Design Rules

Indentation: 4 spaces (no tabs).

Packages: under nl.wantedchef.empirewand.

Filenames: public class name == filename (e.g., KajCloud.java).

Naming: Classes/Interfaces — PascalCase; Methods/Fields — camelCase; Constants — UPPER_SNAKE_CASE.

Design: small focused methods, explicit side‑effects, constructor injection for services, no blocking I/O on main thread.

7) Testing Policy (JUnit 5)

Frameworks: JUnit 5 + Mockito (inline mocking).

Location: mirrored package structure (.../FxService.java → .../FxServiceTest.java).

Filenames: *Test.java.

Coverage: ≥ 80% on changed/new code; verify with Jacoco before PR.

8) Commit & PR Protocol

Conventional Commits (preferred): feat:, fix:, refactor:, docs:, ci:, test:.

Example: fix(spell): prevent NullPointerException in Teleport logic.

PRs must include:

Motivation and behavior change (link issues if applicable).

Tests for the change, or a justified rationale for any gap.

Resource updates (plugin.yml, messages.properties) if user‑visible behavior changed.

Passing CI equivalent to: ./gradlew build checkstyleMain spotbugsMain (or documented SpotBugs exception).

9) Selection & Handoffs (Operational Rules)

Conductor selects the specialist using agents.md signals.

Handoffs are explicit and traceable (reference PRs/issues).

Each task must produce: diff -u patches per file, tests, verify commands, and proposed commit messages.

10) Security