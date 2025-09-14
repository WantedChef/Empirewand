Here are the repository guidelines in English.
Agent Protocol & Repository Guidelines (empirewand)
This document defines the technical requirements and workflow for all AI agents contributing to this project.
Agent Coordination
 * This protocol is binding for all agents working on this repository.
 * All specialized agent definitions and persona instructions are located in the main agents/ directory.
 * The plugin-dev-orchestra-conductor.md agent is responsible for coordination and distributes all tasks.
Essential Project Directives
This project uses Java 21. The Paper API (Spigot/Minecraft) must be treated as compileOnly and must never be embedded in the final JAR.
Key 'Agent Notes':
 * Only touch files directly relevant to the assigned task.
 * Preserve all existing resource keys and message formats in .properties and .yml files.
 * Never modify the group or version in the Gradle files.
 * Maintain API stability within nl.wantedchef.empirewand.api unless a major version bump is the explicit goal of the task.
Project Structure
 * Java Source: src/main/java/nl/wantedchef/empirewand/**
 * Resources (Config): src/main/resources/ (Contains plugin.yml, config.yml, messages*.properties, spells.yml)
 * Tests: src/test/java/** (JUnit 5 + Mockito)
 * Build Config: build.gradle.kts, gradle.properties, settings.gradle.kts
 * Static Analysis Config: config/checkstyle/checkstyle.xml, config/spotbugs/exclude.xml
Mandatory Build & Verification Workflow
Use the Gradle Wrapper (./gradlew or gradlew.bat on Windows) for all tasks.
 * Build the Plugin (Shaded JAR):
   * Command: ./gradlew shadowJar
   * Output: build/libs/*-all.jar
 * Run Unit Tests:
   * Command: ./gradlew test
   * Targeted tests (if needed): ./gradlew test --tests "*EnergyShieldTest" --tests "*SummonSwarmTest"
 * Static Analysis (Linting):
   * Command: ./gradlew checkstyleMain spotbugsMain
   * Temporary Exception: When focusing on rapid gameplay fixes, SpotBugs may be temporarily skipped using -x spotbugsMain (Checkstyle is always mandatory).
 * Full Verification Build (Required for PR):
   * Command: ./gradlew build (Runs tests, analysis, and shading)
 * Test Coverage Report:
   * Command: ./gradlew jacocoTestReport
   * Output: build/reports/jacoco/test/html
Code Style & Naming Conventions
 * Language: Java 21.
 * Indentation: 4 spaces. Do not use tabs.
 * Packages: All code must reside under nl.wantedchef.empirewand.
 * Files: Public class names must match the filename (e.g., KajCloud.java).
 * Naming:
   * Classes/Interfaces: PascalCase
   * Methods/Fields: camelCase
   * Constants (static final): UPPER_SNAKE_CASE
 * Design: Keep methods small and focused. Make side-effects clear. Prefer constructor injection (DI) for services.
Testing Guidelines (JUnit 5)
 * Frameworks: Use JUnit 5 (@Test) and Mockito (inline) for mocking.
 * Location: Tests must reside under a mirrored package structure (e.g., source .../framework/service/FxService.java \rightarrow test .../framework/service/FxServiceTest.java).
 * Naming: Test files must end with *Test.java.
 * Coverage: Aim for \ge 80\% code coverage on all changed and new code. Verify this locally via jacocoTestReport.
Commit & Pull Request Protocol
 * Commits: Use Conventional Commits (preferred).
   * Prefixes: feat:, fix:, refactor:, docs:, ci:, test:
   * Example: fix(spell): prevent NullPointerException in Teleport logic
 * Pull Requests (PRs) must:
   * Clearly describe the motivation and behavior change (link issues if present).
   * Include new tests for the change, or provide a clear rationale for test gaps.
   * Update resources (like plugin.yml or messages.properties) if user-facing behavior changes.
   * Pass the full CI pipeline (equivalent to ./gradlew build checkstyleMain spotbugsMain).
