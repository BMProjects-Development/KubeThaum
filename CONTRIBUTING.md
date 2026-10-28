# Working on KubeThaum

This branch targets Minecraft 1.21.1. Thaumaturge is built from source until
its matching version is available on CurseForge. The tested upstream commit
is pinned as `thaumaturge_source_revision` in `gradle.properties`.

Clone [Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge), check out
that commit, and build its mod JAR with Java 25 (its build tools require it;
the mod itself targets Java 21):

```sh
git clone --branch 1.21.1 https://github.com/Leclowndu93150/Thaumaturge.git .tools/thaumaturge-1.21.1
git -C .tools/thaumaturge-1.21.1 checkout 90fe443c83cc0ce868ec7e57dc944c99694ce533
cd .tools/thaumaturge-1.21.1
./gradlew --no-daemon runData
./gradlew --no-daemon jar
```

Data generation is required: the source tree does not include all research
categories and other generated resources. Run it before the separate JAR build.

Return to KubeThaum and copy the playable JAR from Thaumaturge's `build/libs/` into
`libs/`, excluding sources and Javadoc JARs. Keep exactly one Thaumaturge JAR
there. Alternatively, pass `-PlocalModsDir=<instance>/mods` to build against
an existing installation containing Thaumaturge, KubeJS, Rhino, and their
runtime dependencies. On Windows use `gradlew.bat` for these commands.

Use the checked-in Gradle wrapper with Java 21:

```sh
./gradlew build releaseArtifacts runGameTestServer
```

This runs the Java checks and GameTests and stages the mod JAR, sources JAR,
and `KubeThaum-1.21.1-<version>.zip` with examples, guides, and release history
in `build/release/`. Dependencies are never bundled in these artifacts.
Dependency versions are maintained in `gradle.properties`.

## Release automation

GitHub Actions builds the pinned Thaumaturge source before building and testing
KubeThaum against the NeoForge versions in its matrix. Pull requests and other
branches run checks. A successful build on `1.21.1`, the default branch, or a
matching `v<version>-mc1.21.1` tag can publish a release.

- Keep `mod_version` unchanged to replace that version's release and move its
  tag to the tested commit. Releases for other mod or Minecraft versions,
  including the existing `v<version>` tags, are preserved.
- To publish a new version, change `mod_version`, add `changelogs/<version>.md`,
  and add it to [the release history](changelogs/README.md). The new stable version
  becomes the latest release; publishing an older tag does not displace a newer version.
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
