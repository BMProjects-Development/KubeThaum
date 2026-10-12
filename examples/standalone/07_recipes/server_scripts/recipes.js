// KubeThaum 1.1: independent examples for every supported recipe serializer.
ServerEvents.recipes(event => {
  const tt = event.recipes.thaumaturge

  const crucible = tt.crucible('minecraft:gold_ingot', '#c:ingots/iron', { 'thaumaturge:ordo': 4 })
    .id('recipes_demo:crucible')
    .research('thaumaturge:metallurgy')

  tt.arcane_workbench_shaped('minecraft:gold_ingot', ['II', 'II'], {
    I: 'minecraft:iron_ingot'
  }, 20, { 'thaumaturge:ignis': 2 }).id('recipes_demo:arcane_shaped')

  tt.arcane_workbench_shapeless('minecraft:gold_ingot', [
    'minecraft:iron_ingot', 'minecraft:redstone'
  ], 10).id('recipes_demo:arcane_shapeless')

  tt.infusion('minecraft:diamond', 'minecraft:gold_ingot', [
    'minecraft:iron_ingot', 'minecraft:emerald'
  ], { 'thaumaturge:ordo': 8 }).instability(2).id('recipes_demo:infusion')

  tt.infusion_enchantment('swift', 'minecraft:iron_boots', [
    'minecraft:feather'
  ], { 'thaumaturge:aer': 8 }).id('recipes_demo:enchantment')

  tt.dust_trigger_simple('minecraft:gold_block', 'minecraft:iron_block')
    .id('recipes_demo:dust_simple')
  tt.dust_trigger_tag('minecraft:gold_block', 'minecraft:logs')
    .id('recipes_demo:dust_tag')
  // Uses a built-in blueprint; custom blueprints belong in a data pack.
  tt.dust_trigger_multiblock('thaumaturge:infernal_furnace', 'thaumaturge:infernal_furnace')
    .id('recipes_demo:dust_multiblock')

  // Keep the returned builder when changing a recipe created in this callback.
  crucible.aspect('thaumaturge:ordo', 6).scaleAspects(0.5)
  // forEachRecipe / replaceInput / replaceOutput / remove operate on loaded data-pack recipes.
  // See docs/recipes-en.md for examples of bulk edits.
})

ServerEvents.generateData('after_mods', event => {
  // Bonuses are keyed by the furnace INPUT. A normal smelting recipe is still required.
  Thaumaturge.replaceInfernalBonus(event, 'minecraft:raw_iron', 'minecraft:gold_nugget', 2, 0.5)
  Thaumaturge.infernalBonus(event, '#c:raw_materials/copper', '#c:nuggets/iron', 1, 0.25)
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
