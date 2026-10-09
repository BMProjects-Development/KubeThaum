# KubeThaum: как создавать свои исследования

[English](guide-en.md) · [Главная](../README.md)

KubeThaum позволяет писать исследования Thaumaturge на JavaScript через KubeJS:
добавлять записи в таумономикон, создавать вкладки, менять этапы существующих
исследований и открывать рецепты по мере прохождения.

Начнём с маленького исследования, а затем разберём задачи, из которых можно
собрать свою ветку прогрессии. Законченный пример с вкладкой, переводами и
рецептом находится в разделе [«Готовая мастерская»](#готовая-мастерская).

- [Куда класть скрипты](#куда-класть-скрипты)
- [Первое исследование](#первое-исследование)
- [Требования и награды этапов](#требования-и-награды-этапов)
- [Цепочки исследований](#цепочки-исследований)
- [Своя вкладка](#своя-вкладка)
- [Названия и переводы](#названия-и-переводы)
- [Рецепт за исследованием](#рецепт-за-исследованием)
- [Изменение существующих исследований](#изменение-существующих-исследований)
- [Копирование и полная замена](#копирование-и-полная-замена)
- [Дополнительные страницы и оформление](#дополнительные-страницы-и-оформление)
- [Готовая мастерская](#готовая-мастерская)
- [Краткий справочник](#краткий-справочник)
- [Проверка скриптов командой](#проверка-скриптов-командой)
- [Если что-то не появилось](#если-что-то-не-появилось)

## Куда класть скрипты

Установите KubeThaum вместе с Thaumaturge, KubeJS, Rhino и зависимостями
Thaumaturge. Версия 1.0 предназначена для Minecraft 26.1.2, NeoForge
26.1.2.109 и выше в пределах 26.1.2, KubeJS 26.1.2-8.0.6 и Thaumaturge 1.0.2.
Мод сам по себе не добавляет исследований: их задают ваши скрипты.

Откройте папку игровой сборки. Нужны две папки KubeJS:

```text
kubejs/
├── server_scripts/
│   └── my_research.js       ← исследования, вкладки и рецепты
└── client_scripts/
    └── my_research_lang.js  ← переводы для таумономикона
```

Создайте отсутствующие папки и обычные файлы с расширением `.js`.
Названия файлов можете выбирать сами. Следите, чтобы редактор не сохранил
`my_research.js.txt`. Ничего импортировать в начале скрипта не требуется.

Исследования регистрируются в `ThaumaturgeEvents.research`, рецепты — в
`ServerEvents.recipes`, переводы — в `ClientEvents.lang`. Первые два события
пишутся в `server_scripts`, третье — в `client_scripts`.
На сервере исследования и рецепты устанавливает автор сборки; клиентские
переводы должны быть в сборке каждого игрока.

**После изменения исследований закройте и заново откройте мир** или
перезапустите выделенный сервер. `/reload` достаточно для рецептов, но он
не перезагружает исследования. После изменения переводов перезагрузите
ресурсы клиента через F3+T; при добавлении нового клиентского файла
перезапустите клиент.

Перед повторным открытием мира можно выполнить [`/kubethaum check`](#проверка-скриптов-командой):
команда прочитает сохранённые файлы и проверит исследования без применения изменений.

## Первое исследование

Создайте `kubejs/server_scripts/my_research.js` и вставьте **весь** пример:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:first_steps')
    .category('thaumaturge:alchemy')
    .title('Первые опыты')
    .icon('minecraft:iron_ingot')
    .position(8, 0)
    .stage(stage => {
      stage.text('Для первых опытов понадобятся четыре железных слитка.')
      stage.obtain('minecraft:iron_ingot', 4)
    })
    .stage(stage => {
      stage.text('Теперь вы знаете, с чего начать изучение металлов.')
    })
})
```

Заново откройте мир и найдите «Первые опыты» во вкладке алхимии.
У записи будет иконка железного слитка и два этапа: сдача железа и итоговый текст.
Координаты `(8, 0)` выбраны для примера; если запись накладывается на другую,
переместите её через `position`.

| Строка | Что она задаёт |
| --- | --- |
| `create('pack:first_steps')` | Уникальный ID исследования |
| `category('thaumaturge:alchemy')` | Вкладку таумономикона |
| `title('Первые опыты')` | Название записи |
| `icon('minecraft:iron_ingot')` | Иконку из существующего предмета |
| `position(8, 0)` | Колонку и строку в дереве; это сетка, не пиксели |
| `stage(...)` | Следующий этап исследования |
| `text(...)` | Текст этапа; нужен каждому новому этапу |
| `obtain(..., 4)` | Требование принести четыре предмета |

`pack` — условное имя вашей сборки, а `first_steps` — имя записи.
Например, для сборки Aurora можно использовать `aurora:first_steps`.
Пишите ID строчными латинскими буквами, без пробелов: `aurora:advanced_metals`.
Во всех ссылках на исследование должен стоять тот же ID.
Имя файла не определяет ID, а название на экране может быть любым.

Дальнейшие короткие примеры с `pack:first_steps` предполагают, что эта запись
уже создана. Вставляйте изменения после её создания внутри того же обработчика
либо в отдельный обработчик, выполняющийся позже. При разделении по файлам
можно использовать имена `00_research.js` и `10_research_edits.js`.
Полные примеры с собственными ID можно устанавливать отдельно.

## Требования и награды этапов

Следующие блоки заменяют тело `.stage(stage => { ... })` в первом примере.
Один этап может содержать сразу несколько требований: игрок должен выполнить все.

### Принести несколько видов предметов

```js
stage.text('Подготовьте восемь железных и четыре золотых слитка.')
stage.obtain('minecraft:iron_ingot', 8)
stage.obtain('minecraft:gold_ingot', 4)
```

`obtain` проверяет предметы в инвентаре. Thaumaturge расходует их при прохождении
этапа. Вызовы добавляют отдельные требования: здесь нужны **и железо, и золото**.
Если количество не указано, требуется один предмет.

### Любые предметы по тегу или из списка

```js
stage.text('Принесите четыре доски любого дерева.')
stage.obtain('#minecraft:planks', 4)
```

Префикс `#` означает тег предметов. Это удобно, если сборка добавляет собственную
древесину и её доски входят в `minecraft:planks`.

Для ограниченного списка допустимых предметов:

```js
stage.text('Принесите четыре железных или медных слитка.')
stage.obtainAny(['minecraft:iron_ingot', 'minecraft:copper_ingot'], 4)
```

Это **одно** требование на суммарное количество подходящих предметов:
подойдёт железо, медь или их смесь. В `obtainAny` передавайте ID предметов;
для тега используйте обычный `obtain('#namespace:tag', amount)`.

### Предмет с компонентами

```js
stage.text('Подготовьте железный меч без повреждений.')
stage.obtain('minecraft:iron_sword', 1, { 'minecraft:damage': 0 })
```

Третий аргумент задаёт компоненты Minecraft, которые проверяются у предмета.
Их формат должен соответствовать вашей версии игры.

### Требование крафта

```js
stage.text('Изготовьте верстак.')
stage.craft('minecraft:crafting_table')
```

`craft` передаёт требование родной системе Thaumaturge. В Thaumaturge 1.0.2
учитывается отметка о создании предмета; наличие подходящего предмета также
может удовлетворить условию. Это не счётчик «скрафтить N раз после начала этапа».
Для гарантированной сдачи конкретного количества используйте `obtain`.
Для альтернатив есть `craftAny(['minecraft:chest', 'minecraft:barrel'], 1)`.

### Дополнительное исследование внутри этапа

```js
stage.text('Перед следующим опытом завершите основы металлургии.')
stage.requireResearch('thaumaturge:metallurgy')
```

Условие относится к прохождению этого этапа. Для связи между записями на дереве
используется `parent`, который разобран ниже.

### Знания, исследовательские записки и варп

```js
stage.text('Изучите теорию алхимии. За этот этап вы получите наблюдение.')
stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
stage.knowledge('observation', 'thaumaturge:alchemy', 16)
stage.warp(1)
```

`requireKnowledge` добавляет требование, `knowledge` — награду за этап.
Типы знаний: `theory` и `observation`. Количества записываются в сырых очках:
32 — одна теория, 16 — одно наблюдение.

В Thaumaturge 1.0.2 требование теории связано с исследовательской запиской,
а требование наблюдения обрабатывается через родную стоимость в аспектах.
Не рассчитывайте на простое списание числа из шкалы знаний.
Для этапов с записками задавайте исследованию `noteAspect` и `complexity`:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:metal_theory')
    .category('thaumaturge:alchemy')
    .title('Теория металлов')
    .icon('minecraft:gold_ingot')
    .position(8, 2)
    .complexity(2)
    .noteAspect('thaumaturge:ordo', 3)
    .noteAspect('thaumaturge:ignis', 2)
    .stage(stage => {
      stage.text('Решите исследовательскую записку о свойствах металлов.')
      stage.requireKnowledge('theory', 'thaumaturge:alchemy', 32)
    })
    .stage(stage => {
      stage.text('Работа принесла новые наблюдения.')
      stage.knowledge('observation', 'thaumaturge:alchemy', 16)
    })
})
```

Сложность записки — `1`, `2` или `3`. `warp(1)` добавляет варп при прохождении
этапа; опускайте его, если исследование не должно иметь такого эффекта.

## Цепочки исследований

Добавим вторую запись, которая открывается после «Первых опытов».
Поместите этот блок после создания `pack:first_steps` внутри обработчика:

```js
event.create('pack:advanced_metals')
  .category('thaumaturge:alchemy')
  .title('Продвинутые сплавы')
  .icon('minecraft:gold_ingot')
  .position(10, 0)
  .parent('pack:first_steps')
  .stage(stage => {
    stage.text('Принесите золотой слиток для следующего опыта.')
    stage.obtain('minecraft:gold_ingot')
  })
  .stage(stage => stage.text('Исследование сплавов завершено.'))
```

`parent(id)` требует полного завершения родителя и создаёт связь на дереве.
Несколько вызовов требуют **всех** перечисленных родителей.
Можно ссылаться на записи Thaumaturge, например `thaumaturge:metallurgy`.

Чтобы родитель был только начат, используйте `.parent('pack:first_steps', 1)`.
Номера в `parent(id, stage)` начинаются с **1**: `1` означает начатую запись,
а не завершённый первый этап. `hiddenParent('pack:first_steps')` задаёт
зависимость без линии к родителю.

Не делайте взаимную зависимость полного завершения: если A требует B, а B
требует A, игрок не сможет пройти ни одну запись.

## Своя вкладка

Следующий пример самодостаточен. Сохраните его как
`kubejs/server_scripts/workshop_tab.js`:

```js
ThaumaturgeEvents.research(event => {
  event.category('pack:workshop')
    .style('thaumaturge:alchemy')
    .index(30)
    .formula('thaumaturge:ordo', 2)
    .formula('thaumaturge:ignis', 1)

  event.create('pack:workshop_intro')
    .category('pack:workshop')
    .title('Добро пожаловать в мастерскую')
    .icon('minecraft:crafting_table')
    .position(0, 0)
    .stage(stage => {
      stage.text('Подготовьте верстак для дальнейшей работы.')
      stage.obtain('minecraft:crafting_table')
    })
})
```

`style` берёт иконку, фон и наложение из существующей вкладки, поэтому рисовать
свои текстуры не нужно. Он копирует только оформление; формулу знаний и
условие открытия вы задаёте отдельно. `formula` добавляет аспекты формулы
этой категории для исследовательского стола, а `index` задаёт порядок вкладок.

Для названия вкладки создайте `kubejs/client_scripts/workshop_lang.js`:

```js
ClientEvents.lang('ru_ru', event => {
  event.add('research_category.pack.workshop', 'Мастерская')
})

ClientEvents.lang('en_us', event => {
  event.add('research_category.pack.workshop', 'Workshop')
})
```

Ключ имени вкладки строится как `research_category.<namespace>.<path>`.
У `pack:workshop` это `research_category.pack.workshop`.

Закрыть вкладку до завершения родной металлургии можно, добавив к её цепочке
`.requireResearch('thaumaturge:metallurgy')`. Исследование, открывающее вкладку,
должно быть доступно игроку вне неё. Уже существующую вкладку меняют так:

```js
ThaumaturgeEvents.research(event => {
  event.editCategory('thaumaturge:alchemy').index(5)
})
```

## Названия и переводы

Для личной сборки достаточно текста прямо в `title` и `text`.
Для нескольких языков используйте ключи перевода. Замените в первом
исследовании название и тексты, добавив после его создания:

```js
event.edit('pack:first_steps')
  .title('research.pack.first_steps')
  .editStage(0, stage => stage.text('research.pack.first_steps.stage.0'))
  .editStage(1, stage => stage.text('research.pack.first_steps.stage.1'))
```

В `kubejs/client_scripts/my_research_lang.js` добавьте:

```js
ClientEvents.lang('ru_ru', event => {
  event.add('research.pack.first_steps', 'Первые опыты')
  event.add('research.pack.first_steps.stage.0', 'Принесите четыре железных слитка.')
  event.add('research.pack.first_steps.stage.1', 'Первые опыты завершены.')
})

ClientEvents.lang('en_us', event => {
  event.add('research.pack.first_steps', 'First Experiments')
  event.add('research.pack.first_steps.stage.0', 'Bring four iron ingots.')
  event.add('research.pack.first_steps.stage.1', 'Your first experiments are complete.')
})
```

Ключи для `title` и `text` вы выбираете сами: они должны точно совпадать
с ключами в клиентском файле. Если вместо текста виден ключ, проверьте язык
игры и наличие соответствующего перевода на клиенте.

## Рецепт за исследованием

У записи в книге и самого рецепта разные задачи:

- `stage.recipe(id)` показывает рецепт на странице.
- `Thaumaturge.requireResearch(recipe, id)` ограничивает его использование.

Полный пример: исследование «Превращение металлов» и рецепт тигля.
Сохраните как `kubejs/server_scripts/transmutation.js`:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:transmutation')
    .category('thaumaturge:alchemy')
    .title('Превращение металлов')
    .icon('minecraft:gold_ingot')
    .position(8, 4)
    .stage(stage => {
      stage.text('Подготовьте два железных слитка.')
      stage.obtain('minecraft:iron_ingot', 2)
    })
    .stage(stage => {
      stage.text('После завершения исследования этот рецепт будет доступен в тигле.')
      stage.recipe('pack:gold_transmutation')
    })
})

ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('pack:gold_transmutation')

  Thaumaturge.requireResearch(recipe, 'pack:transmutation')
})
```

У рецепта свой ID, `pack:gold_transmutation`; именно его указывают в
`stage.recipe`. Условие использования ссылается на ID **исследования**,
`pack:transmutation`. До завершения записи рецепт закрыт.

Не требуйте в этом исследовании скрафтить предмет по рецепту, который
открывается только после его полного завершения: получится тупик.

Чтобы разрешить рецепт сразу после начала исследования, используйте третий
аргумент `0`: `Thaumaturge.requireResearch(recipe, 'pack:transmutation', 0)`.
Здесь используется родное значение прогресса Thaumaturge, с нуля.
Для полного завершения **не передавайте** третий аргумент.

Помощник работает с рецептами Thaumaturge: `arcane_workbench_shaped`,
`arcane_workbench_shapeless`, `crucible`, `infusion`, `infusion_enchantment`,
`dust_trigger_simple`, `dust_trigger_tag`, `dust_trigger_multiblock`.
Он не добавляет ограничения к обычным `minecraft:crafting_shaped` и рецептам
сторонних модов. Формат остальных полей рецепта определяется самим Thaumaturge.

Если собираете JSON вручную, эквивалентное поле внутри `event.custom({...})`:

```js
research: Thaumaturge.gate('pack:transmutation')
```

`Thaumaturge.gate(id, stage, true)` инвертирует условие прогресса.
Обычно достаточно `requireResearch`: он также проверяет существование записи.

## Изменение существующих исследований

`edit` сохраняет поля, которых вы не касались. Добавим медь к первому
этапу родной металлургии и пояснение в конец записи:

```js
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.obtain('minecraft:copper_ingot', 8)
    })
    .addendum(page => {
      page.text('В этой сборке медь также используется в металлургических опытах.')
    })
})
```

Требование меди **добавляется** к старым требованиям первого этапа.
Чтобы заменить только требования принесённых предметов, сначала очистите их:

```js
ThaumaturgeEvents.research(event => {
  event.edit('thaumaturge:metallurgy').editStage(0, stage => {
    stage.text('Для начала соберите четыре медных слитка.')
    stage.clearObtain()
    stage.obtain('minecraft:copper_ingot', 4)
  })
})
```

`clearObtain` не удаляет крафты, знания и другие условия. Для них есть отдельные
`clearCraft`, `clearRequiredResearch`, `clearRequiredKnowledge`, `clearRecipes`.

Изменение структуры этапов — внутри обработчика исследования:

```js
event.edit('pack:first_steps')
  .insertStage(1, stage => {
    stage.text('Между первым и итоговым этапом подготовьте золотой слиток.')
    stage.obtain('minecraft:gold_ingot')
  })
```

`editStage(0, ...)` меняет первый этап, `insertStage(1, ...)` вставляет этап
перед прежним вторым, `removeStage(1)` удаляет второй.
**Индексы редактирования начинаются с 0.** `stage(...)` всегда добавляет этап
в конец. После вставки или удаления индексы следующих этапов сдвигаются.

При обновлении готовой сборки сохраняйте ID записей. Прогресс игроков хранится
по ID и номерам этапов; перестановка уже пройденных этапов меняет смысл
сохранённого прогресса. Текст, иконки и дополнительные страницы менять проще.

## Копирование и полная замена

Для варианта существующего исследования используйте `copy`:

```js
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'pack:copper_metallurgy')
    .title('Медная металлургия')
    .position(10, 4)
    .clearIcons().icon('minecraft:copper_ingot')
    .clearParents().parent('thaumaturge:metallurgy')
    .editStage(0, stage => {
      stage.text('Начните новый опыт с четырёх медных слитков.')
      stage.clearObtain().obtain('minecraft:copper_ingot', 4)
    })
})
```

Копируются все текущие поля оригинала. Последующие правки копии не меняют
оригинал; прогресс у нового ID отдельный. Ссылки на рецепты, родителей,
дополнения и ключи перевода сохраняются: заменяйте их, если для копии
нужны свои. Новый ID не должен быть занят.

Если нужно полностью переписать существующую запись:

```js
ThaumaturgeEvents.research(event => {
  event.replace('thaumaturge:metallurgy')
    .category('thaumaturge:alchemy')
    .title('Металлургия этой сборки')
    .icon('minecraft:iron_ingot')
    .position(4, 0)
    .stage(stage => {
      stage.text('Теперь металлургия начинается со сбора восьми слитков железа.')
      stage.obtain('minecraft:iron_ingot', 8)
    })
})
```

`replace` начинает запись заново: прежние этапы, родители, иконки и дополнения
не сохраняются. Оставьте `edit` для небольших правок.
Примеры изменения и замены одной записи в этом разделе — альтернативы;
устанавливайте нужный вариант.

## Дополнительные страницы и оформление

Дополнение появляется у завершённого исследования. У него может быть условие
на завершение другой записи — удобно для текста, который раскрывается позже:

```js
ThaumaturgeEvents.research(event => {
  event.edit('pack:first_steps').addendum(page => {
    page.text('После изучения металлургии вы иначе смотрите на первые опыты.')
    page.requireResearch('thaumaturge:metallurgy')
  })
})
```

В дополнении также доступен `recipe(id)` для показа рецепта.
`clearAddenda()` убирает все старые дополнения перед добавлением новых.

У исследования можно поменять форму и иконку:

```js
event.edit('pack:first_steps')
  .flag('round')
  .clearIcons()
  .icon('minecraft:gold_ingot')
```

Доступные флаги Thaumaturge: `round`, `spiky`, `reverse`, `hidden`, `autounlock`,
`hex`. `autoUnlock()` — сокращение для `flag('autounlock')`.
Поведение флагов задаётся Thaumaturge. Для обычной записи они не обязательны.
`icon` добавляет иконку; `clearIcons` позволяет заменить прежний набор.
Кроме ID предмета, можно передать путь к существующей текстуре `.png`.
У **вкладки** `icon` принимает именно текстуру, а не предмет.

Этап может показывать схему конструкции. Например, столбик из двух каменных
блоков — этот код находится внутри `.stage(stage => { ... })`:

```js
stage.text('Пример схемы: два каменных блока один над другим.')
stage.construct(1, 2, 1, [
  'minecraft:stone', // верхний слой
  'minecraft:stone'  // нижний слой
], [{ aspect: 'thaumaturge:ordo', amount: 2 }])
```

Размеры идут как `x, y, z`, число ячеек должно быть ровно `x*y*z`.
Слои перечисляются сверху вниз; внутри слоя — сначала x, затем z.
`''` означает пустую ячейку, `#namespace:tag` — тег.
Это схема на странице, а не регистрация нового мультиблока или его рецепта.

## Готовая мастерская

В [06_workshop](../examples/standalone/06_workshop) находится законченный пример:

1. Вкладка «Мастерская» с оформлением алхимии.
2. «Подготовка мастерской»: сдача досок и верстака.
3. «Превращение металлов»: открывается после подготовки, требует железо.
4. Рецепт превращения железа в золото в тигле, доступный после второго исследования.
5. Русские и английские переводы всех названий и страниц.

Скопируйте [workshop.js](../examples/standalone/06_workshop/server_scripts/workshop.js)
в `kubejs/server_scripts/`, а
[workshop_lang.js](../examples/standalone/06_workshop/client_scripts/workshop_lang.js)
в `kubejs/client_scripts/`. Перезапустите клиент и откройте мир.
Пример использует namespace `workshop`, поэтому его можно установить рядом
с учебными скриптами `pack:*` из этого руководства.

Начните с изменения количества досок и железа. Затем добавьте третье
исследование с `.parent('workshop:transmutation')` и собственным ID.
Если захотите переименовать namespace, замените его в ID записей, вкладки,
рецепта, зависимостях и ключах перевода.

Другие готовые скрипты:

| Пример | Для чего использовать |
| --- | --- |
| [01_minimal](../examples/standalone/01_minimal/server_scripts/minimal.js) | Добавить небольшую запись в существующую вкладку |
| [02_edit](../examples/standalone/02_edit/server_scripts/edit.js) | Дополнить запись Thaumaturge |
| [03_category](../examples/standalone/03_category/server_scripts/category.js) | Создать вкладку; нужен [клиентский перевод](../examples/standalone/03_category/client_scripts/category_lang.js) |
| [04_recipe_gate](../examples/standalone/04_recipe_gate/server_scripts/recipe_gate.js) | Создать исследование с рецептом тигля |
| [05_copy](../examples/standalone/05_copy/server_scripts/copy.js) | Сделать вариант родной металлургии |
| [Полная металлургия](../examples/server_scripts/research.js) | Исследовательские записки, знания и условное дополнение; [переводы](../examples/client_scripts/research_lang.js) |

Переносите нужные `.js` из вложенных `server_scripts` и `client_scripts`
в одноимённые папки KubeJS. Папка `examples` сама по себе в игре не выполняется.

## Краткий справочник

| Задача | Метод |
| --- | --- |
| Создать / изменить / заменить | `event.create(id)` / `event.edit(id)` / `event.replace(id)` |
| Скопировать под новый ID | `event.copy(sourceId, newId)` |
| Проверить наличие | `event.exists(id)` |
| Создать / изменить вкладку | `event.category(id)` / `event.editCategory(id)` |
| Название, место и иконка | `title(text)`, `category(id)`, `position(x, y)`, `icon(id)` |
| Родитель / родитель без линии | `parent(id)` / `hiddenParent(id)` |
| Добавить / изменить / вставить / удалить этап | `stage(callback)`, `editStage(index, callback)`, `insertStage(index, callback)`, `removeStage(index)` |
| Очистить этапы, связи, иконки, дополнения | `clearStages()`, `clearParents()`, `clearIcons()`, `clearAddenda()` |
| Текст, рецепт на странице, условие исследования | `text(text)`, `recipe(id)`, `requireResearch(id)` |
| Предметы / варианты предметов | `obtain(idOrTag, amount)` / `obtainAny(ids, amount)` |
| Крафт / варианты крафта | `craft(idOrTag, amount)` / `craftAny(ids, amount)` |
| Награда знаниями / требование знаний | `knowledge(type, category, amount)` / `requireKnowledge(type, category, amount)` |
| Записка и варп | `noteAspect(id, amount)`, `complexity(1..3)`, `stage.warp(amount)` |
| Дополнительная страница | `addendum(callback)` |
| Оформление и доступ вкладки | `style(id)`, `icon(texture)`, `background(texture)`, `overlay(texture)`, `noOverlay()`, `index(n)`, `requireResearch(id)`, `noRequiredResearch()` |
| Формула вкладки | `formula(aspectId, amount)` |
| Ограничить рецепт | `Thaumaturge.requireResearch(recipe, researchId[, progress])` |

`event.edit(id, research => { ... })` — вариант редактирования через callback.
Он возвращает `void`; для цепочки методов используйте `event.edit(id)`.
`get(id)` и `getCategory(id)` возвращают независимый снимок Gson JSON:
имя читается через `snapshot.get('name').getAsString()`.
Изменение снимка не меняет запись; для этого нужен `edit`.

Для автодополнения в редакторе можно скопировать
[kubethaum.d.ts](../types/kubethaum.d.ts) и [globals.d.ts](../types/globals.d.ts)
в `kubejs/types/`, а [шаблон jsconfig](../types/jsconfig.example.json) — в
`kubejs/jsconfig.json`. Если настройки уже существуют, добавьте `types/**/*.d.ts`
в `include`. Эти файлы не нужны игре; если другие декларации уже объявляют
`ThaumaturgeEvents` и `Thaumaturge`, не подключайте `globals.d.ts` второй раз.

## Проверка скриптов командой

Пока мир открыт, сохраните изменения в редакторе и выполните в чате:

```text
/kubethaum check
```

Команда доступна операторам с уровнем прав 2 и выше, а также из консоли сервера.
В одиночной игре нужны разрешённые команды. Она читает **свежие файлы с диска**:
предварительно выполнять `/reload` или переоткрывать мир не нужно.

Например, если вы случайно написали `stage.obtain('minecraft:iron_ingot', 0)`,
команда покажет сообщение примерно такого вида:

```text
kubejs/server_scripts/my_research.js:12: Research 'pack:first_steps', stage[0], field 'obtain': Item amount must be positive
```

Здесь `my_research.js:12` — файл и строка вызова, `pack:first_steps` — запись,
`stage[0]` — первый этап, `obtain` — требование, в котором обнаружена проблема.
Исправьте количество, сохраните файл и снова выполните команду.

Что проверяется:

- Синтаксис серверных скриптов в `kubejs/server_scripts`, включая вложенные папки.
- Выполнение `ThaumaturgeEvents.research` на временных копиях данных.
- Ошибки методов: количества, сложность, индексы этапов, обязательный текст.
- Повторные ID, отсутствующие категории и исследования, циклы зависимостей.
- Ссылки на предметы, аспекты, теги и формат данных через проверки KubeThaum
  и родные кодеки Thaumaturge.

Для ошибок выполнения обычно показывается строка конкретного вызова.
Для ошибки итоговой структуры или родного кодека может быть указана строка
связанного объявления: в сообщении также будут ID записи и описание причины.
В чат выводятся первые восемь проблем, полный список — в `logs/latest.log`.
Учитываются приоритеты и свойства файлов KubeJS, включая `ignored`, `requires`
и `packmode`. Для порядка загрузки действуют те же правила, что и в игре.

**Проверка ничего не применяет к миру.** Она не открывает исследования,
не меняет прогресс игроков, не заменяет работающие обработчики KubeJS и
не обновляет рецепты. После успешной проверки заново откройте мир,
чтобы применить исследования обычным способом.

У проверки есть границы: клиентские и стартовые скрипты не проверяются.
Тела `ServerEvents` — в том числе рецепты — не выполняются; их синтаксис
проверяется вместе с файлом. Временный контекст предоставляет JavaScript
и API исследований, но не `Java`, `global`, `Item`, сервер и другие игровые
объекты. Если исследованию нужен такой объект, команда сообщит об ошибке
или неполной проверке; это само по себе не означает, что скрипт ошибочен в игре.
Данные, генерируемые другими событиями, используются в уже загруженном виде.

Поэтому успешный результат означает «ошибок в проверенной части нет»,
а не полную проверку всей сборки. Для повторяющихся зависаний выполнение
ограничено по инструкциям и времени. Очень большие наборы файлов
(более 256 файлов, 256 КиБ на файл или 4 МиБ суммарно) команда не проверяет.

## Если что-то не появилось

| Симптом | Что проверить |
| --- | --- |
| Запись не появилась после изменения | Заново откройте мир; `/reload` не обновляет исследования |
| Запись не видна во вкладке | Проверьте `category`, координаты, родителей и условие открытия вкладки |
| Видны ключи вместо названий | Проверьте `ClientEvents.lang`, язык и наличие клиентского файла |
| Старые требования остались | Методы добавляют условия; используйте нужный `clear...` перед заменой |
| Ошибка повторного ID | Не устанавливайте две копии одного примера; для правки используйте `edit` |
| Рецепт показан, но не закрыт | Добавьте `Thaumaturge.requireResearch` в событии рецептов |
| Не найден родитель или предмет | Проверьте namespace и ID; наличие мода не гарантирует наличие любого ID |

Ошибки скриптов ищите в `kubejs/logs/server.log` и `logs/latest.log`.
Например, `Research 'pack:first_steps', stage[0], field 'obtain'` указывает
на требование предметов **первого** этапа этой записи.
Начинайте с одного рабочего примера и добавляйте по одному изменению:
так проще увидеть, какая строка повлияла на результат.
