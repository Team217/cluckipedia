# Git Reference

Local replacement for the interactive Git courses linked from [Git Basics](../../training-materials/git/basics.md)
([Learn Git Branching](https://learngitbranching.js.org/),
[GitHub Skills](https://skills.github.com/),
[WPILib's Git guide](https://docs.wpilib.org/en/stable/docs/software/basic-programming/git-getting-started.html)).
The interactive sites let you practice in a browser. Here you practice in a real repository
instead, which works offline.

## How Git stores history

A commit is a snapshot of every file plus a pointer to its **parent** commit. A **branch** is just a
movable label pointing at one commit. `HEAD` means "the commit you're on right now."

```text
main:            A───B───C
                          \
feature:                   D───E   ← HEAD
```

- `git commit` adds a new commit after `HEAD` and moves the current branch label to it.
- `git switch <branch>` moves `HEAD` to another branch and updates your files to match.
- **Merging** `feature` into `main` creates a commit with two parents that combines both histories.

## Practice repository (offline)

Make a throwaway repo to experiment in:

```powershell
mkdir git-practice
cd git-practice
git init
"first line" | Out-File notes.txt
git add notes.txt
git commit -m "First commit"
```

### Exercise 1: branches

```powershell
git switch -c feature
"feature line" | Add-Content notes.txt
git commit -am "Add feature line"
git switch main
type notes.txt          # the feature line isn't here
git merge feature
type notes.txt          # now it is
git log --oneline --graph --all
```

### Exercise 2: a merge conflict

```powershell
git switch -c left
"LEFT" | Out-File notes.txt
git commit -am "Left change"
git switch main
git switch -c right
"RIGHT" | Out-File notes.txt
git commit -am "Right change"
git switch main
git merge left          # fast-forward, no conflict
git merge right         # CONFLICT
```

Open `notes.txt`, resolve the conflict (see [Git Basics](../../training-materials/git/basics.md#resolving-a-merge-conflict)),
then `git add notes.txt` and `git commit`.

### Exercise 3: undoing

```powershell
"oops" | Add-Content notes.txt
git restore notes.txt         # discard the uncommitted change
"bad" | Add-Content notes.txt
git commit -am "Bad commit"
git revert HEAD               # new commit that undoes it; history is kept
git log --oneline
```

### Exercise 4: looking around

```powershell
git log --oneline --graph --all   # history as a graph
git show HEAD~1                   # what changed one commit ago
git diff main feature             # differences between two branches
git blame notes.txt               # who last changed each line
```

## Command cheat sheet

| Goal | Command |
|---|---|
| See what's changed | `git status`, `git diff`, `git diff --staged` |
| Stage / unstage | `git add <path>`, `git restore --staged <path>` |
| Commit | `git commit -m "message"` |
| New branch from current | `git switch -c <name>` |
| Change branch | `git switch <name>` |
| List branches | `git branch -a` |
| Get remote changes | `git pull` (fetch + merge) |
| Bring `main` into your branch | `git switch my-branch` then `git merge main` |
| Publish a branch | `git push -u origin <name>` |
| Set work aside temporarily | `git stash`, then later `git stash pop` |
| Undo a pushed commit safely | `git revert <sha>` |

## Working on GitHub (needs internet)

When you are back online:

1. `git push -u origin <branch>`.
2. On the repository page on GitHub, click **Compare & pull request** (it appears after a push), or
   go to **Pull requests → New**.
3. Set **base** to `main` and **compare** to your branch. Write a title and description (what, why,
   how tested).
4. Pick **Reviewers** on the right side.
5. Reviewers use the **Files changed** tab to comment on lines, then **Review changes →
   Approve / Request changes**.
6. Push more commits to fix comments. The PR updates automatically.
7. When it's approved and checks pass, click **Merge pull request**, then delete the branch.

You can do all your commits offline and push everything later. Git doesn't need a connection
until you `push` or `pull`.

## GitHub Desktop equivalents

| Command line | GitHub Desktop |
|---|---|
| `git status` / `git diff` | The **Changes** tab |
| `git add` + `git commit` | Tick files, write a summary, click **Commit to <branch>** |
| `git switch -c` | **Current Branch → New Branch** |
| `git push` | **Push origin** |
| `git pull` | **Fetch origin**, then **Pull origin** |
| Merge `main` into your branch | **Branch → Update from main** |

## `.gitignore`

A `.gitignore` file lists paths Git should never track. Ours already ignores build output. Add
patterns like `*.wpilog` if you keep logs in the project folder. A pattern only affects files that
aren't already committed.
