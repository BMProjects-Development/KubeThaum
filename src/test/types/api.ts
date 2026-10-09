/// <reference path="../../../types/globals.d.ts" />
// Compile with TypeScript --strict --noEmit. Invalid calls below must remain type errors.
ThaumaturgeEvents.research(event => {
  event.create('pack:typed').category('thaumaturge:alchemy').complexity(2)
    .stage(stage => stage.text('Typed callback').obtain('#minecraft:planks', 2)
      .craftAny(['minecraft:stone'], 1).knowledge('theory', 'thaumaturge:alchemy', 32)
      .construct(1, 1, 1, [''], [{ aspect: 'thaumaturge:ordo', amount: 1 }]))
    .addendum(page => page.text('Extra').requireResearch('pack:other'))
  event.copy('pack:typed', 'pack:copy').editStage(0, stage => stage.clearObtain().warp(0))
  event.edit('pack:typed', research => { research.title('Edited') })
  event.editCategory('thaumaturge:alchemy').noOverlay().formula('thaumaturge:ignis', 2)
  const name = event.get('pack:typed').get('name')?.getAsString()
  const text: string | undefined = name
  void text
  // @ts-expect-error ID needs a namespace
  event.create('missing_namespace')
  // @ts-expect-error complexity is limited to 1..3
  event.edit('pack:typed').complexity(4)
  // @ts-expect-error wrong knowledge type
  event.edit('pack:typed').stage(stage => stage.knowledge('experience', 'thaumaturge:alchemy', 1))
  // @ts-expect-error edit with a callback returns void
  event.edit('pack:typed', research => { research.title('Edited') }).clearStages()
})
const typedRecipe = { id(_id: KubeThaum.Id) { return this }, marker: true }
const sameRecipe = Thaumaturge.requireResearch(typedRecipe, 'pack:typed')
const marker: boolean = sameRecipe.marker
void marker
Thaumaturge.gate('pack:typed', 0, true).get('stage')?.getAsInt()
// @ts-expect-error a plain string is not a KubeRecipe
Thaumaturge.requireResearch('minecraft:stone', 'pack:typed')
