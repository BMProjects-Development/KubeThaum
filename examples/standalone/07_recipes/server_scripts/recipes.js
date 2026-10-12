// Recipes for your modpack. Keep the blocks you need.
// Copy to kubejs/server_scripts/recipes.js and run /reload.
ServerEvents.recipes(event => {
  // Turn iron into gold in the crucible after completing Metallurgy.
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    '#c:ingots/iron',
    { 'thaumaturge:ordo': 3 }
  )
    .id('kubejs:gold_in_crucible')
    .research('thaumaturge:metallurgy')

  // Four iron ingots, 20 vis, and two Ignis crystals.
  event.recipes.thaumaturge.arcane_workbench_shaped(
    'minecraft:gold_ingot',
    ['II', 'II'],
    { I: 'minecraft:iron_ingot' },
    20,
    { 'thaumaturge:ignis': 2 }
  ).id('kubejs:gold_at_arcane_workbench')

  // Shapeless: place the iron and redstone in any slots.
  event.recipes.thaumaturge.arcane_workbench_shapeless(
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:redstone'],
    10
  ).id('kubejs:gold_from_redstone')

  // Gold in the center, with iron and an emerald on the pedestals.
  event.recipes.thaumaturge.infusion(
    'minecraft:diamond',
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:emerald'],
    { 'thaumaturge:ordo': 8 }
  )
    .instability(2)
    .id('kubejs:diamond_infusion')

  // Swift enchantment for a feather and eight Aer; boots are shown in the recipe.
  event.recipes.thaumaturge.infusion_enchantment(
    'swift',
    'minecraft:iron_boots',
    ['minecraft:feather'],
    { 'thaumaturge:aer': 8 }
  ).id('kubejs:swift_boots')

  // Salis Mundus turns a placed iron block into a gold block.
  event.recipes.thaumaturge.dust_trigger_simple(
    'minecraft:gold_block',
    'minecraft:iron_block'
  ).id('kubejs:gilded_iron')

  // The same transformation for any block in the logs tag. Omit # here.
  event.recipes.thaumaturge.dust_trigger_tag(
    'minecraft:gold_block',
    'minecraft:logs'
  ).id('kubejs:gilded_logs')

  // Build an infernal furnace using Thaumaturge's existing blueprint.
  event.recipes.thaumaturge.dust_trigger_multiblock(
    'thaumaturge:infernal_furnace',
    'thaumaturge:infernal_furnace'
  ).id('kubejs:infernal_furnace')
})

ServerEvents.generateData('after_mods', event => {
  // Replace raw iron's old bonuses with two gold nuggets at a 50% chance.
  Thaumaturge.replaceInfernalBonus(
    event,
    'minecraft:raw_iron',
    'minecraft:gold_nugget',
    2,
    0.5
  )

  // Add an extra iron nugget for raw copper at a 25% chance.
  Thaumaturge.infernalBonus(
    event,
    '#c:raw_materials/copper',
    '#c:nuggets/iron',
    1,
    0.25
  )

  // Smelt raw gold without bonuses.
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
