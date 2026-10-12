# KubeThaum: рецепты для сборки

[English](recipes-en.md) · [Исследования](guide-ru.md) · [Главная](../README.md)

Допустим, вы хотите добавить превращение железа в золото, удешевить алхимию
и поменять бонусы адской печи. Ниже — отдельные примеры для этих задач.
Выберите нужные и положите в `kubejs/server_scripts/thaumaturge_recipes.js`.
Несколько обработчиков `ServerEvents.recipes` в одном файле допустимы.

Сохраните файл и выполните `/reload`. Если вместе с рецептом вы добавляли
исследование, заново откройте мир: исследования обновляются при его загрузке.
Готовый файл со всеми восемью видами рецептов есть в
[07_recipes](../examples/standalone/07_recipes/server_scripts/recipes.js).

<details>
<summary>Содержание</summary>

- [Золото из железа в тигле](#золото-из-железа-в-тигле)
- [Золото на магическом верстаке](#золото-на-магическом-верстаке)
- [Алмаз через инфузию](#алмаз-через-инфузию)
- [Превращение блоков с Salis Mundus](#превращение-блоков-с-salis-mundus)
- [Изменить готовый рецепт](#изменить-готовый-рецепт)
- [Бонусы адской печи](#бонусы-адской-печи)
- [Справочник и проверка](#справочник-и-проверка)

</details>

## Золото из железа в тигле

Игрок бросает в тигель железный слиток, тратит четыре Ordo и получает золото.
Рецепт открывается после обычного исследования металлургии Thaumaturge.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.crucible(
    'minecraft:gold_ingot',
    '#c:ingots/iron',
    { 'thaumaturge:ordo': 4 }
  )
    .id('kubejs:gold_from_iron')
    .research('thaumaturge:metallurgy')
})
```

Аргументы идут в порядке **результат, катализатор, аспекты**. Здесь катализатор —
любой слиток из тега `c:ingots/iron`. Если нужен только ванильный предмет,
напишите `'minecraft:iron_ingot'` без `#`.

`kubejs:gold_from_iron` — ID нового рецепта: его можно оставить как есть.
Для другого рецепта выберите своё имя после `kubejs:`, например
`kubejs:copper_from_iron`. По этому ID рецепт можно показывать в исследовании
и находить при дальнейших правках. Название предмета в игре от него не зависит.

Чтобы выдавать два слитка, замените результат на `'2x minecraft:gold_ingot'`.
Если ограничение исследованием не нужно, уберите строку `.research(...)`.
Для своей ветки укажите ID созданного исследования — это разобрано в
[гайде исследований](guide-ru.md#рецепт-за-исследованием).

## Золото на магическом верстаке

Этот вариант требует четыре железных слитка квадратом, 20 vis и два кристалла Ignis.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.arcane_workbench_shaped(
    'minecraft:gold_ingot',
    ['II', 'II'],
    { I: 'minecraft:iron_ingot' },
    20,
    { 'thaumaturge:ignis': 2 }
  ).id('kubejs:gold_at_arcane_workbench')
})
```

Каждая строка в `['II', 'II']` — ряд сетки. Ниже указано, что буква `I`
означает железный слиток. Для пустой ячейки используйте пробел.
После схемы и ингредиентов идут количество vis и кристаллы.

Если расположение не важно, используйте рецепт без схемы. Например, железо
и редстоун в любых ячейках превращаются в золото за 10 vis:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.arcane_workbench_shapeless(
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:redstone'],
    10
  ).id('kubejs:gold_from_redstone')
})
```

Последний аргумент с кристаллами пропущен, поэтому они не нужны.
Оба вида рецептов можно закрыть исследованием через `.research(...)`.

## Алмаз через инфузию

В центре лежит золотой слиток, на пьедесталах — железо и изумруд.
Инфузия расходует восемь Ordo и превращает центральный предмет в алмаз.

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.infusion(
    'minecraft:diamond',
    'minecraft:gold_ingot',
    ['minecraft:iron_ingot', 'minecraft:emerald'],
    { 'thaumaturge:ordo': 8 }
  )
    .instability(2)
    .id('kubejs:diamond_infusion')
})
```

Порядок: результат, центральный предмет, список предметов на пьедесталах,
аспекты. `.instability(2)` задаёт нестабильность; допустимы значения от 0 до 100.

Для зачарования вместо результата указывается его имя в Thaumaturge.
Например, так добавляется Swift с пером на пьедестале и стоимостью восемь Aer:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.infusion_enchantment(
    'swift',
    'minecraft:iron_boots',
    ['minecraft:feather'],
    { 'thaumaturge:aer': 8 }
  ).id('kubejs:swift_boots')
})
```

Железные ботинки здесь показываются в рецепте. Какие предметы действительно
можно зачаровать, определяет само зачарование Thaumaturge.
Пишите `'swift'`, а не ID зачарования Minecraft.

## Превращение блоков с Salis Mundus

Один блок можно превратить напрямую. Здесь Salis Mundus превращает
поставленный железный блок в золотой:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_simple(
    'minecraft:gold_block',
    'minecraft:iron_block'
  ).id('kubejs:gilded_iron')
})
```

Для целой группы блоков подойдёт тег. Этот рецепт действует на брёвна:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_tag(
    'minecraft:gold_block',
    'minecraft:logs'
  ).id('kubejs:gilded_logs')
})
```

У `dust_trigger_tag` тег блоков пишется **без `#`**. Это отличается от
ингредиентов в рецептах тигля или верстака.

Для конструкции нужен готовый чертёж. Например, адская печь использует
чертёж `thaumaturge:infernal_furnace`:

```js
ServerEvents.recipes(event => {
  event.recipes.thaumaturge.dust_trigger_multiblock(
    'thaumaturge:infernal_furnace',
    'thaumaturge:infernal_furnace'
  ).id('kubejs:infernal_furnace')
})
```

Первый аргумент — результат, второй — ID чертежа. Этот пример добавляет
рецепт для уже существующей конструкции. Новые чертежи задаются в датапаке.

## Изменить готовый рецепт

Допустим, Alumentum должен требовать меньше огня. Найдём родной рецепт
и уменьшим Ignis с десяти до пяти, сохранив другие аспекты и исследование:

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ id: 'thaumaturge:crucible/alumentum' }, recipe => {
    recipe.aspect('thaumaturge:ignis', 5)
  })
})
```

Для общего упрощения алхимии можно уменьшить стоимость всех рецептов тигля:

```js
ServerEvents.recipes(event => {
  event.forEachRecipe({ type: 'thaumaturge:crucible' }, recipe => {
    recipe.scaleAspects(0.75)
  })
})
```

Каждая стоимость умножается на `0.75` и округляется вверх: восемь станет
шестью, пять — четырьмя. На магическом верстаке этот метод меняет кристаллы.
Выберите точечную правку или общий множитель: если поставить оба обработчика,
изменения будут применяться последовательно.

Обычные замены KubeJS тоже работают. Например, сделать изумруд заменой
алмаза во всех готовых рецептах инфузии:

```js
ServerEvents.recipes(event => {
  event.replaceInput(
    { type: 'thaumaturge:infusion' },
    'minecraft:diamond',
    'minecraft:emerald'
  )
})
```

`replaceOutput` так же заменяет фиксированный результат. Чтобы совсем
отключить изготовление Alumentum, используйте:

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'thaumaturge:crucible/alumentum' })
})
```

Эти фильтры работают с рецептами, загруженными из модов и датапаков.
Свой новый рецепт настраивайте сразу цепочкой после создания, как
`.research(...)` и `.instability(...)` в примерах выше.

## Бонусы адской печи

Пусть при переплавке сырого железа иногда выпадают два золотых самородка.
Основным результатом по-прежнему будет железный слиток.

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.replaceInfernalBonus(
    event,
    'minecraft:raw_iron',
    'minecraft:gold_nugget',
    2,
    0.5
  )
})
```

После `event` идут **вход печи, бонус, количество, вероятность**.
`0.5` означает шанс 50%. Указывается сырое железо, которое кладут в печь,
а не слиток на выходе. Для входного предмета нужен обычный рецепт переплавки.
Бонусы задаются в `generateData`, потому что Thaumaturge хранит их отдельно
от рецептов, в data map NeoForge.

`replaceInfernalBonus` заменяет старые бонусы этого входа. Чтобы сохранить
прежние бонусы и добавить ещё один, используйте `infernalBonus`.
Вот добавление железного самородка для сырой меди и отключение бонусов сырого золота:

```js
ServerEvents.generateData('after_mods', event => {
  Thaumaturge.infernalBonus(
    event,
    '#c:raw_materials/copper',
    'minecraft:iron_nugget',
    1,
    0.25
  )
  Thaumaturge.removeInfernalBonus(event, 'minecraft:raw_gold')
})
```

Вход и бонус принимают ID предмета или тег с `#`; для бонуса из тега
случайно выбирается один предмет. Количество — от 1 до 64, вероятность —
от 0 до 1. Тег бонуса должен содержать хотя бы один предмет.
Повторный `infernalBonus` дописывает бонус к списку того же входа, максимум
64 бонуса. Замена начинает список заново, удаление очищает его.
При пересечении тегов и ID действуют обычные правила data maps NeoForge.

## Справочник и проверка

| Что изменить | Метод после создания рецепта или внутри `forEachRecipe` |
| --- | --- |
| Один аспект, сохранив остальные | `.aspect('thaumaturge:ordo', 4)` |
| Весь список аспектов | `.aspects({ 'thaumaturge:ordo': 4 })` |
| Все затраты одним множителем | `.scaleAspects(0.75)` |
| Кристаллы верстака | `.crystals({ 'thaumaturge:ignis': 2 })` |
| Vis верстака | `.vis(20)` |
| Нестабильность инфузии | `.instability(2)` |
| Катализатор | `.catalyst('minecraft:iron_ingot')` |
| Предметы на пьедесталах | `.components(['minecraft:iron_ingot', 'minecraft:emerald'])` |
| Результат | `.result('minecraft:gold_ingot')` |
| Открыть после исследования | `.research('thaumaturge:metallurgy')` |
| Открыть с начала исследования | `.research('thaumaturge:metallurgy', 0)` |

Методы работают там, где у рецепта есть соответствующее поле. Аспекты задаются
положительными целыми количествами. Вместо объекта допустим родной массив
`[{ aspect: 'thaumaturge:ordo', amount: 4 }]`. Vis должен быть положительным;
кристаллы принимают только первичные аспекты, максимум 64 каждого.
Множитель `scaleAspects` должен быть конечным и больше нуля.
Прежний помощник `Thaumaturge.requireResearch` тоже поддерживается.

Правки сохраняют нетронутые поля и дополнительные данные JSON. Инфузии,
улучшающие центральный предмет через `catalyst_patch`, сохраняют это улучшение.
Замены предметов меняют ингредиенты и фиксированные результаты, включая
символы схемы и компоненты инфузии. Они не меняют ячейки чертежа и эффект зачарования.
У особых рецептов вроде изготовления Salis Mundus нет настраиваемых
полей ингредиентов в этом API: они сохраняют родное поведение.

Тауматорий использует рецепты тигля, адская печь — обычную переплавку и бонусы
из примеров выше. Эссенциеплавильня работает с аспектами предметов, центрифуга —
с составом аспектов. Отдельных рецептов для них нет; эти скрипты не меняют
правила аспектов или скорость плавки.

После `/reload` проверьте рецепт в игре и загляните в `kubejs/logs/server.log`
при ошибке. Дополнительные сообщения есть в `logs/latest.log`.
`/kubethaum check` проверяет исследования и синтаксис серверных скриптов,
но не выполняет обработчики рецептов и генерации данных.
