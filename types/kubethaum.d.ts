/** KubeThaum 1.0. Editor-only declarations; no runtime import is needed. */
declare namespace KubeThaum {
  type Id = `${string}:${string}`
  type ItemOrTag = Id | `#${Id}`
  type KnowledgeType = 'theory' | 'observation'
  type ResearchFlag = 'round' | 'spiky' | 'reverse' | 'hidden' | 'autounlock' | 'hex'
  type Components = Record<string, unknown>
  interface AspectCost { aspect: Id; amount: number }

  interface ResearchEvent {
    /** Create a new record. An existing ID is an error. */
    create(id: Id): ResearchBuilder
    /** Replace all fields of an existing record (or create a new one). */
    replace(id: Id): ResearchBuilder
    edit(id: Id): ResearchBuilder
    edit(id: Id, callback: (research: ResearchBuilder) => void): void
    /** Independent snapshot of all current fields. Links and translation keys retain their values. */
    copy(sourceId: Id, newId: Id): ResearchBuilder
    category(id: Id): CategoryBuilder
    editCategory(id: Id): CategoryBuilder
    exists(id: Id): boolean
    /** Detached Gson JSON snapshot. Change the live record with edit(), not this value. */
    get(id: Id): JsonObject
    getCategory(id: Id): JsonObject
  }

  interface ResearchBuilder {
    category(id: Id): this
    /** Literal text or a translation key. */
    title(textOrKey: string): this
    position(column: number, row: number): this
    /** Appends an item ID, texture path ending in .png, or focus:namespace:part. */
    icon(itemOrTextureOrFocus: Id): this
    /** Full completion, or native parent syntax such as ~pack:entry@2. */
    parent(reference: string): this
    /** Parent stage numbers start at 1; 1 means the parent has been started. */
    parent(id: Id, stage: number): this
    hiddenParent(id: Id): this
    sibling(id: Id): this
    flag(flag: ResearchFlag): this
    autoUnlock(): this
    noteAspect(id: Id, amount: number): this
    complexity(value: 1 | 2 | 3): this
    stage(callback: (stage: StageBuilder) => void): this
    /** Stage indices start at 0. */
    editStage(index: number, callback: (stage: StageBuilder) => void): this
    /** Index from 0 through the current stage count (inclusive). */
    insertStage(index: number, callback: (stage: StageBuilder) => void): this
    removeStage(index: number): this
    addendum(callback: (page: AddendumBuilder) => void): this
    clearStages(): this
    clearParents(): this
    clearIcons(): this
    clearAddenda(): this
  }

  interface PageBuilder {
    /** Required on each new stage/addendum. Accepts literal text or a translation key. */
    text(textOrKey: string): this
    /** Displays a recipe; does not lock crafting. */
    recipe(id: Id): this
    requireResearch(id: Id): this
  }
  interface AddendumBuilder extends PageBuilder {}
  interface StageBuilder extends PageBuilder {
    /** Native inventory requirement. Amount defaults to 1 and must be positive. */
    obtain(itemOrTag: ItemOrTag): this
    obtain(itemOrTag: ItemOrTag, amount: number): this
    obtain(itemOrTag: ItemOrTag, amount: number, components: Components): this
    /** One tag uses obtain('#namespace:tag'); this list accepts direct item IDs. */
    obtainAny(items: Id[], amount: number): this
    craft(itemOrTag: ItemOrTag): this
    craft(itemOrTag: ItemOrTag, amount: number): this
    craft(itemOrTag: ItemOrTag, amount: number, components: Components): this
    craftAny(items: Id[], amount: number): this
    /** Raw knowledge points: 32 per theory, 16 per observation. */
    knowledge(type: KnowledgeType, category: Id, amount: number): this
    requireKnowledge(type: KnowledgeType, category: Id, amount: number): this
    warp(amount: number): this
    /** Exactly x*y*z cells, top layer first; '' is an empty cell. */
    construct(x: number, y: number, z: number, cells: (ItemOrTag | '')[], cost?: AspectCost[]): this
    clearObtain(): this
    clearCraft(): this
    clearRequiredResearch(): this
    clearRequiredKnowledge(): this
    clearRecipes(): this
  }

  interface CategoryBuilder {
    /** Copies icon/background/overlay only, not the formula or unlock condition. */
    style(existingCategoryId: Id): this
    /** Texture path, not an item ID. */
    icon(texture: Id): this
    background(texture: Id): this
    overlay(texture: Id): this
    noOverlay(): this
    index(index: number): this
    requireResearch(id: Id): this
    noRequiredResearch(): this
    formula(aspect: Id, amount: number): this
  }

  interface Events {
    /** Server scripts only. Applied on world opening; /reload does not reload research registries. */
    research(callback: (event: ResearchEvent) => void): void
  }
  /** The minimal KubeRecipe shape used here; requireResearch returns the original recipe object. */
  interface Recipe { id(id: Id): this }
  interface Binding {
    /** ResearchGate stage values start at 0, unlike parent(id, stage). Omit for full completion. */
    gate(entry: Id): JsonObject
    gate(entry: Id, stage: number): JsonObject
    gate(entry: Id, stage: number, negate: boolean): JsonObject
    /** Supported Thaumaturge recipes only; call inside ServerEvents.recipes. */
    requireResearch<T extends Recipe>(recipe: T, entry: Id, stage?: number): T
  }

  /** Gson objects returned by get()/getCategory()/gate(), not plain JavaScript objects. */
  interface JsonElement {
    getAsString(): string
    getAsInt(): number
    getAsBoolean(): boolean
    getAsJsonObject(): JsonObject
    getAsJsonArray(): JsonArray
  }
  interface JsonObject extends JsonElement {
    has(key: string): boolean
    get(key: string): JsonElement | null
    getAsJsonObject(): JsonObject
    getAsJsonObject(key: string): JsonObject
    getAsJsonArray(): JsonArray
    getAsJsonArray(key: string): JsonArray
    addProperty(key: string, value: string | number | boolean): void
    deepCopy(): JsonObject
  }
  interface JsonArray extends JsonElement {
    size(): number
    get(index: number): JsonElement
  }
}
