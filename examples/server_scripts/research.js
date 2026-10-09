// Copy to kubejs/server_scripts/research.js, then reopen the world / restart the server.
ThaumaturgeEvents.research(event => {
  event.category('pack:metallurgy')
    .style('thaumaturge:alchemy')
    .index(20)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('pack:basic_metallurgy')
    .category('pack:metallurgy')
    .title('research.pack.basic_metallurgy')
    .icon('minecraft:iron_ingot')
    .position(0, 0)
    .stage(stage => {
      stage.text('research.pack.basic_metallurgy.stage.0')
      stage.obtain('minecraft:iron_ingot', 8)
      stage.obtain('minecraft:gold_ingot', 4)
    })
    .stage(stage => {
      stage.text('research.pack.basic_metallurgy.stage.1')
      stage.recipe('pack:enchanted_alloy')
      stage.knowledge('observation', 'thaumaturge:alchemy', 16)
    })
    .addendum(page => {
      page.text('research.pack.basic_metallurgy.addendum')
      page.requireResearch('pack:advanced_metallurgy')
    })

  event.create('pack:advanced_metallurgy')
    .category('pack:metallurgy')
    .title('research.pack.advanced_metallurgy')
    .icon('minecraft:gold_ingot')
    .position(3, 0)
    .parent('pack:basic_metallurgy')
    .complexity(2)
    .noteAspect('thaumaturge:ordo', 3)
    .noteAspect('thaumaturge:ignis', 2)
    .stage(stage => {
      stage.text('research.pack.advanced_metallurgy.stage.0')
      stage.craft('minecraft:gold_block', 1)
      stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
      stage.warp(1)
    })
    .stage(stage => stage.text('research.pack.advanced_metallurgy.stage.1'))
})

ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('pack:enchanted_alloy')
  Thaumaturge.requireResearch(recipe, 'pack:basic_metallurgy')
})
