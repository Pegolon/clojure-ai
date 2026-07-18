## Summary

<!-- What does this PR change and why? -->

## OpenSpec

This repo uses a spec-driven (OpenSpec) workflow: **propose → apply → archive**.
Before this PR can be merged, every change it introduces must be archived — i.e.
folded into the main specs under `openspec/specs/` with its change directory moved
into `openspec/changes/archive/`. No active change directories may remain under
`openspec/changes/` (only `archive/` is allowed).

- [ ] All OpenSpec changes for this work have been archived (`/opsx:archive`), and
      `openspec/changes/` contains no in-flight change directories.
- [ ] `openspec list` reports no active changes.
- [ ] `openspec validate --strict` passes.

> The **OpenSpec Archived** status check enforces the first item automatically and
> must be green before merging. If your work did not require an OpenSpec change,
> the check still passes (there is simply nothing to archive).

## Checklist

- [ ] `lein test` passes locally.
- [ ] Documentation updated where relevant.
