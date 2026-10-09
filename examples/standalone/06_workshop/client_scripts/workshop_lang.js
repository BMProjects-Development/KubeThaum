// Copy to kubejs/client_scripts/workshop_lang.js on every client.
ClientEvents.lang('en_us', event => {
  event.add('research_category.workshop.workshop', 'Workshop')
  event.add('research.workshop.preparation', 'Preparing the Workshop')
  event.add('research.workshop.preparation.stage.0', 'Bring four planks of any wood and a crafting table. These items will be consumed when you complete this stage.')
  event.add('research.workshop.preparation.stage.1', 'Your workshop is ready. You can now begin studying metal transmutation.')
  event.add('research.workshop.preparation.addendum', 'Your transmutation experiments have given the workshop a new purpose.')
  event.add('research.workshop.transmutation', 'Metal Transmutation')
  event.add('research.workshop.transmutation.stage.0', 'Bring two iron ingots for the first experiment.')
  event.add('research.workshop.transmutation.stage.1', 'Complete this research to unlock the recipe below. In a crucible, combine an iron ingot with five points of Ordo to obtain a gold ingot.')
})

ClientEvents.lang('ru_ru', event => {
  event.add('research_category.workshop.workshop', 'Мастерская')
  event.add('research.workshop.preparation', 'Подготовка мастерской')
  event.add('research.workshop.preparation.stage.0', 'Принесите четыре доски любого дерева и верстак. Предметы будут израсходованы при прохождении этапа.')
  event.add('research.workshop.preparation.stage.1', 'Мастерская готова. Теперь можно заняться превращением металлов.')
  event.add('research.workshop.preparation.addendum', 'Опыты с превращением металлов дали мастерской новое назначение.')
  event.add('research.workshop.transmutation', 'Превращение металлов')
  event.add('research.workshop.transmutation.stage.0', 'Принесите два железных слитка для первого опыта.')
  event.add('research.workshop.transmutation.stage.1', 'Завершите исследование, чтобы открыть рецепт ниже. В тигле соедините железный слиток с пятью единицами Ordo и получите золотой слиток.')
})
