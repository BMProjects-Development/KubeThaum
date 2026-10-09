// Standalone: defines both the research and the recipe that depends on it.
ThaumaturgeEvents.research(event => {
  event.create('example:transmutation')
    .category('thaumaturge:alchemy').title('Превращение металлов')
    .icon('minecraft:gold_ingot').position(8, 2)
    .stage(stage => stage.text('Соберите два железных слитка.').obtain('minecraft:iron_ingot', 2))
    .stage(stage => stage.text('Изучите рецепт в тигле.').recipe('example:gold_transmutation'))
})

ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('example:gold_transmutation')
  // No stage argument means full research completion. recipe() on a page only displays it.
  Thaumaturge.requireResearch(recipe, 'example:transmutation')
})
