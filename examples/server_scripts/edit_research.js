// Optional example. Uses the built-in Thaumaturge entry loaded from active datapacks.
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy', research => {
    research.editStage(0, stage => stage.obtain('minecraft:copper_ingot', 8))
    research.addendum(page => page.text('Copper is now part of your metallurgical studies.'))
  })
})
