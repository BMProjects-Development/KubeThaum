// Regression: editing native research that requires progress flags (f_onfire is not a research entry).
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:base_auromancy')
    .addendum(page => page.text('Progress flag regression test'))

  // Flags no built-in research uses: an event flag, a scan entry key, and ScanKeys IDs.
  event.create('pack:runtime_flags')
    .category('thaumaturge:auromancy')
    .parent('thaumaturge:m_endoftheworld')
    .stage(stage => stage.text('Runtime progress flags')
      .requireResearch('thaumaturge:scanned/orblock3')
      .requireResearch('thaumaturge:scanned/entity/minecraft/zombie')
      .requireResearch('thaumaturge:scanned/item/minecraft/iron_ingot')
      .requireResearch('thaumaturge:scanned/aspect/thaumaturge/aer')
      .requireResearch('thaumaturge:scanned/effect/minecraft/speed'))
})
