# AGENTS.md

Instructions for coding agents working on this repository.

## Code style

- Always use TAB (`\t`) for indentation instead of spaces throughout the project.

## Documentation

- Whenever necessary, update the "AI authorship" section of `README.md` so it accurately reflects whether the project is fully AI/LLM-authored and which agent/model was used.

## Git commits

- Before creating or amending commits, configure a repository-local Git identity for the current agent; do not rely on or modify the user/system/global Git identity.
- Set the local identity with `git config --local user.name "<agent>"` and `git config --local user.email "<agent-owned address>"`. If no verified agent-owned address exists, use a non-personal address such as `<agent>@localhost` rather than inventing a public identity.
- Ensure both the author and committer use the local agent identity. Clear or override any `GIT_AUTHOR_*` or `GIT_COMMITTER_*` environment variables that would supersede it.
- Disable commit signing for agent-created commits by using `git -c commit.gpgsign=false commit ...`; do not change the user's or repository's persistent signing configuration.
- Before creating or amending a commit, confirm the current LLM model. If it is unknown, ask the user for the model name before proceeding; do not guess or use a placeholder.
- Add a metadata trailer to each commit message that identifies the LLM agent and model currently in use.
- Follow the Linux kernel trailer style: put `Tag: value` lines in the trailer block at the end of the commit message, separated from the body by a blank line, one tag per line, with no line wrapping.
- Use `Assisted-by: <agent>:<model>`, for example `Assisted-by: OpenCode Build Agent:deepseek-v4.1-flash-free`.
- Do not create a Git tag for this; it is commit message metadata.
- Agents must not add `Signed-off-by:`; only a human can certify the Developer's Certificate of Origin (DCO).
