# KubeThaum 1.1: recipes for your modpack

[Русский](recipes-ru.md) · [Research guide](guide-en.md) · [Home](../README.md)

Suppose you want to turn iron into gold, make alchemy cheaper, and change
infernal furnace bonuses. The examples below cover these tasks separately.
Pick the ones you need and put them in `kubejs/server_scripts/thaumaturge_recipes.js`.
You can have several `ServerEvents.recipes` handlers in one file.

Save the file and run `/reload`. If you also added research, reopen the world:
research updates when the world loads. A complete file with all eight recipe
types is available in [07_recipes](../examples/standalone/07_recipes/server_scripts/recipes.js).

## Gold from iron in the crucible

The player throws an iron ingot into the crucible, spends four Ordo, and gets gold.
The recipe unlocks after Thaumaturge's regular metallurgy research.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    '#c:ingots/iron',
    { 'thaumaturge:ordo': 4 }
  )
    .id('kubejs:gold_from_iron')
    .research('thaumaturge:metallurgy')
})
```

The arguments are **result, catalyst, aspects**. Here the catalyst is any ingot
in the `c:ingots/iron` tag. To accept only the vanilla item, use
`'minecraft:iron_ingot'` without `#`.

`kubejs:gold_from_iron` is the new recipe's ID; you can keep it as written.
For another recipe, choose a different name after `kubejs:`, such as
`kubejs:copper_from_iron`. This ID lets you show the recipe in research
and find it for later edits. It does not change the item's name in the game.

To give two ingots, change the result to `'2x minecraft:gold_ingot'`.
If the recipe should always be available, remove `.research(...)`.
For your own research branch, use the ID of the entry you created, as shown
in the [research guide](guide-en.md#unlocking-a-recipe-through-research).

## Gold at the arcane workbench

This version takes four iron ingots in a square, 20 vis, and two Ignis crystals.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.arcane_workbench_shaped(
    'minecraft:gold_ingot',
    ['II', 'II'],
    { I: 'minecraft:iron_ingot' },
    20,
    { 'thaumaturge:ignis': 2 }
  ).id('kubejs:gold_at_arcane_workbench')
})
```

Each string in `['II', 'II']` is a grid row. The next argument says that
`I` stands for an iron ingot. Use a space for an empty slot.
The pattern and ingredients are followed by the vis cost and crystals.

If the layout does not matter, use a shapeless recipe. Here iron and redstone
in any slots become gold for 10 vis:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.arcane_workbench_shapeless(
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:redstone'],
    10
  ).id('kubejs:gold_from_redstone')
})
```

The final crystal argument is omitted, so no crystals are needed.
Both recipe types can require research through `.research(...)`.

## A diamond through infusion

Put a gold ingot in the center, with iron and an emerald on the pedestals.
The infusion spends eight Ordo and turns the central item into a diamond.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.infusion(
    'minecraft:diamond',
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:emerald'],
    { 'thaumaturge:ordo': 8 }
  )
    .instability(2)
    .id('kubejs:diamond_infusion')
})
```

The order is result, central item, pedestal items, aspects.
`.instability(2)` sets instability; values from 0 to 100 are accepted.

For an enchantment, use its Thaumaturge name in place of a result.
For example, this adds Swift for a feather on a pedestal and eight Aer:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.infusion_enchantment(
    'swift',
    'minecraft:iron_boots',
    ['minecraft:feather'],
    { 'thaumaturge:aer': 8 }
  ).id('kubejs:swift_boots')
})
```

The iron boots are the item shown in the recipe. Thaumaturge's enchantment
determines which items can actually receive it.
Use `'swift'`, rather than a Minecraft enchantment ID.

## Transforming blocks with Salis Mundus

A single block can be transformed directly. Here Salis Mundus turns
a placed iron block into a gold block:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_simple(
    'minecraft:gold_block',
    'minecraft:iron_block'
  ).id('kubejs:gilded_iron')
})
```

Use a tag for a whole group of blocks. This recipe works on logs:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_tag(
    'minecraft:gold_block',
    'minecraft:logs'
  ).id('kubejs:gilded_logs')
})
```

For `dust_trigger_tag`, write the block tag **without `#`**. This differs
from ingredients in crucible and workbench recipes.

A structure needs an existing blueprint. For example, the infernal furnace
uses the `thaumaturge:infernal_furnace` blueprint:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_multiblock(
    'thaumaturge:infernal_furnace',
    'thaumaturge:infernal_furnace'
  ).id('kubejs:infernal_furnace')
})
```

The first argument is the result; the second is the blueprint ID.
This example adds a recipe for an existing structure. Define new blueprints
in a data pack.

## Changing an existing recipe

Suppose Alumentum should require less fire. Find its built-in recipe and
reduce Ignis from ten to five, keeping the other aspects and research gate:

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ id: 'thaumaturge:crucible/alumentum' }, recipe => {
    recipe.aspect('thaumaturge:ignis', 5)
  })
})
```

To make alchemy cheaper across the board, reduce all crucible recipe costs:

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ type: 'thaumaturge:crucible' }, recipe => {
    recipe.scaleAspects(0.75)
  })
})
```

Each cost is multiplied by `0.75` and rounded up: eight becomes six, five
becomes four. For arcane workbench recipes, this method changes crystals.
Choose the individual edit or the general multiplier; if you install both
handlers, the changes apply in sequence.

Standard KubeJS replacements work too. For example, use emeralds instead
of diamonds in all existing infusion recipes:

```js
ServerEvents.recipes(event => {
  event.replaceInput(
    { type: 'thaumaturge:infusion' },
    'minecraft:diamond',
    'minecraft:emerald'
  )
})
```

`replaceOutput` similarly replaces a fixed result. To disable Alumentum
crafting altogether, use:

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'thaumaturge:crucible/alumentum' })
})
```

These filters work on recipes loaded from mods and data packs. Configure
your new recipes directly after creating them, as with `.research(...)`
and `.instability(...)` in the earlier examples.

## Infernal furnace bonuses

Let's make raw iron sometimes drop two gold nuggets during smelting.
The main result is still an iron ingot.

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.replaceInfernalBonus(
    event,
    'minecraft:raw_iron',
    'minecraft:gold_nugget',
    2,
    0.5
  )
})
```

After `event`, the arguments are **furnace input, bonus, count, chance**.
`0.5` means a 50% chance. Use the raw iron that goes into the furnace,
rather than the ingot that comes out. The input needs a normal smelting recipe.
Bonuses belong in `generateData` because Thaumaturge stores them separately
from recipes, in a NeoForge data map.

`replaceInfernalBonus` replaces this input's old bonuses. To keep them
and add another, use `infernalBonus`. Here we add an iron nugget bonus
for raw copper and disable bonuses for raw gold:

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.infernalBonus(
    event,
    '#c:raw_materials/copper',
    'minecraft:iron_nugget',
    1,
    0.25
  )
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
```

Inputs and bonuses accept item IDs or tags with `#`; a bonus tag picks
one item at random. Count is from 1 to 64, chance is from 0 to 1.
A bonus tag must contain at least one item. Repeated `infernalBonus` calls
append to the same input's list, up to 64 bonuses. Replacement starts
a new list, and removal clears it. Overlapping tags and item IDs follow
NeoForge's normal data map rules.

## Reference and checking

| What to change | Method after creating a recipe or inside `forEachRecipe` |
| --- | --- |
| One aspect, keeping the others | `.aspect('thaumaturge:ordo', 4)` |
| The whole aspect list | `.aspects({ 'thaumaturge:ordo': 4 })` |
| All costs by a multiplier | `.scaleAspects(0.75)` |
| Workbench crystals | `.crystals({ 'thaumaturge:ignis': 2 })` |
| Workbench vis | `.vis(20)` |
| Infusion instability | `.instability(2)` |
| Catalyst | `.catalyst('minecraft:iron_ingot')` |
| Pedestal items | `.components(['minecraft:iron_ingot', 'minecraft:emerald'])` |
| Result | `.result('minecraft:gold_ingot')` |
| Unlock after research | `.research('thaumaturge:metallurgy')` |
| Unlock from the start of research | `.research('thaumaturge:metallurgy', 0)` |

Methods apply where the recipe type has the corresponding field. Aspect
amounts are positive integers. The native array form is also accepted:
`[{ aspect: 'thaumaturge:ordo', amount: 4 }]`. Vis must be positive;
crystals allow only primal aspects, up to 64 of each.
The `scaleAspects` multiplier must be finite and greater than zero.
The earlier `Thaumaturge.requireResearch` helper is still supported.

Edits preserve untouched fields and extra JSON data. Infusions that upgrade
the central item through `catalyst_patch` retain that upgrade. Item replacements
cover ingredients and fixed results, including pattern symbols and infusion
components. They do not change blueprint cells or enchantment effects.
Special recipes such as Salis Mundus crafting have no configurable ingredient
fields in this API and keep their native behavior.

The Thaumatorium uses crucible recipes. The infernal furnace uses regular
smelting recipes and the bonuses shown above. The essentia smeltery works
with item aspects, and the centrifuge with aspect composition. They have
no separate recipes; these scripts do not change aspect rules or smelting speed.

After `/reload`, try the recipe in the game and check `kubejs/logs/server.log`
if something goes wrong. More messages are available in `logs/latest.log`.
`/kubethaum check` checks research and server-script syntax, but does not
execute recipe or generated-data handlers.
