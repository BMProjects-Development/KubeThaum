# KubeThaum 1.0 Guide

[Русский](guide-ru.md) · [Home](../README.md)

KubeThaum integrates KubeJS with Thaumaturge's native research system.
Use JavaScript to create, edit, replace and copy research entries, configure
Thaumonomicon tabs and gate supported recipes behind research progress.
The addon does not add items, models or textures of its own.

- [Installation and compatibility](#installation-and-compatibility)
- [Quick start](#quick-start)
- [API](#api)
- [Standalone examples](#standalone-examples)
- [Editor autocomplete](#editor-autocomplete)
- [Building and testing](#building-and-testing)
- [Automatic builds and releases](#automatic-builds-and-releases)
- [Checks before publishing](#checks-before-publishing)

## Installation and compatibility

| Component | Tested version / requirement |
| --- | --- |
| Minecraft | 26.1.2 |
| Java | 25 |
| NeoForge | 26.1.2.109 or newer, before 26.2; tested on 109 and 115 |
| KubeJS | 26.1.2-8.0.6; this exact version is required by the metadata |
| Rhino | 2101.2.8-build.91 |
| Thaumaturge | 1.0.2; metadata permits `[1.0.2,1.1)` |
| Curios / Lithostitched | Thaumaturge dependencies; development uses 15.0.0+26.1.2 / 1.8.0 |

Install matching versions of KubeThaum and its required dependencies on both
the client and server. Better Advanced Tooltips is an optional KubeJS integration
included in the development environment, but it is not required by KubeThaum's API.
Dependencies retain their own licenses; KubeThaum's license is recorded in its JAR metadata.

Download `kubethaum-26.1.2-1.0.jar` from
[GitHub Releases](https://github.com/BMProjects-Development/KubeThaum/releases)
and place it in `mods`. Replace any previous KubeThaum JAR rather than keeping
multiple addon versions installed. The `-sources.jar` file is for developers;
do not install it in `mods`. The ZIP includes the game JAR, guides, examples
and editor declarations. Scripts must be installed separately: the addon does
not create pack-specific content on its own.

NeoForge is allowed by the range `[26.1.2.109,26.2)`. Future API changes within
that range still require validation. Released KubeJS 8.0.6 targets an old
`RegistryDataLoader.load` signature, causing its WorldLoaderMixin to fail on
NeoForge 114/115. KubeThaum includes a small compatibility library using
NeoForge's official ClassProcessor API: it updates that exact injection descriptor
in memory only when the loader exposes the newer `pendingTags` argument.
On the older API, or for an already fixed descriptor, no patch is applied.
KubeJS files on disk do not need to be replaced or edited.

The addon also selects the appropriate registry hook and forwards `pendingTags`
unchanged. Implementation:
[KubeJSWorldLoaderProcessor.java](https://github.com/BMProjects-Development/KubeThaum/blob/main/src/compat/java/org/bmp/kubethaum/compat/KubeJSWorldLoaderProcessor.java).
API reference:
[NeoForge ClassProcessor](https://github.com/neoforged/FancyModLoader/blob/main/loader/src/main/java/net/neoforged/neoforgespi/transformation/ClassProcessor.java).

## Quick start

1. Install the addon and its dependencies on the client and server.
2. Copy [research.js](../examples/server_scripts/research.js) into
   `kubejs/server_scripts/` and [research_lang.js](../examples/client_scripts/research_lang.js)
   into `kubejs/client_scripts/` on the client.
3. Restart the server or reopen the world. After changing client translations,
   reload client resources or restart the client.
4. Open the Thaumonomicon: the Metallurgy tab will contain two research entries.

```text
mods/kubethaum-26.1.2-1.0.jar
kubejs/server_scripts/research.js
kubejs/client_scripts/research_lang.js
kubejs/types/kubethaum.d.ts       # editor only
kubejs/types/globals.d.ts         # editor only
kubejs/jsconfig.json             # optional
```

Research and recipes belong in `server_scripts`; translations belong in
`client_scripts`. For a dedicated server, distribute client translations to
players. Server research entries are sent to clients through Thaumaturge's
native registry synchronization.

`/reload` updates recipes but **does not reload research registries**.
After changing `ThaumaturgeEvents.research`, reopen the world or restart the server.
Player progress is associated with research IDs. Reordering or removing stages
of an existing research entry changes the meaning of saved stage indices.

Minimal server script:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:metals')
    .category('thaumaturge:alchemy')
    .title('Studying metals')
    .icon('minecraft:iron_ingot')
    .position(4, 2)
    .stage(stage => {
      stage.text('Gather eight iron ingots.')
      stage.obtain('minecraft:iron_ingot', 8)
    })
    .stage(stage => stage.text('You have studied the properties of iron.'))
})
```

Titles and page text accept literal strings or translation keys. Tab titles use
`research_category.<namespace>.<path>`, for example `research_category.pack.metallurgy`.
Define that key using the client's `ClientEvents.lang` event.

After successful registry loading, `logs/latest.log` contains a summary:

```text
Research loaded: 2 added, 1 changed; categories: 1 added, 0 changed.
```

The counts represent unique records: repeated edits to one ID count as one change.
The message is emitted after the native loader succeeds, not when handlers are registered.

## API

Use explicit namespaced IDs such as `pack:entry` and `minecraft:iron_ingot`.
Research handlers run in order and see changes from earlier handlers.
Records are read from the highest-priority active datapack resources, including
`kubejs/data` and generated KubeJS resources. KubeThaum applies its overlay
when the native registries load.

### Research event

| Method | Behavior |
| --- | --- |
| `create(id)` | Create a record; an existing ID is an error |
| `replace(id)` | Replace all fields with a new builder; also works for a new ID |
| `edit(id)` | Edit a record, preserving unspecified fields |
| `edit(id, callback)` | Edit through a callback; returns `void` |
| `copy(sourceId, newId)` | Create an independent snapshot under a new ID |
| `category(id)` | Create a tab; an existing ID is an error |
| `editCategory(id)` | Edit an existing tab |
| `exists(id)` | Check whether a research entry exists |
| `get(id)` / `getCategory(id)` | Return a detached Gson JSON snapshot |

Use `edit()` to modify the live record. Mutating a `get()` snapshot does not
change the registered research. Read Gson fields using
`snapshot.get('name').getAsString()`, not `snapshot.name`.

`copy()` snapshots all current fields, including stages, appearance, parents,
siblings, addenda, translation keys and unknown JSON fields. Earlier script
edits are included. Later edits to the source and copy remain independent.
References and translation keys are kept literally; they are not redirected
to the new ID. Player progress is not copied. The destination ID must be unused.

```js
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'pack:advanced_metallurgy')
    .title('Advanced metallurgy')
    .position(4, 2)
    .clearParents().parent('thaumaturge:metallurgy')
})
```

### Research builder

| Method | Behavior |
| --- | --- |
| `category(id)`, `title(text)`, `position(column, row)` | Set the tab, title and grid coordinates |
| `icon(id)` | Append an item ID, a texture path ending in `.png`, or `focus:namespace:part` |
| `parent(id)` | Require the parent's full completion |
| `parent(id, stage)` | Require native parent progress, numbered from **1**; `1` means started |
| `hiddenParent(id)` | Parent without a visible connector; `parent('~pack:id@2')` is also supported |
| `sibling(id)` | Native decorative/cascade-completion relationship |
| `flag(name)` | `round`, `spiky`, `reverse`, `hidden`, `autounlock` or `hex` |
| `autoUnlock()` | Shortcut for the `autounlock` flag |
| `noteAspect(id, amount)` | Add/update an aspect for the research note |
| `complexity(1..3)` | Set research note complexity |
| `stage(callback)` | Append a stage |
| `editStage(index, callback)` | Edit a stage; indices start at **0** |
| `insertStage(index, callback)` | Insert a stage; index may range from 0 through the current stage count |
| `removeStage(index)` | Remove a stage at a zero-based index |
| `addendum(callback)` | Append a page shown after completion |
| `clearStages()`, `clearParents()`, `clearIcons()`, `clearAddenda()` | Clear the corresponding collection |

Each research entry needs at least one stage. Each new stage or addendum needs
`text()`. Repeated icons, parents and recipe references are not duplicated.
Use a chain beginning with `edit(id)`, not with the callback overload that returns `void`.

### Stages and additional pages

Stages and addenda both support `text(textOrKey)`, `recipe(id)` and `requireResearch(id)`.
For stages, `requireResearch` is a completion requirement; for addenda, it controls
page visibility. `recipe(id)` displays a recipe and does not lock crafting.

Stages additionally support:

| Method | Behavior |
| --- | --- |
| `obtain(itemOrTag, amount = 1)` | Native inventory requirement |
| `craft(itemOrTag, amount = 1)` | Native crafting requirement |
| `obtainAny([itemIds], amount)`, `craftAny([itemIds], amount)` | Aggregate count of the listed direct items |
| `obtain(itemOrTag, amount, components)` / `craft(...)` | Requirement with a JSON component patch |
| `knowledge(type, category, amount)` | Knowledge reward |
| `requireKnowledge(type, category, amount)` | Knowledge cost |
| `warp(amount)` | Stage warp; must not be negative |
| `construct(x, y, z, cells, cost = [])` | Native multiblock diagram |
| `clearObtain()`, `clearCraft()` | Clear item requirements |
| `clearRequiredResearch()`, `clearRequiredKnowledge()`, `clearRecipes()` | Clear dependencies, knowledge costs or displayed recipes |

Amounts must be positive. Tags use `#minecraft:planks`. The `*Any` methods
accept direct item IDs; use `obtain('#minecraft:planks', 2)` or `craft(...)`
for a single tag. Components use the Minecraft 26.1.2 component patch format.
Knowledge types are `theory` and `observation`; amounts are raw Thaumaturge points:
32 points per theory, 16 per observation. Inventory and crafting behavior
is implemented by Thaumaturge itself; `obtain` is not a history of item pickups.

`construct` requires exactly `x*y*z` cells, top layer first, in x-major order.
A cell is an item ID, `#tag` or `''`. Cost is an aspect list, for example
`[{ aspect: 'thaumaturge:ignis', amount: 5 }]`.

### Tabs

`style(existingCategoryId)` copies only the icon, background and overlay.
`icon(texture)`, `background(texture)`, `overlay(texture)` and `noOverlay()`
configure existing textures. A tab icon is a texture, whereas a research icon
may be an item. `index(nonNegativeNumber)` sets ordering.

`formula(aspectId, amount)` adds/updates the research table knowledge formula.
An empty formula is permitted; define a custom one when needed.
`requireResearch(id)` sets the full-completion condition for opening a tab;
`noRequiredResearch()` removes that condition.

### Recipe gates

```js
ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('pack:gold')
  Thaumaturge.requireResearch(recipe, 'pack:metals')
})
```

Create `pack:metals` first, for example with the minimal script above.
`Thaumaturge.requireResearch(recipe, entry[, stage])` returns the same recipe
and supports these native Thaumaturge types:

- `arcane_workbench_shaped`, `arcane_workbench_shapeless`;
- `crucible`, `infusion`, `infusion_enchantment`;
- `dust_trigger_simple`, `dust_trigger_tag`, `dust_trigger_multiblock`.

An unsupported recipe type, missing research entry or excessive stage value
causes an error. This helper does not gate vanilla crafting or other mods' recipes.
Call it inside `ServerEvents.recipes`.

For manually constructed recipe JSON, use
`research: Thaumaturge.gate(entry[, stage[, negate]])`.
The returned value is Gson JSON. Omitting the stage requires full completion;
`negate = true` inverts the condition and is available with the three-argument overload.

### Stage numbering

| API | Numbering | Example |
| --- | --- | --- |
| `editStage`, `insertStage`, `removeStage` | From 0 | `editStage(0, ...)` edits the first stage |
| `parent(id, stage)` | From 1 | `parent('pack:metals', 1)` requires a started parent |
| Recipe `requireResearch` / `gate` | From 0 | Value `0` requires the research to be known, without full completion |
| Error paths `stage[n]` / `addendum[n]` | From 0 | `stage[1]` refers to the second stage |

Recipe stage values are Thaumaturge's native progress values. To require full
completion, **omit the stage argument** instead of calculating a final index.

### Diagnostics and limits

Builder errors include the record ID, stage/addendum index and field. Examples:

```text
Research 'pack:metals', stage[0], field 'obtain': Item amount must be positive
Research 'pack:metals', stage[0], field 'obtain[0].items': Unknown item 'minecraft:missing_item'
```

Look for the script location and stack trace in `kubejs/logs/server.log`
and `logs/latest.log`. Invalid static item IDs in requirements and construct cells
are checked before changed records load. Tags, aspects and components are
ultimately resolved by native codecs.

Validation covers duplicate IDs, missing research/category references, parent
stage bounds and full-completion dependency cycles, including tab unlock conditions.
Stage-qualified reciprocal parent links are excluded from full-completion cycle checks.
An uncaught handler error aborts research loading rather than silently applying partial changes.
Arbitrary kill/dimension tasks and universal recipe restrictions are outside version 1.0's scope.

## Standalone examples

Choose a folder under [examples/standalone](../examples/standalone).
Copy its `server_scripts` contents to `kubejs/server_scripts/`; for the tab example,
also install its `client_scripts` contents on the client.

| Example | Purpose |
| --- | --- |
| [01_minimal](../examples/standalone/01_minimal/server_scripts/minimal.js) | A two-stage research entry |
| [02_edit](../examples/standalone/02_edit/server_scripts/edit.js) | An addendum for native Metallurgy |
| [03_category](../examples/standalone/03_category/server_scripts/category.js) | A tab and formula; [translation](../examples/standalone/03_category/client_scripts/category_lang.js) |
| [04_recipe_gate](../examples/standalone/04_recipe_gate/server_scripts/recipe_gate.js) | Research and a recipe gated by it |
| [05_copy](../examples/standalone/05_copy/server_scripts/copy.js) | A native record copy with an edited first stage |

Each example works independently with the required mods. Their new IDs differ,
so the examples can also be installed together. Select the script files;
do not copy the whole `standalone` directory into `server_scripts`.
The full Metallurgy demonstration remains in
[research.js](../examples/server_scripts/research.js),
[edit_research.js](../examples/server_scripts/edit_research.js) and
[research_lang.js](../examples/client_scripts/research_lang.js).
Some bundled example text is Russian; replace literal text or supply translation keys as needed.

## Editor autocomplete

1. Copy [kubethaum.d.ts](../types/kubethaum.d.ts) and
   [globals.d.ts](../types/globals.d.ts) to `kubejs/types/`.
2. If there is no `jsconfig.json`, copy the [template](../types/jsconfig.example.json)
   to `kubejs/jsconfig.json`. Otherwise, add `types/**/*.d.ts` to its `include`
   while preserving existing settings.
3. Open `kubejs` in VS Code; restart the JavaScript language server if necessary.

Declarations are editor-only and do not replace runtime ID/value validation.
If another declaration set already defines `ThaumaturgeEvents` or `Thaumaturge`,
do not also include `globals.d.ts`. The namespace-only `kubethaum.d.ts` remains
available for JSDoc:

```js
/** @param {KubeThaum.ResearchEvent} event */
function registerResearch(event) {
  event.create('pack:sample').stage(stage => stage.text('Example'))
}
ThaumaturgeEvents.research(registerResearch)
```

Optional developer validation (PowerShell):

```powershell
npm.cmd install --prefix .tools/typescript --no-audit --no-fund --ignore-scripts typescript@5.9.3
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --target ES2020 src/test/types/api.ts
$examples = @(rg --files examples/standalone -g '*.js')
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --allowJs --checkJs --target ES2020 types/globals.d.ts src/test/types/kubejs-stubs.d.ts @examples
```

Node.js is only needed for declaration checks, not for the mod or Gradle build.

## Building and testing

Clone the source repository before running build commands:

```shell
git clone https://github.com/BMProjects-Development/KubeThaum.git
cd KubeThaum
```

Use JDK 25 and set `JAVA_HOME` if necessary. The Gradle Wrapper downloads Gradle
and dependencies automatically; a global Gradle installation is not required.

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer '-Pneo_version=26.1.2.115'
.\gradlew.bat releaseArtifacts
```

On Linux/macOS, use `chmod +x gradlew`, followed by
`./gradlew build releaseArtifacts runGameTestServer`.

`build` runs 37 research/copy/diagnostic checks and 9 compatibility checks against
the released KubeJS bytecode. GameTest runs actual examples and native codecs
in the separate `run-gametest` directory, checking research/category loading,
item tags, independent copies, recipe gates and full/partial progress through
Thaumaturge's disk and network codecs. Test classes and scripts are not bundled
in the game JAR. GUI and actual multiplayer scenarios require manual testing.

Dependencies are fetched from Maven/CurseMaven and are not bundled in the addon.
The addon's own compatibility library is embedded automatically. For development
against an existing installation, use `-PlocalModsDir=D:/path/to/instance/mods`.

Outputs:

- Game JAR: `build/libs/kubethaum-26.1.2-1.0.jar`.
- Source JAR: `build/libs/kubethaum-26.1.2-1.0-sources.jar`.
- Distribution: `build/distributions/KubeThaum-1.0.zip` (game JAR in `mods`, guides, examples, declarations).
- `releaseArtifacts` stages only the current version's three files in `build/release/`.

## Automatic builds and releases

The [build.yml workflow](https://github.com/BMProjects-Development/KubeThaum/blob/main/.github/workflows/build.yml)
runs on Linux with Java 25:

- Branch pushes, pull requests, `v*` tag pushes and manual dispatch trigger builds.
- The NeoForge 109/115 matrix runs `build`, `releaseArtifacts` and `runGameTestServer`.
- TypeScript additionally checks declarations and standalone examples.
- Actions stores the game JAR, source JAR and distribution ZIP from the minimum-version NeoForge 109 build.
- After both matrix builds succeed, a default-branch push or a `v*` tag push
  publishes a new GitHub release. Manual dispatch on the default branch may also publish it.
- The version comes from `mod_version` in `gradle.properties`; `1.0` produces `v1.0`.
  A manually pushed tag must match that property.
- Existing releases are preserved; publishing another release requires a new version.
  An existing tag without a release must point to the commit being built, or publishing fails.
- PRs and ordinary development branches only build. The dedicated release job uses
  the built-in `GITHUB_TOKEN` with `contents: write`; no extra secrets are required.

For the next release, change `mod_version`, update guides/declarations when API
changes require it, and push the commit to the default branch. Alternatively,
push the matching `v<mod_version>` tag for the prepared commit.
For manual execution, select Actions → Build and Release → Run workflow
on the default branch. Repository/organization policies must allow the actions
and permit GitHub Releases writes with the built-in token.

## Checks before publishing

GameTests validate scripts and codecs but do not open a GUI or connect a real
client. Use a separate test world with matching mod versions for these checks:

- [ ] Thaumonomicon tabs, titles, icons, stages and recipes display correctly.
- [ ] Reopening a world preserves completed/partial progress without duplicated stages or addenda.
- [ ] Editing only text/title for an existing ID applies the new text while preserving progress.
- [ ] A new-ID copy has independent progress; the original retains its progress.
- [ ] `/reload` updates recipes without duplicating research; research changes apply after reopening.
- [ ] A client on a dedicated server sees server-side entries and the correct recipe gate state.
- [ ] Reconnecting preserves progress and Thaumonomicon display.

For progress-preservation checks, keep research IDs and stage order/count unchanged.
Changing the stage structure changes the meaning of saved indices.
