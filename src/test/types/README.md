# Проверка деклараций

Необязательная проверка для разработки; Java/Gradle не требуют Node.js.
TypeScript можно установить в игнорируемую служебную папку:

```powershell
npm.cmd install --prefix .tools/typescript --no-audit --no-fund --ignore-scripts typescript@5.9.3
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --target ES2020 src/test/types/api.ts
$examples = @(rg --files examples/standalone -g '*.js')
node .tools/typescript/node_modules/typescript/lib/tsc.js --strict --noEmit --allowJs --checkJs --target ES2020 types/globals.d.ts src/test/types/kubejs-stubs.d.ts @examples
```

`api.ts` проверяет вывод типов callback, цепочки методов, сохранение типа рецепта
и ожидаемые ошибки неправильных вызовов. Host-декларации используются только
для проверки примеров; они не устанавливаются вместе с публичными декларациями.
