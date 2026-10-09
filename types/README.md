# Подсказки в редакторе

1. Скопируйте `kubethaum.d.ts` и `globals.d.ts` в `kubejs/types/`.
2. Если у проекта нет `jsconfig.json`, скопируйте `jsconfig.example.json`
   в `kubejs/jsconfig.json`. Откройте папку `kubejs` в VS Code.
3. В `server_scripts` появятся подсказки методов и аргументов, включая callbacks этапов.
   После установки файлов при необходимости перезапустите JavaScript language server.

Если `jsconfig.json` уже есть, добавьте `types/**/*.d.ts` в его `include`,
сохранив остальные настройки. Декларации предназначены только для редактора:
Minecraft и KubeJS их не выполняют. Они не заменяют проверки ID и чисел при загрузке мира.

Если другие декларации уже объявляют `ThaumaturgeEvents` или `Thaumaturge`,
не подключайте одновременно `globals.d.ts`: это вызовет конфликт глобальных имён.
Сам файл `kubethaum.d.ts` добавляет только namespace `KubeThaum`, пригодный для JSDoc:

```js
/** @param {KubeThaum.ResearchEvent} event */
function registerResearch(event) {
  event.create('pack:sample').stage(stage => stage.text('Пример'))
}
ThaumaturgeEvents.research(registerResearch)
```

`get()`, `getCategory()` и `gate()` возвращают Gson JSON, поэтому для чтения
используйте `get('name').getAsString()`, а не `snapshot.name`.
`edit(id, callback)` возвращает `void`; цепочку методов начинайте с `edit(id)`.
В `obtainAny`/`craftAny` перечисляются ID предметов; для одного тега используйте
`obtain('#minecraft:planks', 2)` / `craft('#minecraft:planks', 2)`.
