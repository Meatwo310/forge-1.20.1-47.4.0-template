# Contributing

If you are working in `Meatwo310/custom-mdk` itself, follow
[MDK Template Changes](#mdk-template-changes). Otherwise, follow
[Commit Message Convention](#commit-message-convention).

## Formatting

Ensure every added or modified file follows the repository-root
[`.editorconfig`](../.editorconfig). Enable EditorConfig support in your editor
and resolve any formatting violations before submitting a change. The
EditorConfig workflow checks all tracked files independently from the build.

Run the same check locally before submitting a change. If Nix is installed,
you can use the development shell provided by this repository:

```sh
nix develop -c editorconfig-checker -format github-actions
```

Generated resources under `src/generated/` are excluded because their contents
are produced by DataGen rather than edited directly.

If you installed `editorconfig-checker` by another method, run the same command
directly without `nix develop`.

## Documentation Languages

`README.md` and `README.ja.md` are the English and Japanese versions of the
same user-facing guide. Any change to `README.md` must include the corresponding
change to `README.ja.md` in the same pull request.

Agent-specific documentation remains English-only. Do not create translated
copies of `AGENTS.md` or files under `mdk/`.

## Commit Message Convention

Commits should follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description
```

Pull request titles should use the same format as commit messages. When a pull
request is squash-merged, its title becomes the resulting commit message, so
the title must be a valid commit message on its own.

Use the smallest scope that describes the affected area.

### Scope Rules

Subprojects are named for shared code, Minecraft versions, and loader targets, such as `common`, `1.20.1-common`, `1.20.1-fabric`, `1.20.1-forge`, or `26.1.2-neo`. Versioned subprojects live under directories such as `1.20.1/common`, `1.20.1/fabric`, `1.20.1/forge`, or `26.1.2/neo`.

| Changes affect...                            | Scope                                                                    |
|----------------------------------------------|--------------------------------------------------------------------------|
| A single subproject                          | Use that project name, such as `fix(26.1.2-fabric): ...`                 |
| Multiple but not all subprojects             | Include the relevant names, such as `feat(1.20.1-forge,1.21.1-neo): ...` |
| All loaders for a specific Minecraft version | Use only the version number, such as `fix(1.20.1): ...`                  |
| All Minecraft versions for a specific loader | Use only the loader name, such as `feat(fabric): ...`                    |
| All subprojects equally                      | Scope is optional                                                        |
| Root-level repository metadata only          | Scope is optional                                                        |

### Recommended Types

`feat` / `fix` / `docs` / `style` / `refactor` / `perf` / `test` / `build` / `ci` / `chore` / `revert` / `release`

Generated release notes include breaking changes (`!`), `feat`, `fix`, and `perf`
commits. Other types are still useful for repository history, but are omitted
from release notes.

Because these entries can appear in changelogs as written, prefer English for
commit messages that may be included in release notes.

### Examples

```
feat(26.1.2-fabric): add config screen support
fix(1.20.1-forge): resolve startup crash
build: update runtime mod staging
ci: add runtime test artifact upload
docs: refresh supported platform matrix
chore(1.18.2-forge,1.19.2-forge): bump forge versions
fix(1.20.1): resolve issue across all 1.20.1 loaders
feat(fabric): add Mod Menu integration across Fabric targets
```

## MDK Template Changes

When committing changes to the [MDK template itself](https://github.com/Meatwo310/custom-mdk),
you **MUST** use `mdk` as the type so that downstream mod release notes can exclude them.

For changes that would otherwise use `feat` or `fix`, use the scope rules above
to identify the affected subprojects, Minecraft version, or loader. For other
changes, use the scope to preserve the usual type, such as `build`, `ci`, or
`docs`.

```
mdk(26.1.2-fabric): add config screen support
mdk(1.20.1-forge): resolve startup crash
mdk(fabric): update Mod Menu integration across Fabric targets
mdk(ci): add release workflow
mdk(build): move the mod version to version.txt
```
