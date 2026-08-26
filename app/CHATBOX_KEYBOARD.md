# ChatAI — Chatbox & Keyboard Handling

> How the input bar stays pinned just above the keyboard with
> texting-app behavior.

---

## The Layout Structure

The key is in `ChatScreen.kt` — the `Scaffold` content `Column`:

```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = { TopAppBar(...) },
) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Only take TOP padding from Scaffold (for TopAppBar).
            // Bottom padding is handled manually below to avoid double-counting.
            .padding(
                top = padding.calculateTopPadding(),
                start = padding.calculateLeftPadding(LocalLayoutDirection.current),
                end = padding.calculateRightPadding(LocalLayoutDirection.current),
            )
            // These two are the magic. They chain together:
            .navigationBarsPadding()   // accounts for system nav bar
            .imePadding()              // accounts for keyboard height
    ) {
        // Health strip / status area
        HealthStrip(...)

        // Message list — takes all remaining space
        MessageList(
            ...,
            modifier = Modifier.weight(1f)  // fills everything above InputBar
        )

        // Input bar — sits at the bottom, pushed up by imePadding()
        InputBar(...)
    }
}
```

**Why this works:**

1. `imePadding()` adds bottom padding equal to the keyboard height when the
   keyboard is visible, and 0 when it's hidden.
2. `navigationBarsPadding()` adds bottom padding for the system nav bar.
3. Chaining them: `.navigationBarsPadding().imePadding()` — they "consume"
   each other. When the keyboard is up, the IME padding dominates. When the
   keyboard is down, the nav bar padding dominates. You never get double.
4. The `Column` layout means the `InputBar` (last child) naturally sits at
   the bottom. The `MessageList` with `Modifier.weight(1f)` fills everything
   above it.
5. When the keyboard appears, `imePadding()` pushes the entire Column up,
   including the InputBar. The MessageList shrinks because its weight-based
   space is reduced. Texting-app behavior.

**Critical: Do NOT take Scaffold's `padding.calculateBottomPadding()`.**
The Scaffold bottom padding accounts for the bottom bar / nav bar. If you
also apply `navigationBarsPadding()`, you double-count and the input floats
above the keyboard. This was a bug we fixed (round 4).

---

## The InputBar Composable

```kotlin
private fun InputBar(
    enabled: Boolean,
    showHint: Boolean,
    placeholder: String,
    attachment: PendingAttachment?,
    readingFile: Boolean,
    generating: Boolean = false,
    onAttachClick: () -> Unit,
    onClearAttachment: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit = {},
) {
    var text by remember { mutableStateOf("") }

    val submit = {
        val trimmed = text.trim()
        if (enabled && !generating && (trimmed.isNotEmpty() || attachment?.extractedText != null)) {
            onSend(trimmed.ifEmpty { "Please use the attached file." })
            text = ""
        }
    }

    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Reading progress bar (shown while processing attachment)
            if (readingFile) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Attachment chip (shown when a file is staged)
            attachment?.let { att ->
                AttachmentChip(attachment = att, onRemove = onClearAttachment)
            }

            // Hint text (shown when chat is empty)
            if (showHint && enabled) {
                Text(
                    text = "The Brain reads every message and files it into folders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp)
                )
            }

            // Main input row: attach button + text field + send/stop button
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Attach button (paperclip)
                IconButton(onClick = onAttachClick, enabled = enabled && !readingFile) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text field — expands to fill available width
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    enabled = enabled,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(Modifier.size(6.dp))

                // Send / Stop / Loading button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (generating) {
                        // Stop button while pipeline runs
                        IconButton(onClick = onStop) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Stop generating",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (!enabled) {
                        // Loading spinner while engine initializes
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Send button
                        IconButton(
                            onClick = submit,
                            enabled = text.isNotBlank() || attachment?.extractedText != null
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (text.isNotBlank() || attachment?.extractedText != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## AndroidManifest.xml — Required for Keyboard Behavior

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:windowSoftInputMode="adjustResize">
```

`adjustResize` is critical. It tells Android to resize the activity's window
when the keyboard appears, which triggers `imePadding()` to recalculate.

**Other modes and why they don't work:**
- `adjustPan` — pans the whole window up (input can go off-screen)
- `adjustNothing` — keyboard overlaps content (input hidden behind keyboard)
- `stateHidden` / `stateUnchanged` — don't affect resize behavior, but
  `adjustResize` is the one that makes `imePadding()` work

---

## Common Mistakes

1. **Using Scaffold's `padding.calculateBottomPadding()`** — This includes
   the nav bar height. If you also use `navigationBarsPadding()`, you
   double-count and the input floats. Solution: only use top padding from
   Scaffold, handle bottom manually with `navigationBarsPadding().imePadding()`.

2. **Missing `adjustResize`** — Without this, `imePadding()` doesn't fire
   on keyboard show/hide. The input stays behind the keyboard.

3. **Using `windowSoftInputMode="adjustPan"`** — This pans the window
   instead of resizing. `imePadding()` doesn't work with pan mode.

4. **Wrapping InputBar in a `Box` instead of `Column`** — The Column ensures
   the InputBar stays at the bottom. A Box would stack everything on top
   of each other.

5. **Not using `Modifier.weight(1f)` on the message list** — Without weight,
   the message list doesn't shrink when the keyboard appears, and the
   InputBar gets pushed off-screen.

---

## How It Behaves

```
Keyboard hidden:                    Keyboard shown:
┌──────────────────┐                ┌──────────────────┐
│ TopAppBar        │                │ TopAppBar        │
├──────────────────┤                ├──────────────────┤
│                  │                │                  │
│  Message list    │  ← weight(1f) │  Message list    │ ← shrinks
│  (fills space)   │                │  (fills space)   │
│                  │                │                  │
├──────────────────┤                ├──────────────────┤
│ [📎] [Type...] [➤]│ ← at bottom  │                  │ ← imePadding
│  Nav bar padding  │                ├──────────────────┤
└──────────────────┘                │ [📎] [Type...] [➤]│ ← pushed up
                                    │  Nav bar hidden   │
                                    └──────────────────┘
```

The `imePadding()` modifier adds bottom padding equal to the keyboard height.
This pushes the entire Column up. The `weight(1f)` on the message list makes
it shrink to fill the remaining space. The InputBar stays at the bottom of the
Column, which is now above the keyboard.
