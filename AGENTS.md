# Repository Guidelines

## Required Agent Skills

Before Java or Minecraft development, debugging, or review, read and apply `java-pro`, `minecraft-multiloader`, and `minecraft-modding` from the available skill catalog. Preserve this repository's pinned versions; newer skill examples are not upgrade instructions.

## Project Structure & Module Organization

This Architectury addon targets Java 21, Minecraft 1.21.1, and Refined Storage 2.0.9 on Fabric and NeoForge.

- `common/src/main/java/dev/rsadvanced`: shared features, configuration, networking, client rendering, and mixins.
- `fabric` / `neoforge`: loader bootstrap, capabilities, chunk tickets, and client registration.
- `common/src/main/resources`: models, textures, translations, recipes, and cell definitions.
- `common/src/test`: JUnit tests; `common/src/gameTest`: development-only integration tests and fixtures.
- `examples/lava-datapack`: external datapack example used by tests; `assets`: README artwork.

## Build, Test, and Development Commands

Run from the repository root:

- `./gradlew.bat build`: builds both loaders and runs unit tests and GameTest.
- `./gradlew.bat :common:test`: runs JUnit tests only.
- `./gradlew.bat :fabric:runGameTest :neoforge:runGameTest`: checks both loaders sequentially.
- `./gradlew.bat :neoforge:runClient`: launches development Minecraft; substitute `fabric` as needed.
- Run `:<loader>:runServerValidation` and `:<loader>:runClientValidation` in separate terminals for connected client checks.

On Linux/macOS, use `bash gradlew`. Use `--offline` only with cached dependencies. Release JARs live in `<loader>/build/libs`; exclude `dev` and `sources` artifacts.

## Coding Style & Naming Conventions

Use four-space indentation, descriptive camelCase members, PascalCase classes, and UPPER_SNAKE_CASE constants. Resource IDs use `rsadvanced:snake_case`. Write explicit control flow and error handling; separate logical operations and comment non-obvious invariants. Match existing formatting; no formatter or linter is configured. Keep loader APIs out of `common` and client classes off dedicated servers.

Format tooltips for addon machine blocks, including the Network Anchor, consistently with equivalent machine blocks in the main Refined Storage mod. Follow the pinned RS version's tooltip structure, text styles, colors, line wrapping, and description display behavior; inspect an equivalent RS tooltip before implementing or changing one.

## Testing Guidelines

Use JUnit 5 (`*Test.java`) and Minecraft GameTest. No percentage coverage gate exists; test meaningful behavior and regressions on both loaders. GUI changes require actual server-driven menu opening, not just direct screen previews. Persistence changes require sequential `runAnchorRestartSeed`, `runAnchorRestartCheck`, `runAnchorRestartEmpty`, and `runAnchorRestartNorandom` scenarios per loader. Reports and screenshots belong in `build/reports`.

## Architecture & Configuration

When adding a new storage cell, add both its storage part and its storage block, following the equivalent cells and blocks in the pinned Refined Storage version. A new storage cell must include both forms.

Use `config/rsadvanced.toml` with AutoConfig/Cloth Config on Fabric and `config/rsadvanced-common.toml` with native `ModConfigSpec` on NeoForge. Both provide a validated, immutable `RSAdvancedConfig` snapshot at server startup; settings require restarting the server or reopening the world. JSON is not migrated. Keep world mutations server-side and enforce RS `OPEN`/`BUILD` permissions. Register NeoForge menu screens in `RegisterMenuScreensEvent` and Fabric menu screens during client setup. Configuration screens use Mod Menu on Fabric and `IConfigScreenFactory` on NeoForge. Preserve energy accounting and independent ticket ownership.

## Commit & Pull Request Guidelines

Use Git Flow for new features: create a `feature/<descriptive-name>` branch from `develop`, implement and validate the feature there, and merge it back into `develop` through a pull request. Do not develop features directly on `develop` or the release branch.

Prefer existing `feat:`, `fix:`, `chore:`, and `ci:` prefixes with imperative summaries. Describe the problem, resulting behavior, loader validation, and relevant issue links; include screenshots for visual changes. Exclude generated builds, reports, caches, and local notes.
