// Standalone: changes only an existing Thaumaturge record, preserving its other fields.
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy')
    .addendum(page => page.text('Сборка добавляет новые способы применения металлов.'))
})
