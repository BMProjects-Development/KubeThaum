// Metallurgy: a custom tab, two research entries, and a gold recipe.
// Copy to kubejs/server_scripts/ and put research_lang.js in kubejs/client_scripts/.
ThaumaturgeEvents.research(event => {
  event.category('kubejs:metallurgy')
    .style('thaumaturge:alchemy')
    .index(20)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('kubejs:basic_metallurgy')
    .category('kubejs:metallurgy')
    .title('research.kubejs.basic_metallurgy')
    .icon('minecraft:iron_ingot')
    .position(0, 0)
    .stage(stage => {
      stage.text('research.kubejs.basic_metallurgy.stage.0')
      stage.obtain('minecraft:iron_ingot', 8)
      stage.obtain('minecraft:gold_ingot', 4)
    })
    .stage(stage => {
      stage.text('research.kubejs.basic_metallurgy.stage.1')
      stage.recipe('kubejs:gold_from_iron')
      stage.knowledge('observation', 'thaumaturge:alchemy', 16)
    })
    .addendum(page => {
      page.text('research.kubejs.basic_metallurgy.addendum')
      page.requireResearch('kubejs:advanced_metallurgy')
    })

  event.create('kubejs:advanced_metallurgy')
    .category('kubejs:metallurgy')
    .title('research.kubejs.advanced_metallurgy')
    .icon('minecraft:gold_ingot')
    .position(3, 0)
    .parent('kubejs:basic_metallurgy')
    .complexity(2)
    .noteAspect('thaumaturge:ordo', 3)
    .noteAspect('thaumaturge:ignis', 2)
    .stage(stage => {
      stage.text('research.kubejs.advanced_metallurgy.stage.0')
      stage.craft('minecraft:gold_block', 1)
      stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
      stage.warp(1)
    })
    .stage(stage => {
      stage.text('research.kubejs.advanced_metallurgy.stage.1')
    })
})

ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    'minecraft:iron_ingot',
    { 'thaumaturge:ordo': 5 }
  )
    .id('kubejs:gold_from_iron')
    .research('kubejs:basic_metallurgy')
})
