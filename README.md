# Cluckipedia
The knowledge base for FIRST robotics team 217 the ThunderChickens.

# Build Info:
Read the tutorial here:
https://docs.readthedocs.io/en/stable/tutorial/
https://diataxis.fr/application/
https://www.sphinx-doc.org/en/master/usage/quickstart.html
https://myst-parser.readthedocs.io/en/latest/intro.html

# Building the site locally

Needs Python 3.12. From the repository root:

```powershell
py -3.12 -m venv .venv
.venv\Scripts\python -m pip install -r docs/requirements.txt
.venv\Scripts\sphinx-build -b html docs/source docs/build/html
```

Open `docs/build/html/index.html` in a browser. Or serve it with
`.venv\Scripts\python -m http.server 8000 -d docs/build/html` and go to `http://localhost:8000`.

# Offline copy

Read the Docs builds an **HTML zip**, PDF, and EPUB of every version (version menu at the bottom of
the sidebar). To make a USB copy from a local build:

```powershell
Compress-Archive -Path docs/build/html/* -DestinationPath cluckipedia-offline.zip -Force
```
