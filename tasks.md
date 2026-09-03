# Tasks Checklist

## Phase 1 – Foundation
- [x] Project skeleton setup (Hilt, Room, Compose)
- [x] Pure dark grayscale theme implementation
- [x] Room entities for Folder + Message
- [x] Sidebar navigation shell
- [x] Empty Chat screen shell
- [x] Settings dropdown shell (Auto/Desktop/Phone toggle UI)
- [x] Signing configuration for debug/shared keystore

## Phase 2 – Model Integration
- [x] Local LiteRT-LM loading (.litertlm path picker)
- [x] Desktop OpenAI-compatible client integration
- [x] Auto mode health-check / fallback logic
- [x] Streaming chat send/receive implementation
- [x] Gemma 3 alternate/organizer support (Generic LiteRT support)

## Phase 3 – Folders, Persistence, Search
- [x] Folder CRUD operations
- [x] Message persistence with Room
- [x] Global and per-folder search (Room FTS)
- [x] New folder dialog with attachments
- [x] Key-decision star / highlight+tag functionality

## Phase 4 – Artifacts & Export
- [x] Structured Report generation logic
- [x] Blueprint append logic
- [x] Export/Share dialog with checkboxes
- [x] Prompt export functionality

## Phase 5 – Multimodal + Tools + Logs
- [x] Image/Document import and text extraction (ML Kit + PDFBox)
- [x] AI rename suggestions for attachments
- [x] Tool interception (search_web / read_webpage)
- [x] Diagnostics and LoRA-style logs dialog
- [x] Backup/Restore (ZIP)

## Phase 6 – Polish & Future Hooks
- [x] Advanced folder management (Merge folders)
- [x] Error resilience and UX polish
- [x] Final audit of blueprint and tasks

## Phase 7 – Audio Support (Conversational)
- [x] Record Audio & Internet permissions
- [x] VoiceManager for STT (SpeechRecognizer) and TTS (TextToSpeech)
- [x] Integration with ChatViewModel (automatic send/speak responses)
- [x] UI Controls (Mic button in input bar, TTS toggle in Settings)

## Phase 8 – Database Recovery & Keyboard Layout Fixes
- [x] Fix Room schema version mismatch (incremented to version 4 with fallbackToDestructiveMigration)
- [x] Position chatbox at bottom of screen with `adjustResize` and IME window insets
- [x] Add top notice bar for attachments, AI generation, and voice listening status
- [x] Fix chat scroll behavior to allow scrolling up during live AI response streaming

## Phase 9 – Visual Learning Features & UX Consolidation
- [x] Add "Concept Map & Architecture Guide" artifact option to folder export (ASCII flowcharts, layer breakdowns, plain-English glossary)
- [x] Add long-press "Explain Concepts & Relationships 💡" dialog on AI messages
- [x] Consolidate export UX by removing redundant hammer icon from TopAppBar
- [x] Initialize Git repository and add `.idea/vcs.xml` for IDE integration
