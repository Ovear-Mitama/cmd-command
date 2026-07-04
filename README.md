<img width="700" height="437" alt="download" src="https://github.com/user-attachments/assets/1e333269-b315-4397-89af-c1fe76647f24" />

# cmd command mod

A client-side mod that executes Windows system commands directly from the Minecraft chat bar.

<img width="1051" height="163" alt="1332255e67a53f7a172af13ddf0444c55681ba6e" src="https://github.com/user-attachments/assets/dfd91001-13dc-4576-b88d-1bae53d232fc" />

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
