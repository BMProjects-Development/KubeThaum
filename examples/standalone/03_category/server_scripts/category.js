// Copy the matching client_scripts/category_lang.js to the client for the tab title.
ThaumaturgeEvents.research(event => {
  event.category('example:workshop')
    .style('thaumaturge:alchemy').index(30)
    .formula('thaumaturge:ordo', 2)
  event.create('example:workshop_intro')
    .category('example:workshop').title('Мастерская').icon('minecraft:crafting_table')
    .stage(stage => stage.text('Подготовьте верстак.').obtain('minecraft:crafting_table'))
})
