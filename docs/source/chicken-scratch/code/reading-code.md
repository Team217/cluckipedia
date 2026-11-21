# How to read code

Professional programmers spend far more time *reading* code than writing
it. This page teaches the habits and tools that make a large codebase
manageable.

## The mindset

1. **You don't need to understand everything.** Pick one question ("what
   happens when Submit is tapped?") and follow only the code that answers
   it.
2. **Start from something you can see.** A label on screen, a button, a
   message in a log. Search for its text, and you've found the code.
3. **Read names first, bodies second.** Good names (`saveLocal`,
   `recordsNewerThan`, `mergeRemote`) tell you what something does. Only
   open the body when you need *how*.
4. **Keep notes.** A scrap of paper with "Submit → `_submit()` →
   `writeFile()` → `saveLocal()`" is how everyone keeps their place.
5. **Run it and change it.** Put in a `print`, change a number, see what
   happens. You can always undo with Git.

## VS Code tools you'll use constantly

| Action | Keys | Use it to… |
|---|---|---|
| **Go to file** | Ctrl+P, then type part of a name | Open `scout_database.dart` without hunting through folders |
| **Search everywhere** | Ctrl+Shift+F | Find where a word or label appears in the whole project |
| **Go to definition** | F12 (or Ctrl+click) | Jump from a use of `saveLocal` to where it's written |
| **Find all references** | Shift+F12 | See every place that calls `saveLocal` |
| **Go back** | Alt+← | Return to where you were before jumping |
| **Outline** | Explorer panel → Outline | See every class and method in the open file |
| **Go to symbol in file** | Ctrl+Shift+O | Jump to a method by name in the open file |
| **Peek** | Alt+F12 | Look at a definition without leaving your place |
| **Rename symbol** | F2 | Rename a method *everywhere* safely (not find-and-replace) |
| **Hover** | Mouse over a name | See its type and its `///` or `"""` documentation |

F12, Shift+F12 and Alt+← together are how you "walk" through code: jump in,
see who calls this, jump back out.

```{admonition} Example: find what the Submit button does
:class: note
1. Ctrl+Shift+F, search `'Submit'`. One hit is in
   `lib/pages/game_periods/submit_data_page.dart`, on a `FilledButton`
   whose `onPressed: _submit`.
2. Ctrl+click `_submit`. It shows a dialog; its Submit button calls
   `widget.scoutData.writeFile()`.
3. Ctrl+click `writeFile`. You're in `scout_data_model.dart`; it writes a
   file and calls `ScoutDatabase.instance.saveLocal(...)`.
4. Ctrl+click `saveLocal`. You're in the database code.

Four jumps from a button to the database. The
[Walkthroughs](walkthroughs.md) page does this for every main feature.
```

## Reading the tests

The tests (`test/` and `companion/tests/`) are the best examples of **how
each piece is meant to be used**, because every test sets something up,
calls it and checks the answer. If you want to understand
`ScoutDatabase.mergeRemote`, read the test
`merge keeps the newest version of each record` in
`test/scout_data_store_test.dart` before the method itself.

## Debugging: stopping the program to look inside

A **debugger** pauses the program at a line you choose (a **breakpoint**)
so you can look at every variable.

**Tablet app:**

1. Open the project in VS Code with the tablet (or emulator) connected.
2. Click left of a line number, for example inside `saveLocal`, to set a
   red-dot breakpoint.
3. **Run → Start Debugging** (F5). Use the app; when that line runs, the app
   freezes and VS Code shows the variables.
4. **F10** runs the next line, **F11** steps *into* a call, **F5**
   continues.

**Companion app:** same idea. Open `companion/` in VS Code, choose the
`.venv` Python interpreter (bottom right), set a breakpoint, and start
debugging with module `chicken_companion`.

**Quick and simple:** `print('fuel is $fuel');` (Dart) or
`print(f"fuel is {fuel}")` (Python) shows a value in the terminal
running `flutter run` or the companion. Remove prints before committing.

## Naming and style conventions in this project

Knowing the conventions lets you guess what something is from its name.

**Dart (tablet):**

| Convention | Meaning | Example |
|---|---|---|
| `_name` | **Private**: only usable inside this file | `_onBytes`, `_DatabaseStore` |
| `UpperCamelCase` | A class, enum or type | `ScoutDatabase`, `TileType` |
| `lowerCamelCase` | Variables, methods | `saveLocal`, `teleopFuelScored` |
| `XyzPage` + `_XyzPageState` | A screen with state: the widget and the object that holds its changing values | `MatchListPage`, `_MatchListPageState` |
| `XyzService` with `.instance` | A **singleton** service that lives as long as the app | `BluetoothSyncService.instance` |
| `get…` / `set…` | Getter and setter properties (older code in `ScoutData`) | `getAutonFuelScored`, `setAutonFuelScored` |
| `/// …` above something | Its documentation; shown when you hover | |
| `[name]` inside a `///` comment | A reference to a parameter or class | `/// Merges [values] into …` |

**Python (companion):**

| Convention | Meaning | Example |
|---|---|---|
| `_name` | Private (by convention) | `_migrate`, `_bump` |
| `UpperCamelCase` | Class | `Store`, `LinkClient` |
| `snake_case` | Functions, variables | `import_records`, `parse_file` |
| `"""…"""` first in a function | Its docstring (documentation) | |
| `x: int`, `-> list[str]` | Type hints: what type a value should be | `def count(self) -> int:` |
| `@dataclass` | A class that's mostly data; Python writes `__init__` for you | `RadioSettings`, `PollResult` |
| `@property` | A method you read like a field: `admin.version`, not `admin.version()` | `Admin.version` |

## Reading the history

Git remembers why code changed:

- **Source Control panel → file → right-click → Open Timeline** shows the
  commits that touched a file.
- `git log --oneline -- lib/data/scout_database.dart` lists them in a
  terminal; `git show <commit>` shows what changed and the message
  explaining why.
- `git blame lib/data/scout_database.dart` shows who last changed each line
  (VS Code's **GitLens** extension shows this inline).

The commit messages in this project are long on purpose: they explain the
reasons behind changes.

## When you're lost

- Go back up a level: what *file* am I in, what is it for? (The
  [Tablet code tour](../how/tablet-code-tour.md) and
  [Companion code tour](../how/companion-code-tour.md) say what each file is
  for in one line.)
- Re-read the doc comment at the top of the class.
- Find a test that uses it.
- Ask: "who calls this?" (Shift+F12). The answer usually explains why it
  exists.
