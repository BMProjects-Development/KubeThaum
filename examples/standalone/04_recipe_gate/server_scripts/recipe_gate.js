// Unlock iron-to-gold transmutation after completing the research.
ThaumaturgeEvents.research(event => {
  event.create('kubejs:metal_transmutation')
    .category('thaumaturge:alchemy')
    .title('Превращение металлов')
    .icon('minecraft:gold_ingot')
    .position(8, 2)
    .stage(stage => {
      stage.text('Подготовьте два железных слитка для первого превращения.')
      stage.obtain('minecraft:iron_ingot', 2)
    })
    .stage(stage => {
      stage.text('Завершите исследование, чтобы превращать железо в золото в тигле.')
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
