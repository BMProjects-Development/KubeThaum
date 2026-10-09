# Руководство KubeThaum 1.0

[English](guide-en.md) · [Главная](../README.md)

KubeJS-интеграция для исследований **Thaumaturge**. Добавляет событие
`ThaumaturgeEvents.research` для создания и изменения записей и вкладок таумономикона.
Новые текстуры, модели и предметы не нужны.

- [Установка и совместимость](#совместимость)
- [Быстрый старт](#быстрый-старт)
- [API](#api)
- [Самостоятельные примеры](#самостоятельные-примеры)
- [Подсказки в редакторе](#подсказки-в-редакторе)
- [Проверки и сборка](#проверки-и-сборка)
- [Автоматическая сборка и релизы](#автоматическая-сборка-и-релизы)
- [Проверки перед публикацией](#проверки-перед-публикацией)

## Совместимость

Версия 1.0 рассчитана на **Minecraft 26.1.2, Java 25, NeoForge 26.1.2.109 и выше,
KubeJS 26.1.2-8.0.6, Rhino 2101.2.8-build.91 и Thaumaturge 1.0.2**.
Установите также зависимости Thaumaturge: Curios и Lithostitched.

| Компонент | Проверенная версия / ограничение |
| --- | --- |
| Minecraft | 26.1.2 |
| Java | 25 |
| NeoForge | от 26.1.2.109 до 26.2, не включая 26.2; проверены 109 и 115 |
| KubeJS | 26.1.2-8.0.6; в метаданных закреплена именно эта версия |
| Rhino | 2101.2.8-build.91 |
| Thaumaturge | 1.0.2; диапазон метаданных `[1.0.2,1.1)` |
| Curios / Lithostitched | зависимости Thaumaturge; в разработке используются 15.0.0+26.1.2 / 1.8.0 |

Установите одинаковые версии KubeThaum и его обязательных зависимостей на клиенте
и сервере. Better Advanced Tooltips — необязательная интеграция KubeJS,
которая присутствует в среде разработки, но не требуется для API KubeThaum.
Мод не создаёт игровое содержимое без скриптов сборки: примеры устанавливаются отдельно.
Все лицензии зависимостей остаются их собственными; лицензия KubeThaum указана в метаданных JAR.

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
[KubeJSWorldLoaderProcessor.java](https://github.com/BMProjects-Development/KubeThaum/blob/main/src/compat/java/org/bmp/kubethaum/compat/KubeJSWorldLoaderProcessor.java).
Описание API загрузчика: [NeoForge ClassProcessor](https://github.com/neoforged/FancyModLoader/blob/main/loader/src/main/java/net/neoforged/neoforgespi/transformation/ClassProcessor.java).

## Быстрый старт

1. Положите JAR аддона в `mods` на клиенте и сервере.
2. Скопируйте [research.js](../examples/server_scripts/research.js) в
   `kubejs/server_scripts/`, а [research_lang.js](../examples/client_scripts/research_lang.js)
   в `kubejs/client_scripts/`.
3. Перезапустите сервер или заново откройте мир. После изменения клиентского перевода
   перезагрузите ресурсы либо перезапустите клиент.
4. Откройте таумономикон: появится вкладка «Металлургия» с двумя исследованиями.

В [GitHub Releases](https://github.com/BMProjects-Development/KubeThaum/releases)
выбирайте `kubethaum-26.1.2-1.0.jar` для игры. `-sources.jar` предназначен
для разработчиков и не устанавливается в `mods`. ZIP содержит игровой JAR,
это руководство, примеры и декларации. Замените предыдущий JAR KubeThaum,
не оставляя одновременно две версии аддона в `mods`.

Структура файлов установленной сборки:

```text
mods/kubethaum-26.1.2-1.0.jar
kubejs/server_scripts/research.js
kubejs/client_scripts/research_lang.js
kubejs/types/kubethaum.d.ts       # только для редактора
kubejs/types/globals.d.ts         # только для редактора
kubejs/jsconfig.json             # необязательно
```

Исследования и рецепты задаются в `server_scripts`, переводы — в `client_scripts`.
На выделенном сервере клиентские переводы устанавливаются на компьютерах игроков;
серверные записи передаются клиенту родным механизмом реестров Thaumaturge.

Для отдельных задач есть [пять независимых примеров](#самостоятельные-примеры):
создание, редактирование, вкладка, ограничение рецепта и копирование.
Для автодополнения в VS Code установите [декларации API](#подсказки-в-редакторе).
После успешной загрузки в `logs/latest.log` появится строка:

```text
Research loaded: 2 added, 1 changed; categories: 1 added, 0 changed.
```

Считаются уникальные записи: несколько правок одного ID дают одно изменение.
Сообщение выводится после успешной загрузки реестров, а не после регистрации обработчиков.

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
| `copy(sourceId, newId)` | Создать независимый снимок текущей записи под новым ID |
| `category(id)` | Создать вкладку |
| `editCategory(id)` | Изменить существующую вкладку |
| `exists(id)` | Проверить наличие исследования |
| `get(id)` / `getCategory(id)` | Получить независимую JSON-копию текущей записи |

Изменения читают приоритетную версию записи из активных datapack, включая `kubejs/data`
и сгенерированные данные KubeJS. KubeThaum применяет свои изменения последним при
загрузке реестров. `get()` возвращает копию; её изменение не заменяет вызов `edit()`.

`copy()` копирует все текущие поля, включая этапы, оформление, родителей, siblings,
addenda и неизвестные JSON-поля. Учитываются предыдущие правки обработчиков.
Ключи перевода и ссылки сохраняются буквально и не переназначаются на новый ID.
Прогресс оригинала в копию не переносится; существующий целевой ID — ошибка.

```js
ThaumaturgeEvents.research(event => {
  event.copy('thaumaturge:metallurgy', 'pack:advanced_metallurgy')
    .title('Продвинутая металлургия')
    .position(4, 2)
    .clearParents().parent('thaumaturge:metallurgy')
})
```

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

### Нумерация этапов

| Вызов | Начало нумерации | Пример |
| --- | --- | --- |
| `editStage`, `insertStage`, `removeStage` | 0 | `editStage(0, ...)` меняет первый этап |
| `parent(id, stage)` | 1 | `parent('pack:metals', 1)` требует, чтобы родитель был начат |
| `Thaumaturge.requireResearch(recipe, id, stage)` / `gate` | 0 | Значение `0` требует наличия записи у игрока, без полного завершения |
| `stage[n]` / `addendum[n]` в ошибках | 0 | `stage[1]` означает второй этап |

Аргумент `stage` в условии рецепта — родное значение прогресса Thaumaturge.
Чтобы требовать полное завершение, **опустите аргумент**, а не вычисляйте последний индекс.

### Диагностика

Ошибки методов конструктора включают ID записи, индекс этапа/дополнения и поле.
Например, отрицательное количество в `obtain` и отсутствующий предмет выглядят так:

```text
Research 'pack:metals', stage[0], field 'obtain': Item amount must be positive
Research 'pack:metals', stage[0], field 'obtain[0].items': Unknown item 'minecraft:missing_item'
```

Строку исходного скрипта и стек ищите в `kubejs/logs/server.log` и `logs/latest.log`.
Некорректные статические ID предметов проверяются до загрузки изменённых записей;
теги, аспекты и компоненты окончательно проверяют родные кодеки.

## Проверки и сборка

Для сборки используйте исходный репозиторий и JDK 25:

```shell
git clone https://github.com/BMProjects-Development/KubeThaum.git
cd KubeThaum
```

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
# Проверка более нового NeoForge (кавычки нужны в PowerShell):
.\gradlew.bat runGameTestServer '-Pneo_version=26.1.2.115'
# JAR, примеры, подсказки и документация одним архивом:
.\gradlew.bat releaseArtifacts
```

Gradle использует Java 25. При необходимости задайте `JAVA_HOME` на JDK 25.
JAR находится в `build/libs/kubethaum-26.1.2-1.0.jar`.
Архив `build/distributions/KubeThaum-1.0.zip` содержит JAR в `mods`,
документацию, примеры и декларации. В игру устанавливается JAR;
примеры и подсказки копируются отдельно по инструкции.
KubeJS, Rhino и зависимости Thaumaturge скачиваются из Maven/CurseMaven
и не входят в JAR аддона. Собственная библиотека совместимости вложена в JAR автоматически.
Для локальной разработки можно использовать
`-PlocalModsDir=D:/path/to/instance/mods` вместо скачивания зависимостей.

`build` запускает 37 проверок конструкторов, копирования, диагностики и прогрессии
и 9 проверок совместимости
на байткоде выпущенного KubeJS (старый/новый API и повторная обработка).
GameTest запускает
настоящие KubeJS-скрипты из примеров в отдельном `run-gametest`, проверяет
загрузку реестров родными кодеками, требования через теги предметов
и блокировку рецепта до завершения исследования.
Также проверяются копирование родных/созданных записей и сохранение завершённого
и частичного прогресса родными дисковым и сетевым кодеками Thaumaturge.
Тестовые классы и примеры не входят в игровой JAR.

Сценарии с настоящим клиентом и повторным входом описаны в
[проверках перед публикацией](#проверки-перед-публикацией).

Проверяются повторные ID, отсутствующие категории/родители/исследования, номера
этапов родителей, циклы полного завершения, включая условия открытия вкладок.
Связи на промежуточные этапы не считаются циклами полного завершения.
Родные кодеки дополнительно проверяют ссылки на предметы, аспекты и компоненты.
Ошибки обработчика прерывают загрузку исследований; детали находятся в логах KubeJS.
Произвольные задания на убийство/посещение измерений и универсальные ограничения
рецептов сторонних модов в эту версию не входят.

## Самостоятельные примеры

Выберите нужную папку из [examples/standalone](../examples/standalone)
и перенесите содержимое её `server_scripts` в `kubejs/server_scripts/`.
В примере вкладки перенесите также `client_scripts` в `kubejs/client_scripts/` на клиенте.

| Пример | Назначение |
| --- | --- |
| [01_minimal](../examples/standalone/01_minimal/server_scripts/minimal.js) | Новая запись с двумя этапами |
| [02_edit](../examples/standalone/02_edit/server_scripts/edit.js) | Дополнение родной металлургии |
| [03_category](../examples/standalone/03_category/server_scripts/category.js) | Своя вкладка и формула; [перевод](../examples/standalone/03_category/client_scripts/category_lang.js) |
| [04_recipe_gate](../examples/standalone/04_recipe_gate/server_scripts/recipe_gate.js) | Исследование и закрытый им рецепт |
| [05_copy](../examples/standalone/05_copy/server_scripts/copy.js) | Копия родной записи с изменением первого этапа |

Каждый пример самодостаточен при наличии обязательных модов. Их новые ID различаются,
поэтому примеры могут работать вместе. Не копируйте весь `standalone` целиком
в `server_scripts`: выбирайте нужные файлы.
Полный пример металлургии находится в [research.js](../examples/server_scripts/research.js),
[edit_research.js](../examples/server_scripts/edit_research.js) и
[research_lang.js](../examples/client_scripts/research_lang.js).

## Подсказки в редакторе

1. Скопируйте [kubethaum.d.ts](../types/kubethaum.d.ts) и
   [globals.d.ts](../types/globals.d.ts) в `kubejs/types/`.
2. Если `jsconfig.json` ещё нет, скопируйте [шаблон](../types/jsconfig.example.json)
   в `kubejs/jsconfig.json`. Если файл уже есть, добавьте `types/**/*.d.ts`
   в `include`, сохранив остальные настройки.
3. Откройте папку `kubejs` в VS Code. При необходимости перезапустите JavaScript language server.

Декларации предназначены только для редактора. Они не выполняются в игре
и не заменяют проверки ID/чисел при загрузке мира.
Если другой набор деклараций уже объявляет `ThaumaturgeEvents` или `Thaumaturge`,
не подключайте `globals.d.ts` одновременно с ним. Файл `kubethaum.d.ts`
объявляет только namespace `KubeThaum` и подходит для JSDoc:

```js
/** @param {KubeThaum.ResearchEvent} event */
function registerResearch(event) {
  event.create('pack:sample').stage(stage => stage.text('Пример'))
}
ThaumaturgeEvents.research(registerResearch)
```

`get()`, `getCategory()` и `gate()` возвращают Gson JSON, а не обычный объект JS:
для чтения используйте `snapshot.get('name').getAsString()`, а не `snapshot.name`.
`edit(id, callback)` возвращает `void`; цепочку начинайте с `edit(id)`.

Для проверки деклараций разработчиком:

```powershell
npm.cmd install --prefix .tools/typescript --no-audit --no-fund --ignore-scripts typescript@5.9.3
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --target ES2020 src/test/types/api.ts
$examples = @(rg --files examples/standalone -g '*.js')
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --allowJs --checkJs --target ES2020 types/globals.d.ts src/test/types/kubejs-stubs.d.ts @examples
```

Node.js нужен только для этой проверки; Gradle и игровому моду он не требуется.

## Автоматическая сборка и релизы

Workflow [build.yml](https://github.com/BMProjects-Development/KubeThaum/blob/main/.github/workflows/build.yml)
работает на Linux с Java 25:

- Каждый push в ветку, pull request, push тега `v*` и ручной запуск запускает сборку.
- Матрица NeoForge 109/115 выполняет `build`, `releaseArtifacts` и `runGameTestServer`.
- Декларации и самостоятельные примеры дополнительно проверяются TypeScript.
- В Actions сохраняются три файла: игровой JAR, JAR исходников и ZIP с руководствами/примерами.
  Для публикации используются артефакты сборки на минимальном NeoForge 109.
- После успешной матрицы push в основную ветку или тега `v*` публикует новую версию.
  Ручной запуск из основной ветки тоже может опубликовать её.
- Версия берётся из `mod_version` в `gradle.properties`; для `1.0` создаётся тег `v1.0`.
  Вручную отправленный тег обязан совпадать с версией в файле.
- Опубликованный релиз с этим тегом сохраняется. Следующий релиз требует новой версии.
  Если тег существует без релиза и указывает на другой commit, публикация останавливается.
- PR и обычные рабочие ветки только проверяются. Для публикации используется встроенный
  `GITHUB_TOKEN` с `contents: write` в отдельном job; дополнительные секреты не нужны.

Чтобы выпустить следующую версию, измените `mod_version`, обновите документацию/декларации
при изменении API и отправьте commit в основную ветку. Либо отправьте соответствующий тег
`v<mod_version>` для подготовленного commit. Для ручного запуска откройте
Actions → Build and Release → Run workflow и выберите основную ветку.
Политики организации должны разрешать GitHub Actions и запись Releases встроенным токеном.

`releaseArtifacts` складывает только текущие файлы в `build/release/`:

```text
kubethaum-26.1.2-1.0.jar
kubethaum-26.1.2-1.0-sources.jar
KubeThaum-1.0.zip
```

Для Linux/macOS используйте `chmod +x gradlew`, затем
`./gradlew build releaseArtifacts runGameTestServer`. Gradle Wrapper и зависимости
скачиваются автоматически; глобальная установка Gradle не требуется.

## Проверки перед публикацией

Автоматические проверки подтверждают работу скриптов, кодеков и прогрессии.
GameTest не открывает GUI и не подключает настоящий клиент. Перед публикацией
проверьте следующие сценарии в отдельном мире с одинаковыми версиями модов:

- [ ] В таумономиконе видны вкладка, названия, иконки, этапы и рецепт.
- [ ] После выхода и повторного открытия мира сохраняются завершение и частичный прогресс;
  этапы и addenda не дублируются.
- [ ] После изменения только текста/названия существующего ID новый текст появляется,
  а прогресс сохраняется.
- [ ] Копия с новым ID получает независимый прогресс; оригинал сохраняет свой.
- [ ] `/reload` обновляет рецепты и не дублирует записи; исследования меняются после открытия мира заново.
- [ ] Клиент на выделенном сервере видит серверные записи; условие рецепта соответствует прогрессу.
- [ ] Переподключение к серверу сохраняет прогресс и отображение таумономикона.

Для теста сохранения не меняйте порядок/число этапов: индексы уже начатых исследований
сохраняют прежний смысл только при сохранённой структуре этапов.
