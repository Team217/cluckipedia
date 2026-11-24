# Git Basics

**Git** tracks every change to our code. It lets us see exactly which code was on the robot in any
match, undo mistakes, and let many people work at once without overwriting each other.
**GitHub** is the website that hosts our Git repositories and where we review each other's code.

## Core ideas

| Term | Meaning |
|---|---|
| **Repository (repo)** | A project folder whose history Git tracks |
| **Commit** | A saved snapshot, with a message describing the change |
| **Branch** | An independent line of commits. Work happens on branches, not on `main` |
| **Remote** (`origin`) | The copy on GitHub |
| **Push / Pull** | Send your commits to GitHub / bring others' commits down |
| **Pull Request (PR)** | A request to merge your branch into `main`, where it gets reviewed |
| **Merge conflict** | Two branches changed the same lines; a human has to choose the result |

## The everyday loop

```powershell
git switch main
git pull                                              # get the latest code
git switch -c user/smith.jane/2027-comp/intake-roller # make a branch
# ... edit code ...
git status                                            # what changed?
git diff                                              # show the exact changes
git add src/main/java/frc/robot/subsystems/intake     # stage the files you mean to commit
git commit -m "Add intake roller IO layer"
git push -u origin user/smith.jane/2027-comp/intake-roller
```

Then open a pull request on GitHub.

:::{admonition} Good commit messages
:class: tip
Write in the imperative and say **what and why**:
`Lower pivot kP to stop oscillation at deploy`. Avoid `fixed stuff` or `asdf`.
Make small commits. If you can't describe a commit in one line, split it up.
:::

## Things you should NOT commit

- Build output (`build/`, `bin/`). The `.gitignore` already handles these.
- Robot log files (`*.wpilog`, `*.revlog`). Store these in the team's log archive instead.
- Personal settings files.

Always run `git status` before `git add` and check what you're about to commit. Avoid `git add .`
until you're sure about everything in the folder.

## Undoing things

| Situation | Command |
|---|---|
| Throw away changes to one file you haven't committed | `git restore path/to/File.java` |
| Unstage a file | `git restore --staged path/to/File.java` |
| Undo a commit that was already pushed | `git revert <commit-sha>` (makes a new commit that reverses it) |
| See history | `git log --oneline --graph` |

:::{danger}
Never use `git push --force` on a shared branch, and never commit directly to `main`.
:::

## Resolving a merge conflict

When Git can't merge automatically, you'll see something like this in the file:

```java
<<<<<<< HEAD
public static final double kP = 0.5;
=======
public static final double kP = 0.8;
>>>>>>> user/smith.jane/2027-comp/pivot-tuning
```

1. Decide what the correct code is. Talk to the other author if you're not sure.
2. Delete the markers (`<<<<<<<`, `=======`, `>>>>>>>`) and leave the correct code.
3. `git add` the file, then `git commit`.
4. **Build and test** before pushing. A conflict resolution can compile and still be wrong.

VS Code highlights conflicts and has "Accept Current / Incoming / Both" buttons that help.

## Learn more

- [GitHub Skills: Introduction to GitHub](https://skills.github.com/) ([offline](../../reference-materials/offline/git-reference.md#working-on-github-needs-internet))
- [Learn Git Branching](https://learngitbranching.js.org/) ([offline](../../reference-materials/offline/git-reference.md#practice-repository-offline)) (interactive, highly recommended)
- [WPILib: Git version control](https://docs.wpilib.org/en/stable/docs/software/basic-programming/git-getting-started.html) ([offline](../../reference-materials/offline/git-reference.md))
