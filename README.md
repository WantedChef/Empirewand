# EmpireWand

Java 21 PaperMC 1.20.6 plugin scaffold for the Empirewand plugin.

 - Build (recommended): `./gradlew build`
 - Alternative (no install needed): `pwsh tools/gradle.ps1 build`
 - Also works: `gradle build` if Gradle is installed
- Output JAR: `build/libs/empirewand-1.0.0.jar`
- Main class: `com.example.empirewand.EmpireWandPlugin`

## Development
- Requires JDK 21
- Paper API: `io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT`

## Run on server
Place the built JAR into your Paper 1.20.6 server `plugins/` folder and start the server.

## Notes
- Wrapper: This repo includes a lightweight PowerShell helper (`tools/gradle.ps1`) that downloads a local Gradle 8.10.2 distribution and runs builds. Use it if you don’t have admin rights to install Gradle.
- CI: GitHub Actions builds on push/PR and uploads artifacts (`empirewand-artifacts`).
