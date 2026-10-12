// Use built-in Metallurgy as the starting point for separate copper research.
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'kubejs:copper_metallurgy')
    .title('Обработка меди')
    .position(8, 4)
    .clearParents()
    .parent('thaumaturge:metallurgy')
    .clearIcons()
    .icon('minecraft:copper_ingot')
    .editStage(0, stage => {
      stage.text('Подготовьте четыре медных слитка для алхимических опытов.')
      stage.clearObtain()
      stage.obtain('minecraft:copper_ingot', 4)
    })
})
