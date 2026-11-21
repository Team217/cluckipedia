# Maintaining

Recipes for the jobs that come up.

| I want to… | Page |
|---|---|
| Get the app ready for a new game | [Getting ready for a new season](new-season.md) |
| Add one new thing for scouts to record | [Worked example: add a field](add-a-field.md) |
| Show a new stat or chart on the dashboards | [Add a metric or dashboard tile](add-metric-or-tile.md) |
| Check I haven't broken anything | [Testing](testing.md) |
| Put a new version on the tablets | [Build and install](build-and-install.md) |
| Save and share my work | [Git and teamwork](git-workflow.md) |
| Fix something that's going wrong | [Troubleshooting](troubleshooting.md) |
| Know about the rough edges | [Known issues](known-issues.md) |
| Change these docs | [Updating these docs](updating-docs.md) |

## Golden rules

1. **Work on a branch**, never straight on `main`.
2. **Run both test suites** before sharing a change.
3. **Never change what a stored value means.** Add a new field instead.
   Old records keep the old meaning forever.
4. **Every database change needs a migration** (see the
   [database pages](../database/index.md)).
5. **Update the docs in the same change** as the code.
6. **Test on a real tablet** before an event, especially Bluetooth and the
   LoRa/TCP link, which the automated tests can't fully cover.

```{rubric} Table of contents
```

```{toctree}
:maxdepth: 1

new-season
add-a-field
add-metric-or-tile
testing
build-and-install
git-workflow
troubleshooting
known-issues
updating-docs
```
