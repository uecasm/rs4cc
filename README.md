# Storage for ComputerCraft [![](http://cf.way2muchnoise.eu/432182.svg)](https://www.curseforge.com/minecraft/mc-mods/refined-storage-for-computercraft) [![](http://cf.way2muchnoise.eu/versions/432182.svg)](https://www.curseforge.com/minecraft/mc-mods/refined-storage-for-computercraft)

**Provides a new _Refined Storage Peripheral_ block that can interface as a peripheral to ComputerCraft or CC: Tweaked computers and turtles (either directly or via modem).**

This peripheral allows you to perform queries about the contents of your RS system (items, fluids, and crafting patterns for both), start new crafting tasks, query or cancel existing crafting tasks, and even to extract items/fluids to an adjacent inventory/tank.

It is based on (and mostly compatible with) the OpenComputers API that was implemented in MC1.12, so programs using it should not require many changes.

**Provides a new _ME Peripheral_ block that can interface as a peripheral to computers and turtles.**

This peripheral allows you to perform similar sorts of queries and crafting requests on an Applied Energistics 2 ME network.

This is based on (but not quite identical to, since they have different features) the RS API above, so only small changes should be needed to convert programs between the two.

## For Players

The mod can be downloaded from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/refined-storage-for-computercraft) and installed like any other mod.

## For Developers

#### Building

It builds using gradle/gradlew, like most other mods.  There are no special steps.

#### Maven

I'm not currently expecting there would be any reason for someone to base another mod on top of this one, so I haven't published any API or Maven repository.  Let me know if you think there might be some reason to. 

## Contributions

Language files in particular are very welcome -- submit a Pull Request with the new files or other changes.  You can see a list of existing translations [here](src/main/resources/assets/storage4computercraft/lang).

Other suggestions, bug reports, and pull requests are also welcome, but please do your research first!
