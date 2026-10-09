ThaumaturgeEvents.research(event => {
  event.create('pack:copy_source')
    .category('thaumaturge:alchemy').title('Original').position(8, 8)
    .stage(stage => stage.text('Original stage').obtain('#minecraft:planks', 1))
  event.copy('pack:copy_source', 'pack:copy_snapshot')
    .title('Independent copy').position(10, 8)
    .editStage(0, stage => stage.clearObtain().obtain('minecraft:gold_ingot', 2))
  event.edit('pack:copy_source').title('Edited after copy')
  event.copy('thaumaturge:metallurgy', 'pack:native_copy').position(12, 8)

  // Exceptions caught by scripts must retain useful context and leave no half-added stage.
  try {
    event.edit('pack:copy_snapshot').stage(stage => stage.text('Bad').warp(-1))
    throw new Error('Negative warp unexpectedly accepted')
  } catch (error) {
    const message = String(error)
    if (!message.includes('pack:copy_snapshot') || !message.includes('stage[1]') || !message.includes("field 'warp'")) {
      throw new Error('Missing research error context: ' + message)
    }
  }
})
