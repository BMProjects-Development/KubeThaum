// Regression: editing native research that requires progress flags (f_onfire is not a research entry).
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:base_auromancy')
    .addendum(page => page.text('Progress flag regression test'))
})
