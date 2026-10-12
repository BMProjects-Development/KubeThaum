// Add a research entry to the Alchemy tab. Copy to kubejs/server_scripts/.
ThaumaturgeEvents.research(event => {
  event.create('kubejs:ironworking')
    .category('thaumaturge:alchemy')
    .title('Обработка железа')
    .icon('minecraft:iron_ingot')
    .position(8, 0)
    .stage(stage => {
      stage.text('Подготовьте четыре железных слитка для опытов с металлами.')
      stage.obtain('minecraft:iron_ingot', 4)
    })
    .stage(stage => {
      stage.text('Железо готово к дальнейшим алхимическим опытам.')
    })
})
