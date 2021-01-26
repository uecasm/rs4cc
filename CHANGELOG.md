# Storage for ComputerCraft Changelog

### 1.2.2

- Refined Storage is now an optional dependency as well.
- It seems stable enough to call this a release now.

### 1.2.1

- Fixed a crash introduced in 1.2.0 if the RS Peripheral block is broken while connected to a computer network. 

### 1.2.0

- (Not breaking, unless you try to downgrade) The mod has been renamed from "Refined Storage for ComputerCraft" to just "Storage for ComputerCraft" (due to the following change).  All the blocks have been renamed too, but existing worlds will upgrade cleanly to the new version without losing anything.
- Added experimental support for Applied Energistics 2 ME networks.  The `ae2` API is similar (but not quite identical, due to different feature sets) to the `refinedstorage` API.

### 1.1.0

- **BREAKING**: the peripheral name (as used in `peripheral.wrap`) has changed from `refinedstorage4computercraft:peripheral` to just `refinedstorage`, to make life easier in future.
- But also, if a refined storage peripheral is found attached to the computer (either directly or via wired modem) at computer startup, it now automatically registers the `refinedstorage` API, which means that you can just use it without wrapping it yourself.  (Check if it's `nil` first for robust code.)
    - This won't be available if you add the peripheral after the computer has started, although `peripheral.wrap` will still work in that case.  Just place the peripheral block first, or reboot the computer after placing the peripheral.  You'll also need to manually wrap if you want to refer to two separate peripherals (e.g. for subnetworks).
- Also available if an RS peripheral is found at startup is the built-in `autostock` and `stock` programs (see "help stock" for more details).  This helps to monitor and maintain minimum stock levels of particular items and fluids (an improvement on the Storage Manager in the wiki from 1.0.0).
- `getPatterns`, `getFluidPatterns`, `getItems`, and `getFluids` now have an optional item/fluid description parameter.  If omitted, this will report all items/fluids, as before.  But if you provide a `name` then this will limit the results to items/fluids of that type, making it easier to find items or recipes with distinct NBT data.  (NBT in the parameter is always ignored, if you want to match that then use the single-result methods instead.)
- `getItem` and `getFluid` now have an extra optional parameter `evenIfZero`; when this is true, it will return a non-empty result (with zero count/amount) even when the item isn't in storage, which can be useful if you're wanting other properties such as the display name.
- `scheduleTask`: if `count` is omitted, it now uses the count (if present) of the stack rather than 1.  (Still defaults to 1 if neither are present.)
- Item/fluid tags are only exposed as the `nbt` hash by default now, to avoid potentially problematic exposure and progression breaking of some mods.  Currently, there are server config options to re-enable them, but it's possible it may be removed in a future release.
- Mod config file is now server-only rather than common, since everything is server-side anyway.  You can delete the old config file.
- Added in-game help files.

### 1.0.0

- Initial release for Minecraft 1.16.4
