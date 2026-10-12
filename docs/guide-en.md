# KubeThaum: creating your own research

[Русский](guide-ru.md) · [Recipes](recipes-en.md) · [Home](../README.md)

Suppose your pack lets players turn iron into gold in a crucible, after
they have studied metals. We'll add the research, give it a few requirements,
and unlock the recipe when the player finishes it.

Start with one file, then add another entry, change built-in Metallurgy,
or make a separate tab. For a complete version with translations, use
[the Workshop example](#the-complete-workshop).

<details>
<summary>Contents</summary>

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
- [Checking scripts with a command](#checking-scripts-with-a-command)
- [When something does not appear](#when-something-does-not-appear)

</details>

## Where scripts go

Install KubeThaum alongside Thaumaturge, KubeJS, Rhino, and Thaumaturge's
dependencies.
KubeThaum does not add research until you install scripts.

Open your Minecraft instance folder. You need two KubeJS directories:

```text
kubejs/
├── server_scripts/
│   └── metallurgy.js       ← research, tabs, and recipes
└── client_scripts/
    └── metallurgy_lang.js  ← Thaumonomicon translations
```

Create any missing folders and plain text files with the `.js` extension.
You can choose your own filenames. Make sure your editor does not save
`metallurgy.js.txt`. No imports are needed at the top of a script.

Use `ThaumaturgeEvents.research` for research, `ServerEvents.recipes` for
recipes, and `ClientEvents.lang` for translations. The first two belong
in `server_scripts`; the third belongs in `client_scripts`.
For multiplayer, install research and recipe scripts on the server and
include translation scripts in each player's client pack.

| What changed | How to apply it |
| --- | --- |
| Research and tabs | Reopen the world or restart the dedicated server |
| Recipes | Run `/reload` |
| Text in an existing client script | Reload resources with F3+T |
| A new client script | Restart the client |

Before reopening the world, run [`/kubethaum check`](#checking-scripts-with-a-command)
to check the saved research scripts without applying their changes.

## Your first research

Create `kubejs/server_scripts/metallurgy.js` and paste the **entire** example:

```js
ThaumaturgeEvents.research(event => {
  event.create('kubejs:ironworking')
    .category('thaumaturge:alchemy')
    .title('Ironworking')
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

Reopen the world and find Ironworking in the Alchemy tab.
The entry has an iron ingot icon and two stages: handing in iron and a
concluding page. Coordinates `(8, 0)` are an example; if the entry overlaps
another one, move it with `position`.

| Line | What it controls |
| --- | --- |
| `create('kubejs:ironworking')` | The unique research ID |
| `category('thaumaturge:alchemy')` | The Thaumonomicon tab |
| `title('Ironworking')` | The entry's name |
| `icon('minecraft:iron_ingot')` | An icon using an existing item |
| `position(8, 0)` | Column and row on the research grid, not pixels |
| `stage(...)` | The next research stage |
| `text(...)` | Stage text; every new stage needs it |
| `obtain(..., 4)` | A requirement to bring four items |

`kubejs:ironworking` is this entry's internal name. You can leave it as it is:
`kubejs:` is a convenient prefix for your pack's additions. Give the next
entry a different name, such as `kubejs:goldworking`. IDs use lowercase Latin
letters and no spaces. The name players see in the book comes from `title`.

Later snippets using `kubejs:ironworking` assume you have created this entry.
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
use `obtain('#minecraft:planks', 4)` for a tag.

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

`craft` uses Thaumaturge's native requirement handling. A recorded craft counts;
holding a matching item can also satisfy the condition.
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

Theory requirements use research notes, while
observation requirements use the native aspect cost handling.
Do not assume they simply subtract the specified number from a knowledge bar.
Configure `noteAspect` and `complexity` for research that uses notes:

```js
ThaumaturgeEvents.research(event => {
  event.create('kubejs:metal_theory')
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

Add a second entry that opens after Ironworking.
Place this block after creating `kubejs:ironworking`, inside the research handler:

```js
event.create('kubejs:goldworking')
  .category('thaumaturge:alchemy')
  .title('Working with Gold')
  .icon('minecraft:gold_ingot')
  .position(10, 0)
  .parent('kubejs:ironworking')
  .stage(stage => {
    stage.text('Bring a gold ingot for the next experiment.')
    stage.obtain('minecraft:gold_ingot')
  })
  .stage(stage => {
    stage.text('You are ready to move from iron to experiments with gold.')
  })
```

`parent(id)` requires full completion of the parent and creates a tree link.
Several calls require **all** listed parents. You can reference built-in
Thaumaturge research, such as `thaumaturge:metallurgy`.

To require only that the parent has been started, use
`.parent('kubejs:ironworking', 1)`. Parent stage numbers start at **1**:
`1` means started, rather than the first stage being completed.
`hiddenParent('kubejs:ironworking')` adds a dependency without drawing a parent line.

Avoid circular completion dependencies. If A requires B and B requires A,
players cannot complete either entry.

## Your own tab

This example is self-contained. Save it as
`kubejs/server_scripts/workshop_tab.js`:

```js
ThaumaturgeEvents.research(event => {
  event.category('kubejs:alchemy_workshop')
    .style('thaumaturge:alchemy')
    .index(30)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('kubejs:workshop_tools')
    .category('kubejs:alchemy_workshop')
    .title('Alchemy Tools')
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
  event.add('research_category.kubejs.alchemy_workshop', 'Алхимическая мастерская')
})

ClientEvents.lang('en_us', event => {
  event.add('research_category.kubejs.alchemy_workshop', 'Alchemy Workshop')
})
```

The tab `kubejs:alchemy_workshop` needs the translation key
`research_category.kubejs.alchemy_workshop`: add `research_category.` before
the ID and replace the colon with a dot.

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
event.edit('kubejs:ironworking')
  .title('research.kubejs.ironworking')
  .editStage(0, stage => {
    stage.text('research.kubejs.ironworking.stage.0')
  })
  .editStage(1, stage => {
    stage.text('research.kubejs.ironworking.stage.1')
  })
```

Add the translations to `kubejs/client_scripts/metallurgy_lang.js`:

```js
ClientEvents.lang('ru_ru', event => {
  event.add('research.kubejs.ironworking', 'Обработка железа')
  event.add('research.kubejs.ironworking.stage.0', 'Принесите четыре железных слитка.')
  event.add('research.kubejs.ironworking.stage.1', 'Теперь можно переходить к опытам с другими металлами.')
})

ClientEvents.lang('en_us', event => {
  event.add('research.kubejs.ironworking', 'Ironworking')
  event.add('research.kubejs.ironworking.stage.0', 'Bring four iron ingots.')
  event.add('research.kubejs.ironworking.stage.1', 'You are ready to experiment with other metals.')
})
```

You choose the keys for `title` and `text`; they must match the keys in
the client file exactly. If a key appears instead of its translation,
check the game language and the client's translation file.

## Unlocking a recipe through research

The book page and the actual recipe have different roles:

- `stage.recipe(id)` displays a recipe on the page.
- `.research(...)` on the recipe itself restricts using it.

This complete example adds Metal Transmutation and a crucible recipe.
Save it as `kubejs/server_scripts/transmutation.js`:

```js
ThaumaturgeEvents.research(event => {
  event.create('kubejs:metal_transmutation')
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
      stage.recipe('kubejs:gold_transmutation')
    })
})

ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    'minecraft:iron_ingot',
    { 'thaumaturge:ordo': 5 }
  )
    .id('kubejs:gold_transmutation')
    .research('kubejs:metal_transmutation')
})
```

The recipe has its own ID, `kubejs:gold_transmutation`; use that in
`stage.recipe`. The crafting condition references the **research** ID,
`kubejs:metal_transmutation`. Until the entry is complete, this recipe is locked.

Do not require crafting an item using a recipe that only unlocks after
this same research is complete: that creates a progression dead end.

To unlock the recipe when the player starts the entry, change the last line
to `.research('kubejs:metal_transmutation', 0)`. To require full completion,
leave it as `.research('kubejs:metal_transmutation')`.

Research gates support Thaumaturge's `arcane_workbench_shaped`,
`arcane_workbench_shapeless`, `crucible`, `infusion`, `infusion_enchantment`,
`dust_trigger_simple`, `dust_trigger_tag`, and `dust_trigger_multiblock` recipes.
They do not lock ordinary `minecraft:crafting_shaped` or other mods' recipes.
Thaumaturge defines the format of the remaining recipe fields.

If you already have a recipe written with `event.custom`, the existing
`Thaumaturge.requireResearch` helper still works. For new recipes, adding
`.research(...)` directly is easier. More recipes are covered in the
[recipe guide](recipes-en.md).

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
  event.edit('thaumaturge:metallurgy')
    .editStage(0, stage => {
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
event.edit('kubejs:ironworking')
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
  event.copy('thaumaturge:metallurgy', 'kubejs:copper_metallurgy')
    .title('Copperworking')
    .position(10, 4)
    .clearIcons()
    .icon('minecraft:copper_ingot')
    .clearParents()
    .parent('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.text('Start a new experiment with four copper ingots.')
      stage.clearObtain()
      stage.obtain('minecraft:copper_ingot', 4)
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
  event.edit('kubejs:ironworking').addendum(page => {
    page.text('Metallurgy has given you a new perspective on your first experiments.')
    page.requireResearch('thaumaturge:metallurgy')
  })
})
```

Addenda also support `recipe(id)` to display recipes.
`clearAddenda()` removes existing addenda before adding replacements.

To change a research entry's shape and icon:

```js
event.edit('kubejs:ironworking')
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
`''` means an empty cell; `#minecraft:logs` accepts any block in the logs tag.
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
The Workshop has its own IDs, such as `kubejs:workshop_preparation`, so it
can run alongside Ironworking from the start of this guide.

Start by changing the quantities of planks and iron. Then add a third
entry with `.parent('kubejs:workshop_transmutation')` and its own ID.
If you rename an entry, update references to it in `parent`, `.research(...)`,
and the translation file as well.

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
| Gate a recipe | `.research('kubejs:ironworking')` in the recipe chain |

For ordinary edits, use `event.edit('thaumaturge:metallurgy')` followed by
the methods you need. `get(id)` and `getCategory(id)` read copies of the
data; use `edit` to change an entry and `editCategory` to change a tab.

For recipe creation, bulk balancing, and infernal furnace bonuses, see
the [recipe guide](recipes-en.md).

## Checking scripts with a command

With the world open, save your changes in the editor and run:

```text
/kubethaum check
```

The command requires operator permission level 2 or higher and also works
from the server console. Singleplayer requires commands to be enabled.
It reads **fresh files from disk**; you do not need to run `/reload` or
reopen the world first.

For example, accidentally writing `stage.obtain('minecraft:iron_ingot', 0)`
produces a message similar to:

```text
kubejs/server_scripts/metallurgy.js:12: Research 'kubejs:ironworking', stage[0], field 'obtain': Item amount must be positive
```

`metallurgy.js:12` identifies the file and call line, `kubejs:ironworking`
identifies the entry, `stage[0]` is its first stage, and `obtain` is the
invalid requirement. Fix the amount, save the file, and run the command again.

<details>
<summary>What it checks and where its limits are</summary>

The command checks:

- Syntax of server scripts in `kubejs/server_scripts`, including subfolders.
- `ThaumaturgeEvents.research` callbacks against detached data.
- Builder errors: amounts, complexity, stage indices, and required text.
- Duplicate IDs, missing categories and research, and dependency cycles.
- Item, aspect, and tag references and data formats using KubeThaum's
  validators and Thaumaturge's native codecs.

Execution errors usually point to the exact call. Final structure or native
codec errors may point to a related declaration instead; the message also
includes the entry ID and reason. The first eight problems appear in chat;
the complete list is written to `logs/latest.log`.
KubeJS file priorities and properties, including `ignored`, `requires`,
and `packmode`, use the same loading rules as the game.

**The check does not apply changes to the world.** It does not unlock research,
change player progress, replace live KubeJS handlers, or update recipes.
After a successful check, reopen the world to apply research normally.

The scope is limited: client and startup scripts are not checked.
`ServerEvents` callbacks, including recipes, are not executed; their syntax
is checked with the rest of the file. The temporary context provides
JavaScript and the research API, but not `Java`, `global`, `Item`, the server,
or other game bindings. If research requires one of these, the command
reports a failed or incomplete check; that alone does not mean the script
is invalid in the game. Data generated by other events is used as already loaded.

A successful result therefore means “no errors in the checked scope,”
rather than validation of the entire pack. Execution has instruction and
time limits to stop loops. Very large script sets are not supported:
the limits are 256 files, 256 KiB per file, and 4 MiB in total.

</details>

## When something does not appear

| Symptom | What to check |
| --- | --- |
| Changes do not appear | Reopen the world; `/reload` does not reload research |
| An entry is missing from its tab | Check its category, coordinates, parents, and the tab's unlock condition |
| Translation keys appear as text | Check `ClientEvents.lang`, game language, and the client file |
| Old requirements remain | Methods append requirements; use the matching `clear...` method first |
| Duplicate ID error | Do not install two copies of one example; use `edit` to change an entry |
| A recipe is displayed but not locked | Add `.research(...)` to the recipe itself |
| A parent or item cannot be found | Check its full ID and whether this entry or item exists in the pack |

Look for script errors in `kubejs/logs/server.log` and `logs/latest.log`.
For example, `Research 'kubejs:ironworking', stage[0], field 'obtain'` points
to the item requirement on this entry's **first** stage.
Start with one working example and make one change at a time, so you can
see which line affected the result.
