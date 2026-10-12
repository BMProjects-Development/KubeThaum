// Add copper to the first stage of Thaumaturge's built-in Metallurgy research.
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.obtain('minecraft:copper_ingot', 8)
    })
    .addendum(page => {
      page.text('Copper is now part of your metallurgical studies.')
    })
})
