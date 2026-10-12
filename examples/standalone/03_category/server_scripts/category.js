// Create a custom tab. Its title is defined in client_scripts/category_lang.js.
ThaumaturgeEvents.research(event => {
  event.category('kubejs:alchemy_workshop')
    .style('thaumaturge:alchemy')
    .index(30)
    .formula('thaumaturge:ordo', 2)
  event.create('kubejs:workshop_tools')
    .category('kubejs:alchemy_workshop')
    .title('Инструменты алхимика')
    .icon('minecraft:crafting_table')
    .position(0, 0)
    .stage(stage => {
      stage.text('Подготовьте верстак для работы в мастерской.')
      stage.obtain('minecraft:crafting_table')
    })
})
