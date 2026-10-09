ServerEvents.recipes(event => {
  const test = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:copper_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 1 }],
    result: { id: 'minecraft:iron_ingot', count: 1 }
  }).id('kubethaum:gate_validation')
  let missingRejected = false
  let stageRejected = false
  let vanillaRejected = false
  try { Thaumaturge.requireResearch(test, 'pack:missing_research') } catch (error) { missingRejected = true }
  try { Thaumaturge.requireResearch(test, 'pack:basic_metallurgy', 50) } catch (error) { stageRejected = true }
  const vanilla = event.shapeless('minecraft:flint', ['minecraft:gravel']).id('kubethaum:vanilla_validation')
  try { Thaumaturge.requireResearch(vanilla, 'pack:basic_metallurgy') } catch (error) { vanillaRejected = true }
  if (!missingRejected || !stageRejected || !vanillaRejected) throw new Error('Invalid recipe gate was accepted')
  Thaumaturge.requireResearch(test, 'pack:basic_metallurgy', 0)
  console.info('KubeThaum: missing research and invalid gate stages rejected')
})
