// Standalone: snapshot all native fields, then change the copy only.
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'example:metallurgy_copy')
    .title('Дополнительная металлургия').position(8, 4)
    .clearParents().parent('thaumaturge:metallurgy')
    .clearIcons().icon('minecraft:copper_ingot')
    .editStage(0, stage => stage.text('Соберите четыре медных слитка.')
      .clearObtain().obtain('minecraft:copper_ingot', 4))
})
