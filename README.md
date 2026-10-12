# KubeThaum

KubeJS integration for **Thaumaturge**. Create and customize research entries
and Thaumonomicon tabs with JavaScript, using Thaumaturge's native research system.

- Create, edit, replace and copy research entries.
- Configure stages, item requirements, knowledge, warp and additional pages.
- Add custom tabs and research dependencies.
- Gate supported Thaumaturge recipes behind research progress.
- Create and rebalance Thaumaturge recipes with native KubeJS filters and replacements.
- Configure infernal furnace bonus outputs, quantities and chances.
- Check saved research scripts with `/kubethaum check` without applying changes.

**Minecraft 26.1.2 · NeoForge 26.1.2.109+ (before 26.2) · Java 25**

## Guides

- [Руководство на русском](docs/guide-ru.md)
- [English guide](docs/guide-en.md)
- [Рецепты и переработка — 1.1](docs/recipes-ru.md)
- [Recipes and processing — 1.1](docs/recipes-en.md)

## Development and releases

Java 25 and the Gradle wrapper are sufficient:

```sh
./gradlew build releaseArtifacts runGameTestServer
```

GitHub Actions builds and tests NeoForge 26.1.2.109 and 26.1.2.115.
After both pass, a push to the default branch publishes a new version from
`mod_version` in `gradle.properties`; a matching `v<version>` tag also triggers
publication. Existing releases are preserved. Pull requests only build and test.
Release files are staged in `build/release/`: the mod JAR, sources JAR, and a ZIP
with the mod, JavaScript examples, and guides.

[Downloads](https://github.com/BMProjects-Development/KubeThaum/releases)
· [Builds](https://github.com/BMProjects-Development/KubeThaum/actions/workflows/build.yml)
