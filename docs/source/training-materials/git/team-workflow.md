# Team Git Workflow

The ThunderChickens use **Git** for version control and **GitHub** to store and review code. These
rules apply to every team repository, including the robot code and this wiki.

## Rules

- **Nobody pushes directly to `main`.** Changes reach `main` only through a reviewed pull request.
  Department leads and mentors have the GitHub permission to push to `main`, but use it only in rare
  and exceptional cases.
- **Work on your own branch**, or on a branch you have permission to work on. If a branch doesn't
  have your name in it, check with its author before pushing to it.
- **Empty folders:** Git doesn't track empty folders. If you create a folder that has no content
  yet, put an empty `.gitkeep` file in it.

## Branch names

| Branch | Purpose |
|---|---|
| `main` | Always builds and always deployable. Changes only arrive through reviewed PRs. |
| `user/LASTNAME.FIRSTNAME/TARGET_ROBOT/DESCRIPTION` | Your working branch, for example `user/smith.jane/2027-comp/intake-roller` |
| `comp/COMP_ID` | Created before each event. Holds the code changed at that event. |
| `comp/COMP_ID/MATCH_ID` | Changes made for one match, merged back into the event branch. |

Branch names use forward slashes (`/`). Use lowercase for the description, with hyphens between
words.

Here's how the branches fit together over a season:

```mermaid
:config: {"gitGraph": {"showBranches": true, "showCommitLabel": false, "mainBranchName": "main"}}

gitGraph
	commit
	branch "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/FEATURE"
	checkout "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/FEATURE"
	commit
	commit
	commit
	checkout "main"
	merge "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/FEATURE"
	commit
	branch "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/BUG_FIX"
	checkout "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/BUG_FIX"
	commit
	commit
	checkout "main"
	merge "user/LASTNAME.FIRSTNAME/TARGET_ROBOT/BUG_FIX"
	commit
	branch "comp/COMP_ID"
	checkout "comp/COMP_ID"
	commit
	branch "comp/COMP_ID/MATCH_ID"
	checkout "comp/COMP_ID/MATCH_ID"
	commit
	commit
	checkout "comp/COMP_ID"
	merge "comp/COMP_ID/MATCH_ID"
	commit
	checkout "main"
	merge "comp/COMP_ID"
```

## The pull request process

1. **Keep PRs small.** One feature or fix per PR. A 2,000-line PR does not get a real review.
2. **Before opening it**, run:
   ```powershell
   ./gradlew spotlessApply
   ./gradlew build
   ```
   `spotlessApply` formats the code with google-java-format. CI fails if formatting is wrong.
   `build` also runs the [unit tests](../testing/unit-testing.md); fix any failures first.
3. **Describe it.** What changed, why, and **how you tested it** (sim, bench, robot, or log replay).
   Include an AdvantageScope screenshot for anything involving control or tuning.
4. **Request a review** from a mentor or an experienced student programmer.
5. **Respond to every comment.** Either change the code or explain why not. Push new commits to the
   same branch; the PR updates automatically.
6. The reviewer approves, and then the **author** merges.

## How to review a PR

Reviewing is a skill we teach on purpose. A useful review checks:

- [ ] Does it build, and does it do what the description says?
- [ ] Are units clear and consistent (rotations vs. radians, motor vs. mechanism)?
- [ ] Does every hardware call go through the IO layer?
- [ ] Are the new values logged so we can debug them on the field?
- [ ] Are constants in `Constants` instead of hard-coded numbers in the middle of logic?
- [ ] Are commands declaring the right requirements?
- [ ] Could anything here block the loop (`while` loops, `Thread.sleep`, slow file I/O)?
- [ ] Are names and Javadoc clear enough for someone new?

Be kind and specific: "This reads the joystick once. Pass a `DoubleSupplier` so it updates every
loop" is better than "this is wrong."

## At competition

Follow the [competition procedures](../../tc26-comp-procedures.md). Most importantly: **commit before every deploy**, so we always know exactly what code ran in
each match. AdvantageKit records the Git SHA and whether there were uncommitted changes in every log
(look for `GitSHA` and `GitDirty` in the log metadata).

## Cleaning up deleted branches

After branches are merged and deleted on GitHub, your computer still has local copies. To remove
the local branches whose GitHub branch is gone, open a **Git Bash** terminal in the repository and
run:

```bash
git fetch --prune
git branch -vv | grep ': gone]' | sed 's/^[* ]*//' | awk '{print $1}' | xargs -r git branch -D
```

:::{warning}
`git branch -D` deletes the branch even if it has commits that were never pushed. Run
`git branch -vv` first and check the list.
:::

If Git Bash says `grep` isn't found, Git's tools aren't on your `PATH`. In an **administrator**
PowerShell, install Git and add its tools folder:

```powershell
winget install --id Git.Git -e --source winget
$PATH = [Environment]::GetEnvironmentVariable("PATH", "Machine"); [Environment]::SetEnvironmentVariable("PATH", "$PATH;C:\Program Files\Git\usr\bin", "Machine")
```
