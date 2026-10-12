// Workshop: prepare a workspace, study transmutation, and unlock a gold recipe.
// Copy to kubejs/server_scripts/ and put workshop_lang.js in kubejs/client_scripts/.
ThaumaturgeEvents.research(event => {
  event.category('kubejs:workshop')
    .style('thaumaturge:alchemy')
    .index(40)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('kubejs:workshop_preparation')
    .category('kubejs:workshop')
    .title('research.kubejs.workshop_preparation')
    .icon('minecraft:crafting_table')
    .position(0, 0)
    .stage(stage => {
      stage.text('research.kubejs.workshop_preparation.stage.0')
      stage.obtain('#minecraft:planks', 4)
      stage.obtain('minecraft:crafting_table')
    })
    .stage(stage => {
      stage.text('research.kubejs.workshop_preparation.stage.1')
    })
    .addendum(page => {
      page.text('research.kubejs.workshop_preparation.addendum')
      page.requireResearch('kubejs:workshop_transmutation')
    })

  event.create('kubejs:workshop_transmutation')
    .category('kubejs:workshop')
    .title('research.kubejs.workshop_transmutation')
    .icon('minecraft:gold_ingot')
    .position(3, 0)
    .parent('kubejs:workshop_preparation')
    .stage(stage => {
      stage.text('research.kubejs.workshop_transmutation.stage.0')
      stage.obtain('minecraft:iron_ingot', 2)
    })
    .stage(stage => {
      stage.text('research.kubejs.workshop_transmutation.stage.1')
      stage.recipe('kubejs:workshop_gold')
    })
})

ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    'minecraft:iron_ingot',
    { 'thaumaturge:ordo': 5 }
  )
    .id('kubejs:workshop_gold')
    .research('kubejs:workshop_transmutation')
})
