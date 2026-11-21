# Sphinx settings for previewing these docs on your own computer.
#
# The docs are published as part of Cluckipedia (github.com/Team217/cluckipedia),
# which copies this folder to docs/source/chicken-scratch/. These settings
# match Cluckipedia's docs/source/conf.py, so what you preview here looks the
# same there. Cluckipedia ignores this file.
#
#   python -m sphinx -b html docs docs/_build
#
# See docs/maintain/updating-docs.md.

project = "Chicken Scratch"
author = "ThunderChickens Programming Department"

extensions = [
    "myst_parser",
    "sphinxcontrib.mermaid",
]

# ```mermaid code blocks become diagrams (same as Cluckipedia).
myst_fence_as_directive = ["mermaid"]

root_doc = "index"
exclude_patterns = ["_build", "requirements.txt"]

html_theme = "sphinx_rtd_theme"
