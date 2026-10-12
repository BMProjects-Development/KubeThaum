// Add a page to completed Metallurgy research, keeping its original requirements.
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy')
    .addendum(page => {
      page.text('Медь тоже пригодится в алхимии. Запаситесь слитками для следующих опытов.')
    })
})
