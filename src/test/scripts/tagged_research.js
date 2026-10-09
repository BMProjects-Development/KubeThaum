// Regression: native research codecs must see item tags on both registry-loading APIs.
ThaumaturgeEvents.research(event => {
  event.create('pack:tagged_research')
    .category('thaumaturge:alchemy')
    .stage(stage => stage.text('Tag regression test').obtain('#minecraft:planks', 1))
})
