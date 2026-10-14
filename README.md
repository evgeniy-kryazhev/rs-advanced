<p align="center">
  <img src="assets/rsadvanced-icon.png" alt="RS Advanced logo" width="256">
</p>

# RS Advanced

An addon for **Refined Storage 2** that expands storage and automation. Includes infinite cobblestone and water cells; datapacks can add more item and fluid sources.

## Features

- **Infinite Cobblestone Cell** — an endless source of cobblestone.
- **Infinite Water Cell** — an endless source of water.
- **Network Anchor** — keeps chunks containing connected RS nodes loaded in its own dimension.

Network Anchors keep connected machines working while you are away. Each dimension needs
its own anchor. Energy use grows with the loaded area; the default limit is 256 chunks.
Loading stops if the network runs out of energy or exceeds the configured limit.

Craft an anchor with a Machine Casing in the center, Advanced Processors in the corners,
and Ender Pearls on the four sides. Its screen shows the state, area, cost and role, and lets
you enable it or display the loaded area. Access follows the network's security settings.

Configure the mod through **Mod Menu → RS Advanced** on Fabric or **Mods → RS Advanced → Config**
on NeoForge. Changes apply after restarting the server or reopening a single-player world.
On multiplayer servers, the server's settings determine gameplay.

Each cell shows a small resource icon in its lower-right corner in inventories. Infinite cells appear in the Refined Storage creative tab. Hold Shift over a cell to view its help tooltip.

Craft an Infinite Storage Part from a 64K Storage Part and a Nether Star. Combine it with a Storage Housing and the cell's resource (a bucket for fluids) to craft a cell.

Install a cell in a standard **Disk Drive** connected to a powered RS network. Access its resource through the Grid or automation devices.

Cells need no initial filling and never run out. They also absorb returned resources of their own type without accumulating them. Infinite sources are marked with **∞**; ordinary disks keep their normal storage behavior and priorities.

## Custom cells

Add more infinite item or fluid cells through datapacks.
See the [lava datapack example](examples/lava-datapack). Add it to your world's `datapacks` directory before opening the world.
Reopen the world or restart the server after changing cell definitions.

## Installation

For **Minecraft 1.21.1** with **NeoForge 21.1.252+** or **Fabric Loader 0.17.2+**. Requires Java 21.

Install RS Advanced on both the client and server, together with:

- **Refined Storage 2.0.9** and its required dependencies.
- **Architectury API 13.0.11+**.
- **Fabric API** for the Fabric version.
- **Cloth Config 15.0.140+** for the Fabric version (bundled with Refined Storage 2.0.9).
- Optional **Mod Menu** on Fabric to access the configuration screen.

Use the release JAR matching your loader. Forge is not supported.

Licensed under the [MIT License](LICENSE).
