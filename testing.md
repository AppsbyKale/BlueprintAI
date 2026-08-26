# BlueprintAI — Testing Checklist

Use this guide to verify the current state of the application. Please note any failures or erratic behavior to report back.

## 1. Chat & Input Experience
- [ ] **Keyboard Handling**: Tap the input bar. Does it slide up smoothly and sit exactly above the keyboard?
- [ ] **Keyboard Dismissal**: Hide the keyboard. Does the input bar return to the bottom (above the nav bar)?
- [ ] **Message Sending**: Type a message and send. Does it appear in the list?
- [ ] **Streaming Responses**: Does the AI response stream in real-time?
- [ ] **Auto-Scroll**: 
    - [ ] When at the bottom, does it scroll automatically as the AI types?
    - [ ] If you scroll UP while the AI is typing, does it stay where you are (letting you read) instead of snapping back to the bottom?

## 2. Attachment System
- [ ] **Pick File**: Tap the `+` button and pick a file. 
- [ ] **Top Notice Bar**: Does the attachment appear in the horizontal bar at the top of the chat?
- [ ] **AI Processing**: Does a progress bar appear at the top while the file is being "analyzed"?
- [ ] **Suggested Name**: Does the attachment show a descriptive name (e.g., "Meeting_Notes") instead of just "unknown"?

## 3. Message Actions
- [ ] **Key Decisions**: Tap the star icon on a message. Does it turn yellow and show the "Key Decision" label?
- [ ] **Tagging**: Long-press a message. Does the "Update Tags" dialog appear? 
- [ ] **Tag Display**: Add tags (e.g., `feature, todo`). Do they appear as chips under the message?

## 4. Voice Features
- [ ] **Speech-to-Text**: Tap the Mic button. Does the "Listening..." bar appear at the top? Does your speech turn into text in the input box?
- [ ] **Text-to-Speech**: Go to the 3-dot menu -> Toggle "Audio Readback". Send a message. Does the app speak the AI's response?

## 5. Folder & Data Management
- [ ] **Sidebar Navigation**: Open the drawer. Can you create a new folder?
- [ ] **Switching Folders**: Switch between folders. Do the messages update correctly?
- [ ] **Search**: Use the search bar in the sidebar. Does it filter folders correctly?
- [ ] **Merging**: (If implemented in UI) Can you drag/merge folders or use the menu to combine them?

## 6. Artifacts & Export
- [ ] **Artifact Generation**: Tap the "Build" (hammer) icon in the top bar.
- [ ] **Export Dialog**: Does the dialog show options for Report, Blueprint, Prompt, and Conversation?
- [ ] **Sharing**: Select an option and tap Export. Does the Android "Share" sheet appear?

## 7. System & Settings
- [ ] **Theme/Mode**: Toggle between Auto/Desktop/Phone modes in the top bar. Does the UI adapt?
- [ ] **Logs**: 3-dot menu -> "View Logs". Can you see the system logs?
- [ ] **Backup**: 3-dot menu -> "Backup & Restore". Can you create a backup file?
- [ ] **AI Config**: 3-dot menu -> "AI Models". Can you update the Gemini API key or local paths?

---
**Report findings here or in the chat.**
