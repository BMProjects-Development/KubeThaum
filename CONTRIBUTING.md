# Working on KubeThaum

Use the checked-in Gradle wrapper with Java 25:

```sh
./gradlew build releaseArtifacts runGameTestServer
```

This runs the Java checks and GameTests and stages the mod JAR, sources JAR,
and the ZIP with examples, guides, and release history in `build/release/`.
Dependency versions are maintained in `gradle.properties`.

## Release automation

GitHub Actions builds and tests KubeJS 8.0.6 on NeoForge `.109` and `.115`,
and KubeJS 8.0.7 on `.110` and `.115`. KubeJS 8.0.7 requires NeoForge
`26.1.2.110` or newer; it cannot be used on `.109`.
Pull requests and other branches run checks. A successful build on the default
branch, or a matching `v<version>` tag, can publish a release.

- Keep `mod_version` unchanged to replace that version's release and move its
  tag to the tested commit. Releases for other versions are preserved.
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
