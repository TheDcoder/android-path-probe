# AGENTS.md

Instructions for coding agents working on this repository.

## Code style

- Always use TAB (`\t`) for indentation instead of spaces throughout the project.

## Documentation

- Whenever necessary, update the "AI authorship" section of `README.md` so it accurately reflects whether the project is fully AI/LLM-authored and which agent/model was used.

## Git commits

- Add a metadata trailer to each commit message that identifies the LLM agent and model currently in use.
- Follow the Linux kernel trailer style: put `Tag: value` lines in the trailer block at the end of the commit message, separated from the body by a blank line, one tag per line, with no line wrapping.
- Use `Assisted-by: <agent>:<model>`, for example `Assisted-by: OpenCode Build Agent:deepseek-v4.1-flash-free`.
- Do not create a Git tag for this; it is commit message metadata.
- Agents must not add `Signed-off-by:`; only a human can certify the Developer's Certificate of Origin (DCO).
