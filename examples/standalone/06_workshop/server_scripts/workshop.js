// Copy to kubejs/server_scripts/workshop.js and reopen the world.
// Copy the matching client_scripts/workshop_lang.js to every client.
ThaumaturgeEvents.research(event => {
  event.category('workshop:workshop')
    .style('thaumaturge:alchemy')
    .index(40)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('workshop:preparation')
    .category('workshop:workshop')
    .title('research.workshop.preparation')
    .icon('minecraft:crafting_table')
    .position(0, 0)
    .stage(stage => {
      stage.text('research.workshop.preparation.stage.0')
      stage.obtain('#minecraft:planks', 4)
      stage.obtain('minecraft:crafting_table')
    })
    .stage(stage => stage.text('research.workshop.preparation.stage.1'))
    .addendum(page => {
      page.text('research.workshop.preparation.addendum')
      page.requireResearch('workshop:transmutation')
    })

  event.create('workshop:transmutation')
    .category('workshop:workshop')
    .title('research.workshop.transmutation')
    .icon('minecraft:gold_ingot')
    .position(3, 0)
    .parent('workshop:preparation')
    .stage(stage => {
      stage.text('research.workshop.transmutation.stage.0')
      stage.obtain('minecraft:iron_ingot', 2)
    })
    .stage(stage => {
      stage.text('research.workshop.transmutation.stage.1')
      stage.recipe('workshop:gold_transmutation')
    })
})

ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('workshop:gold_transmutation')

  // The recipe is usable after the entire transmutation research is complete.
  // stage.recipe() above displays the recipe without unlocking it.
  Thaumaturge.requireResearch(recipe, 'workshop:transmutation')
})
