# KubeThaum 1.1: рецепты и переработка

[English](recipes-en.md) · [Исследования](guide-ru.md) · [Главная](../README.md)

Скрипты JavaScript размещаются в `kubejs/server_scripts/`. Рецепты и бонусы
печи обновляются через `/reload`. Для изменений исследований по-прежнему
нужно переоткрыть мир. Готовый пример всех форматов —
[07_recipes](../examples/standalone/07_recipes/server_scripts/recipes.js).

## Создание рецептов

Внутри `ServerEvents.recipes` используйте `event.recipes.thaumaturge`.
Каждый метод возвращает рецепт KubeJS с поддержкой `.id(...)`. Результат
принимает обычную строку KubeJS, например `'2x minecraft:gold_ingot'`, или
`Item.of(...)`. Ингредиенты принимают ID, теги и ингредиенты KubeJS.
Стоимость аспектов задаётся объектом «ID: количество» либо родным массивом
`[{ aspect: 'thaumaturge:ordo', amount: 4 }]`. Количества — положительные целые.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot', '#c:ingots/iron', { 'thaumaturge:ordo': 4 }
  ).id('pack:gold').research('thaumaturge:metallurgy')
})
```

| Метод | Аргументы по порядку |
| --- | --- |
| `crucible` | результат, катализатор, аспекты |
| `arcane_workbench_shaped` | результат, строки схемы, объект символов и ингредиентов, vis, необязательные кристаллы |
| `arcane_workbench_shapeless` | результат, массив ингредиентов, vis, необязательные кристаллы |
| `infusion` | результат, катализатор, массив компонентов, аспекты |
| `infusion_enchantment` | имя зачарования, отображаемый катализатор, массив компонентов, аспекты |
| `dust_trigger_simple` | результат, ID целевого блока |
| `dust_trigger_tag` | результат, ID тега блоков без `#` |
| `dust_trigger_multiblock` | результат, ID чертежа |

`vis` должен быть положительным. Кристаллы задаются так же, как аспекты,
но принимают только первичные аспекты, максимум 64 каждого. Если их
не указать, список пуст. Для инфузии доступен `.instability(0..100)`.
Зачарования используют имена Thaumaturge, например `'swift'`, а не ID
зачарований Minecraft. Мультиблок ссылается на существующий чертёж;
новые чертежи определяются в датапаке. Особые рецепты вроде изготовления
Salis Mundus сохраняют родное поведение: у них нет настраиваемых
полей ингредиентов в этом API.

```js
ServerEvents.recipes(event => {
  const tt = event.recipes.thaumaturge
  tt.arcane_workbench_shaped('minecraft:gold_ingot', ['II', 'II'], {
    I: 'minecraft:iron_ingot'
  }, 20, { 'thaumaturge:ignis': 2 }).id('pack:arcane_gold')

  tt.infusion('minecraft:diamond', 'minecraft:gold_ingot', [
    'minecraft:iron_ingot', 'minecraft:emerald'
  ], { 'thaumaturge:ordo': 8 }).instability(2).id('pack:infused_diamond')
})
```

## Редактирование и массовый баланс

Обычные фильтры KubeJS работают с рецептами, загруженными из модов и
датапаков. `forEachRecipe`, `replaceInput`, `replaceOutput` и `remove`
обрабатывают именно их. Для изменения рецепта, созданного в том же
обработчике, сохраните возвращённый объект и вызывайте его методы.

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ type: 'thaumaturge:crucible' }, recipe => {
    recipe.scaleAspects(0.75)
  })
  event.forEachRecipe({ type: 'thaumaturge:infusion' }, recipe => {
    recipe.instability(1)
  })
  event.replaceInput({ type: 'thaumaturge:infusion' },
    'minecraft:diamond', 'minecraft:emerald')
  event.replaceOutput({ type: 'thaumaturge:crucible' },
    'minecraft:iron_ingot', 'minecraft:copper_ingot')
  // event.remove({ id: 'thaumaturge:some_recipe' })
})
```

- `.aspect(id, amount)` задаёт стоимость одного аспекта, сохраняя остальные.
  У магического верстака меняет кристаллы.
- `.scaleAspects(factor)` умножает все аспекты или кристаллы с округлением
  вверх. Множитель должен быть конечным и положительным; родные лимиты действуют.
- `.aspects({...})` / `.crystals({...})` заменяют весь список стоимости.
- `.vis(amount)`, `.instability(amount)`, `.catalyst(ingredient)`,
  `.components([...])` и `.result(item)` меняют соответствующее поле,
  если оно поддерживается выбранным форматом.
- `.research(id)` требует завершения исследования. `.research(id, progress)`
  использует родной прогресс от нуля. Прежний метод
  `Thaumaturge.requireResearch(recipe, id[, progress])` также работает.

Нетронутые поля, исследование и дополнительные поля JSON сохраняются.
Существующие инфузии с изменением компонентов катализатора сохраняют
`catalyst_patch`: фиксированный результат им не нужен. Фильтры и замены
предметов обрабатывают ингредиенты и фиксированные результаты, включая
символы схемы и компоненты инфузии. Ячейки чертежа и эффекты зачарования
не являются предметными ингредиентами рецепта и такими заменами не меняются.

Тауматорий использует рецепты тигля. Основной результат адской печи берётся
из обычных рецептов переплавки. Эссенциеплавильня работает с аспектами
предметов, центрифуга — с составом аспектов; отдельных типов рецептов
у них нет. Это обновление не меняет их правила аспектов и скорость плавки.

## Бонусы адской печи

Они хранятся в data map NeoForge и настраиваются через
`ServerEvents.generateData('after_mods', ...)`. Ключ — **входной предмет**
печи. Для него должен существовать обычный рецепт переплавки.

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.replaceInfernalBonus(event,
    'minecraft:raw_iron', 'minecraft:gold_nugget', 2, 0.5)
  Thaumaturge.infernalBonus(event,
    '#c:raw_materials/copper', 'minecraft:iron_nugget', 1, 0.25)
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
```

`infernalBonus` добавляет бонус к данным пакетов с меньшим приоритетом.
`replaceInfernalBonus` заменяет прежние бонусы выбранного входа,
`removeInfernalBonus` удаляет их список. Вход и бонус принимают ID или
тег предметов; для выходного тега случайно выбирается один предмет.
Количество — `1..64`, вероятность — `0..1`. Выход должен содержать хотя
бы один предмет при загрузке данных. Повторные вызовы `infernalBonus`
добавляют бонусы к списку одного входа, максимум 64. Замена начинает список
заново, удаление очищает его. Разные входы независимы.
Действуют родные правила объединения data maps, приоритетов тегов,
синхронизации с клиентом и `/reload`.

`/kubethaum check` проверяет исследования и синтаксис серверных скриптов.
Он не исполняет обработчики рецептов и генерации данных. Их ошибки после
`/reload` смотрите в `kubejs/logs/server.log` и `logs/latest.log`.
