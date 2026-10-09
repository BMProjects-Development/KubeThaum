// Test-only host declarations so standalone examples can be checked without an editor plugin.
declare const ServerEvents: {
  recipes(callback: (event: {
    custom(json: Record<string, unknown>): KubeThaum.Recipe
  }) => void): void
}
declare const ClientEvents: {
  lang(locale: string, callback: (event: { add(key: string, value: string): void }) => void): void
}
