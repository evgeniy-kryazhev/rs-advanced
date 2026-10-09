<p align="center">
  <img src="docs/assets/rsadvanced-icon.png" alt="RS Advanced logo" width="256">
</p>

# RS Advanced

An addon for **Refined Storage 2** that expands storage and automation. Includes infinite cobblestone and water cells; datapacks can add more item and fluid sources.

## Features

- **Infinite Cobblestone Cell** — an endless source of cobblestone.
- **Infinite Water Cell** — an endless source of water.
- **Network Anchor** — keeps chunks containing connected RS nodes loaded in its own dimension.

An enabled Network Anchor costs `80 + N × (N + 1) / 2` FE per tick for `N` unique
network chunks. Anchors in the same network and dimension share the area: the anchor with
the lowest X, then Y, then Z pays, and the others serve as reserves. A separate dimension
needs its own anchor. Insufficient power immediately releases the area. The default limit
is 256 chunks; exceeding it stops the entire area.

Craft an anchor with a Machine Casing in the center, Advanced Processors in the corners,
and Ender Pearls on the four sides. Its screen shows the state, area, cost and role, and lets
you enable it or show the area's white contour with translucent blue faces within 64 blocks.
Adjacent chunks form one area without internal walls or intermediate chunk divisions.
The screen uses the vanilla Minecraft panel and buttons. Opening uses RS `OPEN` permission;
changing enabled state uses `BUILD`.

The shared mod configuration `config/rsadvanced.json` controls `baseCost` (80), `chunkCostMultiplier` (1), `maxChunks`
(256), and `randomTicks` (true). Restart the server to apply changes. Random ticks follow
`randomTickSpeed`; the anchor does not create additional natural mob spawning. On restart,
the last paid area is restored for at most 100 ticks while the RS graph initializes.

Each cell shows a small resource icon in its lower-right corner in inventories. Infinite cells appear in the Refined Storage creative tab. Hold Shift over a cell to view its help tooltip.

Craft an Infinite Storage Part from a 64K Storage Part and a Nether Star. Combine it with a Storage Housing and the cell's resource (a bucket for fluids) to craft a cell.

Install a cell in a standard **Disk Drive** connected to a powered RS network. Access its resource through the Grid or automation devices.

Cells need no initial filling and never run out. They also absorb returned resources of their own type without accumulating them. Infinite sources are marked with **∞**; ordinary disks keep their normal storage behavior and priorities.

## Custom cells

Add item or fluid definitions and ordinary crafting recipes through a datapack — no Java changes,
compilation or datagen needed. Names use the resource’s client translation automatically.
See the [lava datapack example](docs/examples/lava-datapack) and [setup instructions](docs/DEVELOPMENT.md).
Reopen the world or restart the server after changing cell definitions.

## Installation

For **Minecraft 1.21.1** with **NeoForge 21.1.252+** or **Fabric Loader 0.17.2+**. Requires Java 21.

Install RS Advanced on both the client and server, together with:

- **Refined Storage 2.0.9** and its required dependencies.
- **Architectury API 13.0.11+**.
- **Fabric API** for the Fabric version.

Use the release JAR matching your loader. Forge is not supported.

## Build

With JDK 21 installed, run `gradlew.bat build` on Windows or `bash gradlew build` on Linux/macOS. Release JARs are in `fabric/build/libs` and `neoforge/build/libs`; use the files without `dev` or `sources` in their names.

See [development notes](docs/DEVELOPMENT.md) and [validation results](VALIDATION.md) for architecture, tests, and known limitations.

Licensed under the [MIT License](LICENSE).
