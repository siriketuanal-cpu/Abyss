# Abyss Timer

A game stamina and idle reward timer tracking web application, rewritten from Android Kotlin/Jetpack Compose to React + TypeScript + Tailwind CSS with Vite.

## Features

- **Stamina Timers**: Tracks stamina regeneration with customized intervals, showing exact full-recovery times and real-time current/max values. Highlight warning states when near full (< 2 hours).
- **Abyss Stamina Timers**: Rank-based max stamina calculations (`240 + (Rank - 1) * 5`) with quick 40-stamina consumption toggle/confirm workflow.
- **Idle Timers**: Flexible idle reward timers with countdown displays and two-step claim confirmation.
- **Account Grouping**: Group multiple timers together under custom account headers for multi-account management.
- **Visual Dividers & Headers**: Organize timers by game titles or categories with custom header colors and divider rules.
- **Storage Synchronization**: Persistent state management utilizing local storage.
- **Responsive Touch & Mouse Controls**: Seamless support for quick taps, numeric edits, and long-press contextual menus.

## Development

```bash
npm install
npm run dev
```

Build for production:

```bash
npm run build
```
