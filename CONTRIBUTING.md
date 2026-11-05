# Working on KubeThaum

The `1.21.1` branch targets Minecraft 1.21.1 with Java 21. Use `main` for
Minecraft 26.1.2. On `1.21.1`, run the Gradle wrapper (`gradlew.bat` on Windows):

```sh
./gradlew build releaseArtifacts runGameTestServer
```

This runs the Java checks and GameTests and stages the mod JAR, sources JAR,
and `KubeThaum-1.21.1-<version>.zip` with examples, guides, and release history
in `build/release/`. Dependencies are never bundled in these artifacts.
Gradle downloads the pinned Thaumaturge file from Curse Maven. Dependency
versions are maintained in `gradle.properties`. For development against an
existing instance, pass `-PlocalModsDir=<instance>/mods` with Thaumaturge, KubeJS,
Rhino, and their runtime dependencies installed there.

## Release automation

GitHub Actions builds and tests each Minecraft version with the dependencies
pinned in its branch. A successful push to `1.21.1` publishes that branch's
GitHub release. A matching `v<version>-mc1.21.1` tag can also publish it.
Pull requests and other development branches run checks.

- Keep `mod_version` unchanged to replace that version's release and move its
  tag to the tested commit. Releases for other mod or Minecraft versions,
  including the existing `v<version>` tags, are preserved.
- To publish a new version, change `mod_version`, add `changelogs/<version>.md`,
  and add it to [the release history](changelogs/README.md). The new stable version
  becomes the latest release. At the same mod version, the `main` release takes
  precedence for GitHub's Latest badge.
- Release descriptions come from the matching changelog file. Missing notes
  fail validation before the build. Files are uploaded to a draft before publication.
- Outdated builds are skipped when the branch or tag has already moved.

The release script uses Python's standard library and the
[GitHub Releases API](https://docs.github.com/en/rest/releases/releases)
with the workflow's GitHub token. Run its tests locally with Python 3:

```sh
python -m unittest discover -s .github/scripts -p "test_*.py"
```

The workflow is in [.github/workflows/build.yml](.github/workflows/build.yml),
and publishing is handled by [.github/scripts/release.py](.github/scripts/release.py).

### CurseForge

After the GitHub release is public, the same job submits the tested mod JAR to
[KubeThaum on CurseForge](https://www.curseforge.com/minecraft/mc-mods/kubethaum).
It uses the branch's Minecraft version, NeoForge, the release notes, and the
required Thaumaturge and KubeJS dependencies. Sources and the ZIP stay on GitHub.

Generate an [author API token](https://authors.curseforge.com/account/api-tokens)
and save it as `CURSEFORGE_TOKEN` in the repository's Actions secrets. This
setup applies to both release branches. If the token is missing, the CurseForge
step fails with setup instructions and the GitHub release stays available.

The GitHub release notes record CurseForge file IDs and JAR digests in a hidden
comment. Preserve this comment when editing the notes. Reruns skip identical
JARs. A changed JAR gets a new CurseForge file, and previously uploaded files
stay available. If a run stops after CurseForge accepts the upload and before
the receipt is saved, check the CurseForge author dashboard before retrying.
CurseForge reviews uploaded files before they appear publicly.
