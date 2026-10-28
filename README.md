# KubeThaum

Customize **Thaumaturge** with KubeJS: write your own research, shape your
modpack's progression, and add or rebalance magical recipes in JavaScript.

[Download](https://github.com/BMProjects-Development/KubeThaum/releases/tag/v1.2-mc1.21.1)
· [Examples](examples)
· [What's changed](changelogs/README.md)
· [Report an issue](https://github.com/BMProjects-Development/KubeThaum/issues)

## What you can do

| Feature | In your modpack |
| --- | --- |
| **Research** | Create your own Thaumonomicon tabs and entries, connect them into a progression tree, or edit existing research. |
| **Stages** | Set item requirements, knowledge rewards, research notes, warp, and additional pages. |
| **Recipes** | Add crucible, arcane workbench, infusion, and Salis Mundus recipes. Adjust costs and ingredients, or lock recipes behind research. |
| **Furnace bonuses** | Choose bonus items, quantities, and chances for the infernal furnace. |
| **Script checking** | Run `/kubethaum check` to find research errors before reopening the world. |

## Getting started

1. Install KubeThaum alongside Thaumaturge, KubeJS, and their dependencies.
2. Pick a guide below or copy an [example](examples). Put research and recipe
   scripts in `kubejs/server_scripts/`, and translations in `kubejs/client_scripts/`.
3. Reopen the world to apply research. For recipe changes, run `/reload`.

KubeThaum adds the scripting tools; your scripts define what appears in the pack.

For this branch, build Thaumaturge from its matching source revision until
it is available on CurseForge. See the [build instructions](CONTRIBUTING.md).

## Guides

| Topic | Русский | English |
| --- | --- | --- |
| Research and progression | [Исследования](docs/guide-ru.md) | [Research](docs/guide-en.md) |
| Recipes and processing | [Рецепты](docs/recipes-ru.md) | [Recipes](docs/recipes-en.md) |
