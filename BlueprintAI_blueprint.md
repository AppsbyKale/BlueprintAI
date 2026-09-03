# BlueprintAI Project Blueprint

## PROJECT_SIGNATURE
BlueprintAI is a local-first Android app for structured AI brainstorming, prompt engineering, and app-project management. It uses a strict MVVM architecture with Hilt for DI and Room for persistence (version 4), featuring a Grok-inspired pure dark grayscale UI. The core functionality enables deep AI collaboration with both local (LiteRT-LM) and remote models, including tool-calling capabilities, audio back-and-forth, visual learning features (Concept Maps, Message-level Concept Breakdown), and automated artifact generation (Reports, Blueprints, Concept Maps).

## SCREENS
- **Main Chat Screen**: Grok-like infinite scroll, markdown rendering, code blocks, top notice bar for attachments & status, bottom input bar pinned to software keyboard (`adjustResize`), and "Key Decision" tagging.
- **Sidebar**: Global/folder-scoped search, folder CRUD, folder merging, and long-press Folder Export.
- **Settings**: AI model selection, diagnostic logs, backup/restore controls, and TTS audio readback toggle.

## LOGIC_TREE
1. **Model Router**: Manages transitions between Auto, Desktop, and Local model modes.
2. **Tool Interception Loop**: Intercepts model-generated tool calls (e.g., web search) for local execution.
3. **Artifact Engine**: Analyzes conversation context to generate structured reports, living blueprint updates, and visual Concept Maps.
4. **Learning & Educational Engine**: Generates message-level concept breakdowns, relationship cause-and-effect mappings, and visual ASCII flowcharts on demand.
5. **Multimodal Pipeline**: Processes images and documents for local text extraction and search indexing.

## DATA_SCHEMA
- **Folder**: Logical container for conversations.
- **Message**: Individual chat entries with role, content, decision tags, and key decision flag.
- **Attachment**: Associated files with AI-suggested naming and extracted metadata.
- **BlueprintEntry**: Log entries for the living engineering record.

## BUILD & TOOLING
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **DI**: Hilt
- **DB**: Room (v4)
- **Networking**: Ktor / OkHttp
- **Local Inference**: LiteRT-LM (`com.google.ai.edge.litertlm`)
- **Version Control**: Git

## TIMELINE & DECISIONS
- **2026-08-25, 03:15**: Initial master prompt processed. `tasks.md` and `blueprint.md` initialized. Phase 1 started.
- **2026-08-25, 03:25**: Phase 1 Foundation implemented: Hilt setup, Room entities/DB, Grayscale theme, and UI skeleton (Sidebar + Chat Shell + Settings UI). Added signing configuration for `debug.keystore` portability.
- **2026-08-25, 10:45**: Phase 2 Model Integration implemented: `ModelManager` for routing, `LiteRtModelClient` (Local) and `RemoteModelClient` (Ktor/OpenAI), `ChatRepository`, `ChatViewModel` with streaming support, `Settings` storage in Room, and Grok-like `ChatScreen` UI.
- **2026-08-25, 10:55**: Phase 3 Folders, Persistence, Search implemented: Enhanced Room entities with FTS4 support, `Attachment` entity, `FolderRepository`, expanded `ChatRepository`, real sidebar folder management with CRUD, and message "Key Decision" starring and tagging.
- **2026-08-25, 11:05**: Phase 4 Artifacts & Export implemented: `ArtifactRepository` for AI-driven report and blueprint generation, `ArtifactViewModel` for state management, `ExportDialog` for multi-select artifact sharing, and UI integration in Sidebar.
- **2026-08-26, 09:45**: Phase 5 Multimodal + Tools + Logs implemented: `AttachmentManager` with PDFBox and ML Kit, AI-driven attachment renaming in `ChatViewModel`, `ToolInterceptor` with HTML stripping for web browsing, `LogManager` for diagnostic tracking, `BackupManager` for ZIP exports, and UI integration for Logs/Backup in `MainActivity`.
- **2026-08-26, 10:00**: Phase 6 Polish & Future Hooks implemented: Advanced folder management with Folder Merging, error resilience with styled error bubbles, and UX improvements like Log clearing and library dependency fixes.
- **2026-08-26, 10:15**: Phase 7 Audio Support implemented: Conversational back-and-forth enabled with Speech-to-Text (STT) for hands-free input and Text-to-Speech (TTS) for AI read-back. Added settings toggle and UI microphone controls.
- **2026-08-26, 11:10**: Phase 8 Database Schema & UI Keyboard Polish: Incremented Room DB version to 4 with destructive migration fallback. Moved input bar to bottom with `adjustResize` and IME window insets. Added top notice bar for attachments and status indicators.
- **2026-08-26, 11:30**: Phase 9 Visual Learning & UX Consolidation: Added "Concept Map & Architecture Guide" artifact generator (ASCII diagrams, layer breakdowns, plain-English glossary). Added message long-press "Explain Concepts & Relationships 💡" dialog. Cleaned up TopAppBar by consolidating export actions into Sidebar long-press menu. Initialized Git repository and IDE VCS mapping.

## CURRENT_CONTEXT
Phase 9 complete. BlueprintAI now features comprehensive visual learning tools for non-professional "vibe coders", including visual Concept Maps in folder exports and message-level concept/relationship breakdowns, backed by clean UI layout and version control.

## FLOWCHART OF SYSTEMS
User → Sidebar (Folders + Search + Long-Press Export) → Chat Screen (Messages + Attachments + Stars + Long-Press Concept Explainer)
Chat Screen ↔ Model Router (Auto/Desktop/Local LiteRT-LM)
Model Router → Tool Interceptor → (search_web / read_webpage) → Local Execution
Sidebar Export ➔ Artifact Engine ➔ Report / Blueprint / Concept Map / Prompt / Conversation
