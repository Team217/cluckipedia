# Configuration file for the Sphinx documentation builder.

# -- Project information

project = 'Cluckipedia'
copyright = '2026, ThunderChickens Robotics Club Inc.'
author = 'ThunderChickens Programming Department'

release = '1.0'
version = '1.0.0'

# -- General configuration

extensions = [
    'sphinx.ext.duration',
    'sphinx.ext.doctest',
    'sphinx.ext.autodoc',
    'sphinx.ext.autosummary',
    'sphinx.ext.intersphinx',
    "myst_parser",
    "sphinxcontrib.mermaid",
    "sphinx_copybutton",
]

intersphinx_mapping = {
    'python': ('https://docs.python.org/3/', None),
    'sphinx': ('https://www.sphinx-doc.org/en/master/', None),
}
intersphinx_disabled_domains = ['std']

templates_path = ['_templates']

# -- Options for HTML output

html_theme = 'sphinx_rtd_theme'

# -- Options for EPUB output
epub_show_urls = 'footnote'

# -- Options for MyST Markdown
# colon_fence: ::: admonitions; tasklist: - [ ] checklists; deflist: definition lists;
# attrs_inline: {kbd} and other inline attributes.
myst_enable_extensions = ["colon_fence", "tasklist", "deflist", "attrs_inline"]
# Generate anchors for headings (levels 1-4) so pages can link to a section: page.md#section-name
myst_heading_anchors = 4

# -- Options for sphinx-copybutton: don't copy prompts or output markers
copybutton_exclude = ".linenos, .gp, .go"

# -- Options for mermaid diagram support.
myst_fence_as_directive = ["mermaid"]
mermaid_params = ["-p", "puppeteer-config.json"]
