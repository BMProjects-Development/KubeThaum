// Standalone: copy this file to kubejs/server_scripts/ and reopen the world.
ThaumaturgeEvents.research(event => {
  event.create('example:first_research')
    .category('thaumaturge:alchemy')
    .title('Первое исследование')
    .icon('minecraft:iron_ingot')
    .position(8, 0)
    .stage(stage => stage.text('Соберите четыре железных слитка.').obtain('minecraft:iron_ingot', 4))
    .stage(stage => stage.text('Свойства железа изучены.'))
})
