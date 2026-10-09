# KubeThaum: creating your own research

[Русский](guide-ru.md) · [Home](../README.md)

KubeThaum lets you write Thaumaturge research in JavaScript with KubeJS:
add Thaumonomicon entries, create tabs, change existing stages, and unlock
recipes as players progress through your research.

Start with a small research entry, then use the examples below to build your
own progression tree. A complete tab with translations and a gated recipe
is available in [the Workshop example](#the-complete-workshop).

- [Where scripts go](#where-scripts-go)
- [Your first research](#your-first-research)
- [Stage requirements and rewards](#stage-requirements-and-rewards)
- [Research chains](#research-chains)
- [Your own tab](#your-own-tab)
- [Names and translations](#names-and-translations)
- [Unlocking a recipe through research](#unlocking-a-recipe-through-research)
- [Editing existing research](#editing-existing-research)
- [Copying and replacing research](#copying-and-replacing-research)
- [Additional pages and appearance](#additional-pages-and-appearance)
- [The complete Workshop](#the-complete-workshop)
- [Quick reference](#quick-reference)
- [When something does not appear](#when-something-does-not-appear)

## Where scripts go

Install KubeThaum alongside Thaumaturge, KubeJS, Rhino, and Thaumaturge's
dependencies. Version 1.0 targets Minecraft 26.1.2, NeoForge 26.1.2.109 and
later within 26.1.2, KubeJS 26.1.2-8.0.6, and Thaumaturge 1.0.2.
KubeThaum does not add research until you install scripts.

Open your Minecraft instance folder. You need two KubeJS directories:

```text
kubejs/
├── server_scripts/
│   └── my_research.js       ← research, tabs, and recipes
└── client_scripts/
    └── my_research_lang.js  ← Thaumonomicon translations
```

Create any missing folders and plain text files with the `.js` extension.
You can choose your own filenames. Make sure your editor does not save
`my_research.js.txt`. No imports are needed at the top of a script.

Use `ThaumaturgeEvents.research` for research, `ServerEvents.recipes` for
recipes, and `ClientEvents.lang` for translations. The first two belong
in `server_scripts`; the third belongs in `client_scripts`.
For multiplayer, install research and recipe scripts on the server and
include translation scripts in each player's client pack.

**Close and reopen the world after changing research**, or restart your
dedicated server. `/reload` updates recipes but does not reload research.
After changing translations, reload client resources with F3+T; restart
the client when adding a new client script.

## Your first research

Create `kubejs/server_scripts/my_research.js` and paste the **entire** example:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:first_steps')
    .category('thaumaturge:alchemy')
    .title('First Experiments')
    .icon('minecraft:iron_ingot')
    .position(8, 0)
    .stage(stage => {
      stage.text('Bring four iron ingots for your first experiments.')
      stage.obtain('minecraft:iron_ingot', 4)
    })
    .stage(stage => {
      stage.text('You now know where to begin studying metals.')
    })
})
```

Reopen the world and find First Experiments in the Alchemy tab.
The entry has an iron ingot icon and two stages: handing in iron and a
concluding page. Coordinates `(8, 0)` are an example; if the entry overlaps
another one, move it with `position`.

| Line | What it controls |
| --- | --- |
| `create('pack:first_steps')` | The unique research ID |
| `category('thaumaturge:alchemy')` | The Thaumonomicon tab |
| `title('First Experiments')` | The entry's name |
| `icon('minecraft:iron_ingot')` | An icon using an existing item |
| `position(8, 0)` | Column and row on the research grid, not pixels |
| `stage(...)` | The next research stage |
| `text(...)` | Stage text; every new stage needs it |
| `obtain(..., 4)` | A requirement to bring four items |

`pack` is a placeholder namespace for your modpack, and `first_steps` is
the entry's name. An Aurora pack could use `aurora:first_steps`.
Use lowercase Latin characters and no spaces: `aurora:advanced_metals`.
Every reference to an entry must use the same ID.
The script filename does not determine the ID; the displayed title is independent.

Later snippets using `pack:first_steps` assume you have created this entry.
Place edits after its creation in the same handler, or in another handler
that runs later. When splitting scripts into files, filenames such as
`00_research.js` and `10_research_edits.js` help make the order explicit.
Complete examples with their own IDs can be installed separately.

## Stage requirements and rewards

The following blocks replace the body of `.stage(stage => { ... })` in the
first example. A stage can have several requirements; players must meet all of them.

### Bring several types of items

```js
stage.text('Prepare eight iron ingots and four gold ingots.')
stage.obtain('minecraft:iron_ingot', 8)
stage.obtain('minecraft:gold_ingot', 4)
```

`obtain` checks the inventory. Thaumaturge consumes the items when the
stage is completed. Calls add separate requirements, so this example needs
**both iron and gold**. Omitting the amount means one item.

### Accept an item tag or a list of alternatives

```js
stage.text('Bring four planks of any wood.')
stage.obtain('#minecraft:planks', 4)
```

The `#` prefix means an item tag. This also accepts modded planks when
they belong to `minecraft:planks`.

For a specific list of acceptable items:

```js
stage.text('Bring four iron or copper ingots.')
stage.obtainAny(['minecraft:iron_ingot', 'minecraft:copper_ingot'], 4)
```

This creates **one** requirement for a total of four matching items:
iron, copper, or a mixture. Pass direct item IDs to `obtainAny`;
use `obtain('#namespace:tag', amount)` for a tag.

### Require item components

```js
stage.text('Prepare an undamaged iron sword.')
stage.obtain('minecraft:iron_sword', 1, { 'minecraft:damage': 0 })
```

The third argument specifies Minecraft components to match on the item.
Component values must use the format supported by your Minecraft version.

### Require crafting

```js
stage.text('Craft a crafting table.')
stage.craft('minecraft:crafting_table')
```

`craft` uses Thaumaturge's native requirement handling. In Thaumaturge 1.0.2,
a recorded craft counts; holding a matching item can also satisfy the condition.
It is not a counter for “craft N items after starting this stage.”
Use `obtain` when you need players to hand in an exact quantity.
Alternatives use `craftAny(['minecraft:chest', 'minecraft:barrel'], 1)`.

### Require another research within a stage

```js
stage.text('Complete Metallurgy before attempting the next experiment.')
stage.requireResearch('thaumaturge:metallurgy')
```

This gates completion of the current stage. Use `parent`, explained below,
to create a dependency between entries on the research tree.

### Knowledge, research notes, and warp

```js
stage.text('Study an Alchemy theory. This stage will reward an observation.')
stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
stage.knowledge('observation', 'thaumaturge:alchemy', 16)
stage.warp(1)
```

`requireKnowledge` adds a requirement; `knowledge` adds a stage reward.
The knowledge types are `theory` and `observation`. Amounts use raw points:
32 points make one theory, and 16 points make one observation.

In Thaumaturge 1.0.2, theory requirements use research notes, while
observation requirements use the native aspect cost handling.
Do not assume they simply subtract the specified number from a knowledge bar.
Configure `noteAspect` and `complexity` for research that uses notes:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:metal_theory')
    .category('thaumaturge:alchemy')
    .title('Theory of Metals')
    .icon('minecraft:gold_ingot')
    .position(8, 2)
    .complexity(2)
    .noteAspect('thaumaturge:ordo', 3)
    .noteAspect('thaumaturge:ignis', 2)
    .stage(stage => {
      stage.text('Solve a research note about the properties of metals.')
      stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
    })
    .stage(stage => {
      stage.text('Your work has produced new observations.')
      stage.knowledge('observation', 'thaumaturge:alchemy', 16)
    })
})
```

Note complexity can be `1`, `2`, or `3`. `warp(1)` grants warp when the
stage is completed; omit it when you do not want that effect.

## Research chains

Add a second entry that opens after First Experiments.
Place this block after creating `pack:first_steps`, inside the research handler:

```js
event.create('pack:advanced_metals')
  .category('thaumaturge:alchemy')
  .title('Advanced Alloys')
  .icon('minecraft:gold_ingot')
  .position(10, 0)
  .parent('pack:first_steps')
  .stage(stage => {
    stage.text('Bring a gold ingot for the next experiment.')
    stage.obtain('minecraft:gold_ingot')
  })
  .stage(stage => stage.text('Your alloy research is complete.'))
```

`parent(id)` requires full completion of the parent and creates a tree link.
Several calls require **all** listed parents. You can reference built-in
Thaumaturge research, such as `thaumaturge:metallurgy`.

To require only that the parent has been started, use
`.parent('pack:first_steps', 1)`. Parent stage numbers start at **1**:
`1` means started, rather than the first stage being completed.
`hiddenParent('pack:first_steps')` adds a dependency without drawing a parent line.

Avoid circular completion dependencies. If A requires B and B requires A,
players cannot complete either entry.

## Your own tab

This example is self-contained. Save it as
`kubejs/server_scripts/workshop_tab.js`:

```js
ThaumaturgeEvents.research(event => {
  event.category('pack:workshop')
    .style('thaumaturge:alchemy')
    .index(30)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('pack:workshop_intro')
    .category('pack:workshop')
    .title('Welcome to the Workshop')
    .icon('minecraft:crafting_table')
    .position(0, 0)
    .stage(stage => {
      stage.text('Prepare a crafting table for your work.')
      stage.obtain('minecraft:crafting_table')
    })
})
```

`style` takes the icon, background, and overlay from an existing tab,
so you do not need custom textures. It copies appearance only; configure
the knowledge formula and unlock condition separately. `formula` adds
aspects to this category's research table formula; `index` controls tab order.

For the tab name, create `kubejs/client_scripts/workshop_lang.js`:

```js
ClientEvents.lang('ru_ru', event => {
  event.add('research_category.pack.workshop', 'Мастерская')
})

ClientEvents.lang('en_us', event => {
  event.add('research_category.pack.workshop', 'Workshop')
})
```

Tab names use `research_category.<namespace>.<path>`.
For `pack:workshop`, the key is `research_category.pack.workshop`.

To keep the tab locked until Metallurgy is complete, add
`.requireResearch('thaumaturge:metallurgy')` to the category chain.
The research that unlocks a tab must be accessible outside that tab.
To change an existing tab:

```js
ThaumaturgeEvents.research(event => {
  event.editCategory('thaumaturge:alchemy').index(5)
})
```

## Names and translations

For a personal pack, literal strings in `title` and `text` are enough.
For multiple languages, use translation keys. Add this after creating
your first entry to change its name and stage text:

```js
event.edit('pack:first_steps')
  .title('research.pack.first_steps')
  .editStage(0, stage => stage.text('research.pack.first_steps.stage.0'))
  .editStage(1, stage => stage.text('research.pack.first_steps.stage.1'))
```

Add the translations to `kubejs/client_scripts/my_research_lang.js`:

```js
ClientEvents.lang('ru_ru', event => {
  event.add('research.pack.first_steps', 'Первые опыты')
  event.add('research.pack.first_steps.stage.0', 'Принесите четыре железных слитка.')
  event.add('research.pack.first_steps.stage.1', 'Первые опыты завершены.')
})

ClientEvents.lang('en_us', event => {
  event.add('research.pack.first_steps', 'First Experiments')
  event.add('research.pack.first_steps.stage.0', 'Bring four iron ingots.')
  event.add('research.pack.first_steps.stage.1', 'Your first experiments are complete.')
})
```

You choose the keys for `title` and `text`; they must match the keys in
the client file exactly. If a key appears instead of its translation,
check the game language and the client's translation file.

## Unlocking a recipe through research

The book page and the actual recipe have different roles:

- `stage.recipe(id)` displays a recipe on the page.
- `Thaumaturge.requireResearch(recipe, id)` restricts using the recipe.

This complete example adds Metal Transmutation and a crucible recipe.
Save it as `kubejs/server_scripts/transmutation.js`:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:transmutation')
    .category('thaumaturge:alchemy')
    .title('Metal Transmutation')
    .icon('minecraft:gold_ingot')
    .position(8, 4)
    .stage(stage => {
      stage.text('Prepare two iron ingots.')
      stage.obtain('minecraft:iron_ingot', 2)
    })
    .stage(stage => {
      stage.text('Complete this research to unlock the following crucible recipe.')
      stage.recipe('pack:gold_transmutation')
    })
})

ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('pack:gold_transmutation')

  Thaumaturge.requireResearch(recipe, 'pack:transmutation')
})
```

The recipe has its own ID, `pack:gold_transmutation`; use that in
`stage.recipe`. The crafting condition references the **research** ID,
`pack:transmutation`. Until the entry is complete, this recipe is locked.

Do not require crafting an item using a recipe that only unlocks after
this same research is complete: that creates a progression dead end.

To unlock a recipe as soon as research has been started, pass `0` as
the third argument: `Thaumaturge.requireResearch(recipe, 'pack:transmutation', 0)`.
This uses Thaumaturge's native progress value, starting at zero.
For full completion, **omit** the third argument.

The helper supports Thaumaturge's `arcane_workbench_shaped`,
`arcane_workbench_shapeless`, `crucible`, `infusion`, `infusion_enchantment`,
`dust_trigger_simple`, `dust_trigger_tag`, and `dust_trigger_multiblock` recipes.
It does not gate ordinary `minecraft:crafting_shaped` or other mods' recipes.
Thaumaturge defines the format of the remaining recipe fields.

When writing JSON manually, the equivalent field inside `event.custom({...})` is:

```js
research: Thaumaturge.gate('pack:transmutation')
```

`Thaumaturge.gate(id, stage, true)` inverts a progress condition.
Usually `requireResearch` is sufficient; it also checks that the entry exists.

## Editing existing research

`edit` preserves fields you do not touch. Add copper to the first stage
of built-in Metallurgy and a note at the end of the entry:

```js
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.obtain('minecraft:copper_ingot', 8)
    })
    .addendum(page => {
      page.text('This pack also uses copper in metallurgical experiments.')
    })
})
```

The copper requirement is **added** to the original requirements.
To replace only the items players must bring, clear those requirements first:

```js
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy').editStage(0, stage => {
    stage.text('Begin by gathering four copper ingots.')
    stage.clearObtain()
    stage.obtain('minecraft:copper_ingot', 4)
  })
})
```

`clearObtain` does not remove crafting, knowledge, or other requirements.
Use `clearCraft`, `clearRequiredResearch`, `clearRequiredKnowledge`, and
`clearRecipes` for those fields.

To change the stage structure, inside the research handler:

```js
event.edit('pack:first_steps')
  .insertStage(1, stage => {
    stage.text('Between the first stage and the conclusion, prepare a gold ingot.')
    stage.obtain('minecraft:gold_ingot')
  })
```

`editStage(0, ...)` changes the first stage; `insertStage(1, ...)` inserts
before the old second stage; `removeStage(1)` removes the second stage.
**Editing indices start at 0.** `stage(...)` always appends to the end.
Inserting or removing a stage shifts later indices.

Keep entry IDs stable when updating a published pack. Player progress is
stored by ID and stage number; rearranging stages changes the meaning of
existing progress. Text, icons, and additional pages are easier to update.

## Copying and replacing research

Use `copy` to make a variant of an existing entry:

```js
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'pack:copper_metallurgy')
    .title('Copper Metallurgy')
    .position(10, 4)
    .clearIcons().icon('minecraft:copper_ingot')
    .clearParents().parent('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.text('Start a new experiment with four copper ingots.')
      stage.clearObtain().obtain('minecraft:copper_ingot', 4)
    })
})
```

All current fields are copied. Editing the copy does not change the
original, and the new ID has separate player progress. Recipe references,
parents, addendum conditions, and translation keys retain their values;
change them when the copy needs its own. The target ID must be unused.

To rewrite an existing entry completely:

```js
ThaumaturgeEvents.research(event => {
  event.replace('thaumaturge:metallurgy')
    .category('thaumaturge:alchemy')
    .title('This Pack\'s Metallurgy')
    .icon('minecraft:iron_ingot')
    .position(4, 0)
    .stage(stage => {
      stage.text('Metallurgy now begins with gathering eight iron ingots.')
      stage.obtain('minecraft:iron_ingot', 8)
    })
})
```

`replace` starts the entry over: original stages, parents, icons, and
addenda are not preserved. Use `edit` for smaller changes.
The editing and replacement examples affecting the same entry are
alternatives; install the one you want.

## Additional pages and appearance

An addendum appears on completed research. It can require completion
of another entry, which is useful for text revealed later:

```js
ThaumaturgeEvents.research(event => {
  event.edit('pack:first_steps').addendum(page => {
    page.text('Metallurgy has given you a new perspective on your first experiments.')
    page.requireResearch('thaumaturge:metallurgy')
  })
})
```

Addenda also support `recipe(id)` to display recipes.
`clearAddenda()` removes existing addenda before adding replacements.

To change a research entry's shape and icon:

```js
event.edit('pack:first_steps')
  .flag('round')
  .clearIcons()
  .icon('minecraft:gold_ingot')
```

Available Thaumaturge flags are `round`, `spiky`, `reverse`, `hidden`,
`autounlock`, and `hex`. `autoUnlock()` is shorthand for `flag('autounlock')`.
Thaumaturge determines flag behavior; ordinary entries do not need flags.
`icon` appends an icon; `clearIcons` lets you replace the original set.
You can also use a path to an existing `.png` texture instead of an item ID.
A **tab's** `icon` takes a texture path, rather than an item ID.

A stage can display a construction diagram. For a column of two stone
blocks, place this inside `.stage(stage => { ... })`:

```js
stage.text('Example diagram: two stone blocks, one above the other.')
stage.construct(1, 2, 1, [
  'minecraft:stone', // top layer
  'minecraft:stone'  // bottom layer
], [{ aspect: 'thaumaturge:ordo', amount: 2 }])
```

Dimensions are `x, y, z`, and the list must contain exactly `x*y*z` cells.
List layers from top to bottom; within a layer, x comes first, then z.
`''` means an empty cell, and `#namespace:tag` means a tag.
This displays a diagram; it does not register a new multiblock or its recipe.

## The complete Workshop

[06_workshop](../examples/standalone/06_workshop) is a complete example with:

1. A Workshop tab using Alchemy's appearance.
2. Preparing the Workshop: handing in planks and a crafting table.
3. Metal Transmutation: unlocked after preparation and requiring iron.
4. An iron-to-gold crucible recipe, unlocked by completing the second research.
5. English and Russian translations for every title and page.

Copy [workshop.js](../examples/standalone/06_workshop/server_scripts/workshop.js)
to `kubejs/server_scripts/` and
[workshop_lang.js](../examples/standalone/06_workshop/client_scripts/workshop_lang.js)
to `kubejs/client_scripts/`. Restart the client and open your world.
This example uses the `workshop` namespace, so it can coexist with the
`pack:*` tutorial scripts in this guide.

Start by changing the quantities of planks and iron. Then add a third
entry with `.parent('workshop:transmutation')` and its own ID.
If you rename the namespace, update entry IDs, the tab, recipe,
dependencies, and translation keys together.

Other ready-to-use scripts:

| Example | Use it to |
| --- | --- |
| [01_minimal](../examples/standalone/01_minimal/server_scripts/minimal.js) | Add a small entry to an existing tab |
| [02_edit](../examples/standalone/02_edit/server_scripts/edit.js) | Extend a Thaumaturge entry |
| [03_category](../examples/standalone/03_category/server_scripts/category.js) | Create a tab; include its [client translation](../examples/standalone/03_category/client_scripts/category_lang.js) |
| [04_recipe_gate](../examples/standalone/04_recipe_gate/server_scripts/recipe_gate.js) | Add research with a gated crucible recipe |
| [05_copy](../examples/standalone/05_copy/server_scripts/copy.js) | Make a variant of built-in Metallurgy |
| [Full metallurgy example](../examples/server_scripts/research.js) | Use research notes, knowledge, and a conditional addendum; [translations](../examples/client_scripts/research_lang.js) |

Move the desired `.js` files from nested `server_scripts` and `client_scripts`
folders into KubeJS's matching folders. The `examples` folder itself is not executed.

## Quick reference

| Task | Method |
| --- | --- |
| Create / edit / replace | `event.create(id)` / `event.edit(id)` / `event.replace(id)` |
| Copy to a new ID | `event.copy(sourceId, newId)` |
| Check whether an entry exists | `event.exists(id)` |
| Create / edit a tab | `event.category(id)` / `event.editCategory(id)` |
| Name, location, and icon | `title(text)`, `category(id)`, `position(x, y)`, `icon(id)` |
| Parent / parent without a line | `parent(id)` / `hiddenParent(id)` |
| Append / edit / insert / remove a stage | `stage(callback)`, `editStage(index, callback)`, `insertStage(index, callback)`, `removeStage(index)` |
| Clear stages, parents, icons, or addenda | `clearStages()`, `clearParents()`, `clearIcons()`, `clearAddenda()` |
| Text, displayed recipe, research prerequisite | `text(text)`, `recipe(id)`, `requireResearch(id)` |
| Items / alternative items | `obtain(idOrTag, amount)` / `obtainAny(ids, amount)` |
| Crafting / alternative crafts | `craft(idOrTag, amount)` / `craftAny(ids, amount)` |
| Knowledge reward / knowledge requirement | `knowledge(type, category, amount)` / `requireKnowledge(type, category, amount)` |
| Research notes and warp | `noteAspect(id, amount)`, `complexity(1..3)`, `stage.warp(amount)` |
| Additional page | `addendum(callback)` |
| Tab appearance and access | `style(id)`, `icon(texture)`, `background(texture)`, `overlay(texture)`, `noOverlay()`, `index(n)`, `requireResearch(id)`, `noRequiredResearch()` |
| Tab formula | `formula(aspectId, amount)` |
| Gate a recipe | `Thaumaturge.requireResearch(recipe, researchId[, progress])` |

`event.edit(id, research => { ... })` is the callback editing form.
It returns `void`; use `event.edit(id)` for method chaining.
`get(id)` and `getCategory(id)` return detached Gson JSON snapshots:
read the name with `snapshot.get('name').getAsString()`.
Changing a snapshot does not change the entry; use `edit` instead.

For editor autocomplete, copy [kubethaum.d.ts](../types/kubethaum.d.ts)
and [globals.d.ts](../types/globals.d.ts) to `kubejs/types/`, and the
[jsconfig template](../types/jsconfig.example.json) to `kubejs/jsconfig.json`.
For an existing configuration, add `types/**/*.d.ts` to `include`.
The game does not need these files. If another declaration set already
defines `ThaumaturgeEvents` and `Thaumaturge`, do not include `globals.d.ts` again.

## When something does not appear

| Symptom | What to check |
| --- | --- |
| Changes do not appear | Reopen the world; `/reload` does not reload research |
| An entry is missing from its tab | Check its category, coordinates, parents, and the tab's unlock condition |
| Translation keys appear as text | Check `ClientEvents.lang`, game language, and the client file |
| Old requirements remain | Methods append requirements; use the matching `clear...` method first |
| Duplicate ID error | Do not install two copies of one example; use `edit` to change an entry |
| A recipe is displayed but not locked | Add `Thaumaturge.requireResearch` in the recipe event |
| A parent or item cannot be found | Check its namespace and ID; installing a mod does not guarantee a particular ID exists |

Look for script errors in `kubejs/logs/server.log` and `logs/latest.log`.
For example, `Research 'pack:first_steps', stage[0], field 'obtain'` points
to the item requirement on this entry's **first** stage.
Start with one working example and make one change at a time, so you can
see which line affected the result.
