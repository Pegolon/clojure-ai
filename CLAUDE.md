# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Clojure library + CLI that generates a podcast RSS 2.0 feed (with the iTunes
and Atom extensions) from plain Clojure/EDN data. Built with Clojure 1.12.5,
Leiningen, and malli for schema validation.

## Commands

```bash
lein test                                   # run all tests
lein test :only podcast-rss.feed-test       # run one namespace
lein test :only podcast-rss.feed-test/generates-well-formed-feed  # one deftest
lein run resources/sample_podcast.edn feed.xml   # EDN in -> RSS file out
lein run resources/sample_podcast.edn            # ...or print to stdout
lein repl
lein uberjar
```

## Architecture

The pipeline is a one-way data transformation, one namespace per stage:

- `podcast-rss.core` — CLI entry (`-main`): reads an EDN file into a map and
  writes/prints the feed.
- `podcast-rss.schema` — malli schemas `Podcast` and `Episode`, plus thin
  wrappers (`valid?`/`explain`/`humanize`/`validate!`) over `malli.core` and
  `malli.error`.
- `podcast-rss.feed` — `generate` is the public API: it calls
  `schema/validate!` first (so **all input validation happens here**, before any
  XML is built), then converts the podcast map into a Hiccup-style element tree.
- `podcast-rss.xml` — a small, dependency-free XML emitter. Elements are
  `[:tag {:attrs} & children]` vectors; nil children are dropped (this is why
  `feed.clj` can conditionally include elements by returning nil).

Data flow: `EDN map -> schema/validate! -> podcast->rss (Hiccup vectors) -> xml/emit (string)`.

Key conventions:
- The domain schemas in `schema.clj` are the single source of truth for what a
  podcast/episode map may contain; optional keys are omitted from output when
  absent rather than emitted empty.
- The XML emitter is intentionally hand-rolled (no `data.xml`) to keep exact
  control over iTunes/Atom namespace prefixes and output formatting.

## Spec-driven workflow (OpenSpec)

This repo uses [OpenSpec](https://github.com/Fission-AI/OpenSpec) for
spec-driven development. Non-trivial features and behavior changes should go
through a change proposal before code is written, so intent lives in versioned
Markdown under `openspec/` rather than in chat history.

Use the `/opsx:*` slash commands (backed by the skills in
`.claude/skills/openspec-*`):

- `/opsx:explore` — think through an idea or investigate a problem before
  committing to a change.
- `/opsx:propose "<what to build>"` — create a change and generate all its
  artifacts (proposal, design, delta specs, tasks) in one step.
- `/opsx:apply` — implement the tasks from a change against its specs.
- `/opsx:update` — revise a change's planning artifacts and keep them coherent
  (does not touch code).
- `/opsx:sync` — fold a change's delta specs into the main specs.
- `/opsx:archive` — finalize a completed change and roll it into the main specs.

The typical loop is **propose → apply → archive**. Inspect state with the CLI:

```bash
openspec list            # in-flight changes
openspec list --specs    # current specs
openspec view            # interactive dashboard
openspec validate        # validate changes and specs
```

The `openspec/` directory (config, specs, and changes) is the source of truth
for intended behavior; keep it in sync when you change what the library does.

## Environment note (Claude Code on the web / sandbox)

Dependency resolution requires Clojars over the sandbox's mandatory HTTPS
CONNECT proxy. Leiningen reads the lowercase `http_proxy` env var, but this
environment only exports `https_proxy`/`HTTPS_PROXY`, so without a fix lein
fails with `403 Forbidden` from Clojars. Fix before running lein:

```bash
export http_proxy="$https_proxy"
export http_no_proxy="127.0.0.1|localhost"
```

Also note: `lein` cannot self-install its standalone jar here (it's a GitHub
release asset, which the proxy restricts to session-attached repos). If `lein`
is missing, it must be run from a Maven-resolved classpath rather than via the
normal self-install.
