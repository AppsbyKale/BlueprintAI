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
- [x] Add long-press "Explain Concepts & Relationships" dialog on AI messages
- [x] Consolidate export UX by removing redundant hammer icon from TopAppBar
- [x] Initialize Git repository and add `.idea/vcs.xml` for IDE integration

## Phase 10 – Model Routing, Context Memory, Threading & Debugging
- [x] Fix empty folder startup issue (auto-create "General" folder on launch so messages are never dropped)
- [x] Add Gemini API fallback client (`GeminiModelClient`) with automatic routing in Auto mode
- [x] Update LiteRT-LM client to use native C++ `com.google.ai.edge.litertlm.Engine` with automatic GPU -> CPU fallback
- [x] Offload model initialization and token generation to `Dispatchers.Default` (fixes UI freeze & keyboard dismissal)
- [x] Add direct Hugging Face model downloader for `gemma-4-E2B-it.litertlm` with live progress tracking
- [x] Fix Ktor `Content-Type: application/json` missing header on remote model POST requests
- [x] Fix atomic settings save race condition preventing remote desktop IP from persisting
- [x] Enable cleartext HTTP traffic (`usesCleartextTraffic="true"`) and 120s network timeouts for remote port-forwarded LLM servers (LM Studio / Ollama / LM Link)
- [x] Add multi-turn conversation memory with 20-message verbatim sliding window and cached App Brainstorming & Starred Highlights summary for older history
- [x] Add visual Context Window Usage Bar at the top of the chat screen
- [x] Add long-press "Debug: Inspect Context & Tokens" dialog showing exact model payload and token counts
- [x] Enable native text selection, highlighting, and Cut/Copy/Paste toolbar across all prompts, responses, and dialogs

## Phase 11 – Saved Remote Server Profiles, Manual IP Selection & Clean Dialogs
- [x] Multi-profile Remote Model Server configuration (Room v7 entity with Label, Local IP, Public IP, Target Model ID, API Key)
- [x] Streamlined top bar AI model selector (`Auto` | `Remote` | `Local`)
- [x] Manual IP Mode switcher (`Local IP` vs `Public IP`) in 3-dot menu and Settings FilterChips
- [x] Target Model Auto-Discovery (`GET /v1/models`) with 1-tap model list fetch in profile editor
- [x] Qwen 2.5 & DeepSeek R1 reasoning/thinking content streaming support (`reasoning_content`)
- [x] Interactive Profile Cards with direct Long-Press gesture and dedicated Edit/Delete action buttons
- [x] Clean professional dialog formatting (stripped raw markdown asterisks, removed emojis, clean bullet points)
- [x] Room database schema stability (`fallbackToDestructiveMigration` safeguards for smooth updates)
