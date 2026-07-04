<img width="700" height="450" alt="无标题" src="https://github.com/user-attachments/assets/c678fe3e-1046-45ce-b4a6-124d42780820" />

# cmd command mod

A client-side mod that executes Windows system commands directly from the Minecraft chat bar.

<img width="1051" height="163" alt="b183a93f-42f9-4061-8a48-fe765f8ef650" src="https://github.com/user-attachments/assets/666a1cfd-b7f8-44f1-8877-dabec4b0bacd" />


## Features

- `/cmd <command>` — Execute a Windows command inside a persistent terminal
- Real-time streaming output – no need to wait for the command to finish
- Multi-terminal management (0–9), up to 10 terminals

## Commands

| Command | Description |
|------|------|
| `/cmd <command>` | Execute a Windows command (automatically creates a terminal on first use) |
| `/cmd internal start` | Open a new terminal |
| `/cmd internal list` | List all terminals |
| `/cmd internal <N>` | Switch to terminal N (0–9) |
| `/cmd internal help` | Show help |

## Color legend

| Color | Meaning |
|------|------|
| `#CCCCCC` Gray | Command echo |
| `#B1EAC2` Green | Execution success / live output |
| `#F9867D` Red | Execution failure / error message |
| `#FFD700` Gold | System message |

## Safety

- Commands run only on the local machine and do not affect the server
- Do not execute dangerous commands (deleting files, formatting disks, etc.)
