<p align="center">
  <img src="docs/assets/rsadvanced-icon.png" alt="RS Advanced logo" width="256">
</p>

# RS Advanced

An addon for **Refined Storage 2** that expands storage and automation. Currently adds infinite cobblestone and water cells.

## Features

- **Infinite Cobblestone Cell** — an endless source of cobblestone.
- **Infinite Water Cell** — an endless source of water.

Install a cell in a standard **Disk Drive** connected to a powered RS network. Access its resource through the Grid or automation devices.

Cells need no initial filling and never run out. They also absorb returned resources of their own type without accumulating them. Infinite sources are marked with **∞**; ordinary disks keep their normal storage behavior and priorities.

## Installation

For **Minecraft 1.21.1** with **NeoForge 21.1.256+** or **Fabric Loader 0.17.2+**. Requires Java 21.

Install RS Advanced on both the client and server, together with:

- **Refined Storage 2.0.9** and its required dependencies.
- **Architectury API 13.0.11+**.
- **Fabric API** for the Fabric version.

Use the release JAR matching your loader. Forge is not supported.

## Build

With JDK 21 installed, run `gradlew.bat build` on Windows or `bash gradlew build` on Linux/macOS. Release JARs are in `fabric/build/libs` and `neoforge/build/libs`; use the files without `dev` or `sources` in their names.

See [development notes](docs/DEVELOPMENT.md) and [validation results](VALIDATION.md) for architecture, tests, and known limitations.

Licensed under the [MIT License](LICENSE).
