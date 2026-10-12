# KubeThaum 1.1: recipes and processing

[Русский](recipes-ru.md) · [Research guide](guide-en.md) · [Home](../README.md)

Put these JavaScript files in `kubejs/server_scripts/`. Recipe and furnace
bonus changes apply with `/reload`. Research changes still require reopening
the world. Start with the complete [07_recipes example](../examples/standalone/07_recipes/server_scripts/recipes.js).

## Creating recipes

Inside `ServerEvents.recipes`, use `event.recipes.thaumaturge`. Every constructor
returns a KubeJS recipe builder supporting `.id(...)`. Results accept ordinary
KubeJS item strings such as `'2x minecraft:gold_ingot'` or `Item.of(...)`.
Ingredients accept item IDs, tags, and KubeJS ingredients. Aspect costs accept
an object mapping IDs to positive integer amounts, or the native array
`[{ aspect: 'thaumaturge:ordo', amount: 4 }]`.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot', '#c:ingots/iron', { 'thaumaturge:ordo': 4 }
  ).id('pack:gold').research('thaumaturge:metallurgy')
})
```

| Constructor | Arguments, in order |
| --- | --- |
| `crucible` | result, catalyst, aspects |
| `arcane_workbench_shaped` | result, pattern rows, symbol-to-ingredient object, vis, optional crystals |
| `arcane_workbench_shapeless` | result, ingredient array, vis, optional crystals |
| `infusion` | result, catalyst, component array, aspects |
| `infusion_enchantment` | enchantment name, display catalyst, component array, aspects |
| `dust_trigger_simple` | result, target block ID |
| `dust_trigger_tag` | result, target block tag ID without `#` |
| `dust_trigger_multiblock` | result, blueprint ID |

`vis` must be positive. Crystals use the same cost format, allow only primal
aspects, and have a native limit of 64 per aspect. Omitted crystals are empty.
Infusion supports `.instability(0..100)`. Infusion enchantments use Thaumaturge's
names such as `'swift'`, rather than Minecraft enchantment IDs.
Multiblocks reference existing blueprints; define new blueprints in a data pack.
Special recipes such as the built-in Salis Mundus crafting recipe retain their
native handling; they have no configurable ingredient fields in this API.

```js
ServerEvents.recipes(event => {
  const tt = event.recipes.thaumaturge
  tt.arcane_workbench_shaped('minecraft:gold_ingot', ['II', 'II'], {
    I: 'minecraft:iron_ingot'
  }, 20, { 'thaumaturge:ignis': 2 }).id('pack:arcane_gold')

  tt.infusion('minecraft:diamond', 'minecraft:gold_ingot', [
    'minecraft:iron_ingot', 'minecraft:emerald'
  ], { 'thaumaturge:ordo': 8 }).instability(2).id('pack:infused_diamond')
})
```

## Editing and bulk balancing

Use standard KubeJS filters on recipes loaded from mods and data packs.
`forEachRecipe`, `replaceInput`, `replaceOutput`, and `remove` operate on those
loaded recipes. To change a recipe created in the same callback, retain its
returned builder and use its setters.

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ type: 'thaumaturge:crucible' }, recipe => {
    recipe.scaleAspects(0.75)
  })
  event.forEachRecipe({ type: 'thaumaturge:infusion' }, recipe => {
    recipe.instability(1)
  })
  event.replaceInput({ type: 'thaumaturge:infusion' },
    'minecraft:diamond', 'minecraft:emerald')
  event.replaceOutput({ type: 'thaumaturge:crucible' },
    'minecraft:iron_ingot', 'minecraft:copper_ingot')
  // event.remove({ id: 'thaumaturge:some_recipe' })
})
```

- `.aspect(id, amount)` sets one aspect cost and preserves the others. For
  workbench recipes, it changes crystals.
- `.scaleAspects(factor)` scales all aspect costs (or crystals), rounding each
  up. The factor must be finite and positive. Native limits still apply.
- `.aspects({...})` / `.crystals({...})` replace the whole cost list.
- `.vis(amount)`, `.instability(amount)`, `.catalyst(ingredient)`,
  `.components([...])`, and `.result(item)` change the corresponding fields
  where the recipe type supports them.
- `.research(id)` requires completion; `.research(id, progress)` uses native
  progress values starting at zero. The existing
  `Thaumaturge.requireResearch(recipe, id[, progress])` also works.

Untouched fields, research gates, and custom JSON fields survive edits.
Existing infusion recipes that modify catalyst components retain their
`catalyst_patch`; they do not need a fixed result. Standard item filters and
replacements cover item ingredients and fixed outputs, including shaped keys
and infusion components. Blueprint cells and enchantment effects are not item
ingredients in a recipe, so these filters do not rewrite them.

The Thaumatorium uses crucible recipes. The infernal furnace uses ordinary
smelting recipes for its main result. The essentia smeltery and centrifuge use
item aspects and aspect composition, respectively; they have no separate recipe
serializer. This update does not change their aspect rules or smelting speed.

## Infernal furnace bonuses

Configure bonuses in `ServerEvents.generateData('after_mods', ...)`, because
Thaumaturge stores them in a NeoForge data map. Keys identify the **input** to
the furnace. A normal smelting recipe must exist for that input.

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.replaceInfernalBonus(event,
    'minecraft:raw_iron', 'minecraft:gold_nugget', 2, 0.5)
  Thaumaturge.infernalBonus(event,
    '#c:raw_materials/copper', 'minecraft:iron_nugget', 1, 0.25)
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
```

`infernalBonus` adds a bonus alongside lower-priority data, while
`replaceInfernalBonus` replaces existing bonuses for the selected input.
`removeInfernalBonus` removes its bonus list. Inputs and bonus outputs accept
item IDs or tags; an output tag selects a random member. Count is `1..64`, and
chance is `0..1`. Outputs must resolve to at least one item when data loads.
Repeated `infernalBonus` calls append bonuses for the same input (up to 64).
Replacement starts a new list, and removal clears it. Different inputs remain independent. Existing native
data-map merging, tag precedence, client synchronization, and `/reload` apply.

`/kubethaum check` checks research callbacks and server-script syntax. It does
not execute recipe or generated-data callbacks. Check `kubejs/logs/server.log`
and `logs/latest.log` after `/reload` for errors in these callbacks.
