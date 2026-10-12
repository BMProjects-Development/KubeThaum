ServerEvents.recipes(event => {
  const tt = event.recipes.thaumaturge
  let changed
  event.forEachRecipe({ id: 'kubethaum:recipe_edit' }, recipe => {
    changed = recipe
    recipe.research('pack:basic_metallurgy').aspect('thaumaturge:ordo', 5).scaleAspects(1.5)
    recipe.serialize()
    if (recipe.json.get('pack_custom_field').getAsString() !== 'keep') throw new Error('Unknown recipe fields were lost')
  })
  if (!changed) throw new Error('Data-pack fixture was not loaded')
  event.replaceInput({ id: 'kubethaum:recipe_edit' }, 'minecraft:iron_ingot', 'minecraft:copper_ingot')
  event.replaceOutput({ id: 'kubethaum:recipe_edit' }, 'minecraft:gold_ingot', 'minecraft:diamond')
  event.remove({ id: 'kubethaum:recipe_removed' })
  event.replaceInput({ id: 'kubethaum:infusion_edit' }, 'minecraft:emerald', 'minecraft:lapis_lazuli')
  event.forEachRecipe({ id: 'kubethaum:infusion_patch' }, recipe => recipe.scaleAspects(2))
  event.replaceInput({ id: 'kubethaum:shaped_edit' }, 'minecraft:iron_ingot', 'minecraft:copper_ingot')
  event.forEachRecipe({ id: 'kubethaum:shaped_edit' }, recipe => recipe.scaleAspects(2).vis(30))
  let rejected = 0
  try { changed.scaleAspects(0) } catch (error) { rejected++ }
  try { changed.aspect('thaumaturge:ordo', -1) } catch (error) { rejected++ }
  try { changed.aspect('thaumaturge:missing', 1) } catch (error) { rejected++ }
  if (rejected !== 3) throw new Error('Recipe validation accepted invalid costs')
})

ServerEvents.generateData('after_mods', event => {
  // These are loaded as original recipes, so KubeJS bulk operations can find them.
  event.json('kubethaum:recipe/recipe_edit.json', {
    type: 'thaumaturge:crucible', catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 4 }],
    result: { id: 'minecraft:gold_ingot' }, pack_custom_field: 'keep'
  })
  event.json('kubethaum:recipe/recipe_removed.json', {
    type: 'thaumaturge:crucible', catalyst: 'minecraft:stone',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 1 }], result: { id: 'minecraft:stick' }
  })
  event.json('kubethaum:recipe/infusion_edit.json', {
    type: 'thaumaturge:infusion', catalyst: 'minecraft:gold_ingot', components: ['minecraft:emerald'],
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 2 }], result: { id: 'minecraft:diamond' }, instability: 2
  })
  event.json('kubethaum:recipe/infusion_patch.json', {
    type: 'thaumaturge:infusion', catalyst: 'minecraft:iron_sword', components: ['minecraft:emerald'],
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 2 }], catalyst_patch: { 'minecraft:damage': 0 }
  })
  event.json('kubethaum:recipe/shaped_edit.json', {
    type: 'thaumaturge:arcane_workbench_shaped', pattern: ['II', 'II'], key: { I: 'minecraft:iron_ingot' },
    vis: 20, crystals: [{ aspect: 'thaumaturge:ignis', amount: 2 }], result: { id: 'minecraft:gold_ingot' }
  })
  // Independent fixed outputs for native data-map assertions.
  Thaumaturge.replaceInfernalBonus(event, 'minecraft:cobblestone', 'minecraft:gold_nugget', 3, 0.75)
  Thaumaturge.infernalBonus(event, 'minecraft:cobblestone', 'minecraft:iron_nugget', 1, 0.25)
  let rejected = 0
  try { Thaumaturge.infernalBonus(event, 'minecraft:stone', 'minecraft:iron_nugget', 0, 1) } catch (error) { rejected++ }
  try { Thaumaturge.infernalBonus(event, 'minecraft:stone', 'minecraft:iron_nugget', 1, 2) } catch (error) { rejected++ }
  if (rejected !== 2) throw new Error('Infernal bonus validation accepted invalid parameters')
})
