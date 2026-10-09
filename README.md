# KubeThaum

KubeJS-интеграция для исследований **Thaumaturge**. Добавляет событие
`ThaumaturgeEvents.research` для создания и изменения записей и вкладок таумономикона.
Новые текстуры, модели и предметы не нужны.

## Совместимость

Версия 0.1.1 рассчитана на **Minecraft 26.1.2, Java 25, NeoForge 26.1.2.109 и выше,
KubeJS 26.1.2-8.0.6, Rhino 2101.2.8-build.91 и Thaumaturge 1.0.2**.
Установите также зависимости Thaumaturge: Curios и Lithostitched.

В метаданных разрешён NeoForge **`[26.1.2.109,26.2)`**; сборка по умолчанию
использует 109. Проверены загрузка мира и GameTest на **109 и 115**.
Будущие версии внутри диапазона потребуют проверки при изменениях API.

Выпущенный KubeJS 8.0.6 использует старую сигнатуру `RegistryDataLoader.load`,
из-за чего загрузка мира на 114/115 падает в `kubejs.mixins.json:WorldLoaderMixin`.
KubeThaum содержит небольшую встроенную библиотеку совместимости, использующую
официальный NeoForge `ClassProcessor`: если загрузчик имеет аргумент `pendingTags`,
она исправляет только устаревший дескриптор инъекции KubeJS в памяти до обработки Mixin.
На старом API исправление не применяется. Уже исправленный дескриптор остаётся без изменений.
Файлы KubeJS на диске менять не нужно.

Хук исследований также выбирается по доступной сигнатуре API и передаёт
`pendingTags` без изменений. Исходники исправления:
[KubeJSWorldLoaderProcessor.java](src/compat/java/org/bmp/kubethaum/compat/KubeJSWorldLoaderProcessor.java).
Описание API загрузчика: [NeoForge ClassProcessor](https://github.com/neoforged/FancyModLoader/blob/main/loader/src/main/java/net/neoforged/neoforgespi/transformation/ClassProcessor.java).

## Быстрый старт

1. Положите JAR аддона в `mods` на клиенте и сервере.
2. Скопируйте [research.js](examples/server_scripts/research.js) в
   `kubejs/server_scripts/`, а [research_lang.js](examples/client_scripts/research_lang.js)
   в `kubejs/client_scripts/`.
3. Перезапустите сервер или заново откройте мир. После изменения клиентского перевода
   перезагрузите ресурсы либо перезапустите клиент.
4. Откройте таумономикон: появится вкладка «Металлургия» с двумя исследованиями.

`/reload` обновляет рецепты, но **не перезагружает реестры исследований**.
После изменения `ThaumaturgeEvents.research` требуется повторная загрузка мира.
Существующий прогресс игроков сохраняется по ID. Перестановка этапов уже начатого
исследования меняет смысл сохранённых индексов; учитывайте это при обновлении сборки.

Минимальный пример в `server_scripts`:

```js
ThaumaturgeEvents.research(event => {
  event.create('pack:metals')
    .category('thaumaturge:alchemy')
    .title('Изучение металлов')
    .icon('minecraft:iron_ingot')
    .position(4, 2)
    .stage(stage => {
      stage.text('Соберите восемь железных слитков.')
      stage.obtain('minecraft:iron_ingot', 8)
    })
    .stage(stage => stage.text('Основные свойства металлов изучены.'))
})
```

Название и текст могут быть обычной строкой или ключом перевода. Название вкладки
использует ключ `research_category.<namespace>.<path>`, например
`research_category.pack.metallurgy`. Его задаёт клиентский `ClientEvents.lang`.

## API

Все ID задаются явно с namespace: `pack:entry`, `minecraft:iron_ingot`.
Несколько обработчиков события выполняются последовательно и видят изменения предыдущих.

| Метод события | Действие |
| --- | --- |
| `create(id)` | Новое исследование; повторный ID — ошибка |
| `replace(id)` | Полностью заменить запись новым конструктором |
| `edit(id)` / `edit(id, callback)` | Изменить существующую запись, сохранив остальные поля |
| `category(id)` | Создать вкладку |
| `editCategory(id)` | Изменить существующую вкладку |
| `exists(id)` | Проверить наличие исследования |
| `get(id)` / `getCategory(id)` | Получить независимую JSON-копию текущей записи |

Изменения читают приоритетную версию записи из активных datapack, включая `kubejs/data`
и сгенерированные данные KubeJS. KubeThaum применяет свои изменения последним при
загрузке реестров. `get()` возвращает копию; её изменение не заменяет вызов `edit()`.

### Исследования

| Метод конструктора | Назначение |
| --- | --- |
| `category(id)`, `title(text)`, `position(column, row)` | Категория, название, координаты |
| `icon(id)` | Добавить иконку предмета, путь `.png` или `focus:namespace:part` |
| `parent(id)` | Требовать завершение родителя |
| `parent(id, stage)` | Требовать этап родителя; номер **с 1** (`1` = начат) |
| `hiddenParent(id)` | Родитель без линии на дереве; также поддерживается `parent('~pack:id@2')` |
| `sibling(id)` | Родная декоративная/каскадная связь Thaumaturge |
| `flag(name)` | `round`, `spiky`, `reverse`, `hidden`, `autounlock`, `hex` |
| `autoUnlock()` | Сокращение для флага `autounlock` |
| `noteAspect(id, amount)`, `complexity(1..3)` | Аспекты и сложность исследовательской записки |
| `stage(callback)` | Добавить этап |
| `editStage(index, callback)` | Изменить этап; индекс **с 0** |
| `insertStage(index, callback)`, `removeStage(index)` | Вставить/удалить этап |
| `addendum(callback)` | Дополнительная страница после завершения исследования |
| `clearStages()`, `clearParents()`, `clearIcons()`, `clearAddenda()` | Очистить соответствующий список |

Каждое исследование требует хотя бы один этап; каждый этап и addendum требуют `text()`.
Иконки, родители и ссылки на рецепты не дублируются при повторном добавлении.

### Этапы и дополнительные страницы

`text(text)`, `recipe(id)` и `requireResearch(id)` доступны у этапов и addenda.
У addendum `requireResearch` управляет показом страницы, у этапа — завершением этапа.

Дополнительно у этапов:

| Метод | Назначение |
| --- | --- |
| `obtain(itemOrTag, amount = 1)` | Родное требование наличия предметов в инвентаре |
| `craft(itemOrTag, amount = 1)` | Родное требование крафта |
| `obtainAny([itemIds], amount)`, `craftAny([itemIds], amount)` | Суммарное количество любых перечисленных предметов |
| `obtain(itemOrTag, amount, components)` / `craft(...)` | Требование с JSON-патчем компонентов |
| `knowledge(type, category, amount)` | Награда знаниями |
| `requireKnowledge(type, category, amount)` | Стоимость знаниями |
| `warp(amount)` | Варп этапа |
| `construct(x, y, z, cells, cost = [])` | Родная схема мультиблока |
| `clearObtain()`, `clearCraft()`, `clearRequiredResearch()`, `clearRequiredKnowledge()`, `clearRecipes()` | Очистить требования/рецепты этапа |

Теги пишутся как `#minecraft:planks`. `components` использует формат компонентов
Minecraft 26.1.2. Тип знаний — `theory` или `observation`; количество — **сырые очки**
Thaumaturge: 32 очка составляют одну теорию, 16 — одно наблюдение.
`obtain` использует поведение самого Thaumaturge, а не историю подбора предметов.
Отслеживание `craft` также полностью выполняет Thaumaturge.

В `construct` нужно ровно `x*y*z` ячеек, верхний слой первым, x-major порядок.
Ячейка — ID предмета, `#tag` или `''`. Стоимость:
`[{ aspect: 'thaumaturge:ignis', amount: 5 }]`.

### Вкладки

`style(existingCategoryId)` копирует оформление существующей вкладки.
`icon(texture)`, `background(texture)`, `overlay(texture)`, `noOverlay()` позволяют
указать уже имеющиеся текстуры. Иконка вкладки — **текстура**, иконка исследования
может быть предметом. `index(nonNegativeNumber)` задаёт порядок.
`formula(aspectId, amount)` задаёт формулу знаний для исследовательского стола.
`requireResearch(id)` / `noRequiredResearch()` задают условие открытия вкладки.
Пустая формула допустима, но собственную формулу желательно задать явно.

### Ограничение рецептов исследованиями

```js
ServerEvents.recipes(event => {
  const recipe = event.custom({
    type: 'thaumaturge:crucible',
    catalyst: 'minecraft:iron_ingot',
    aspects: [{ aspect: 'thaumaturge:ordo', amount: 5 }],
    result: { id: 'minecraft:gold_ingot', count: 1 }
  }).id('pack:gold')
  Thaumaturge.requireResearch(recipe, 'pack:metals')
})
```

`Thaumaturge.requireResearch(recipe, entry[, stage])` поддерживает родные
`arcane_workbench_shaped`, `arcane_workbench_shapeless`, `crucible`, `infusion`,
`infusion_enchantment` и три `dust_trigger_*` типа рецептов Thaumaturge.
Для неподдерживаемого типа, отсутствующего исследования и слишком большого номера
этапа будет ошибка. Вызов не ограничивает обычный крафт
Minecraft или рецепты других модов.

Для ручного JSON доступен `research: Thaumaturge.gate(entry[, stage[, negate]])`.
У **ResearchGate** индекс этапа родной, **с 0**, в отличие от `parent(id, stage)`.
Без stage требуется полное завершение. `negate = true` инвертирует условие.
`stage.recipe(id)` лишь показывает рецепт на странице; ограничение крафта задаётся отдельно.

## Проверки и сборка

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
# Проверка более нового NeoForge (кавычки нужны в PowerShell):
.\gradlew.bat runGameTestServer '-Pneo_version=26.1.2.115'
```

Gradle использует Java 25. При необходимости задайте `JAVA_HOME` на JDK 25.
JAR находится в `build/libs/kubethaum-26.1.2-0.1.1.jar`.
KubeJS, Rhino и зависимости Thaumaturge скачиваются из Maven/CurseMaven
и не входят в JAR аддона. Собственная библиотека совместимости вложена в JAR автоматически.
Для локальной разработки можно использовать
`-PlocalModsDir=D:/path/to/instance/mods` вместо скачивания зависимостей.

`build` запускает 22 проверки конструкторов и прогрессии и 9 проверок совместимости
на байткоде выпущенного KubeJS (старый/новый API и повторная обработка).
GameTest запускает
настоящие KubeJS-скрипты из примеров в отдельном `run-gametest`, проверяет
загрузку реестров родными кодеками, требования через теги предметов
и блокировку рецепта до завершения исследования.
Тестовые классы и примеры не входят в игровой JAR.

Проверяются повторные ID, отсутствующие категории/родители/исследования, номера
этапов родителей, циклы полного завершения, включая условия открытия вкладок.
Связи на промежуточные этапы не считаются циклами полного завершения.
Родные кодеки дополнительно проверяют ссылки на предметы, аспекты и компоненты.
Ошибки обработчика прерывают загрузку исследований; детали находятся в логах KubeJS.
Произвольные задания на убийство/посещение измерений и универсальные ограничения
рецептов сторонних модов в эту версию не входят.
