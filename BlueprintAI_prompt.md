# BlueprintAI_Master_Prompt.md
**Version:** 1.0  
**App Name:** BlueprintAI  
**Package:** com.example.blueprintai  
**Min SDK:** 35  
**Architecture Mandate:** Kotlin + Jetpack Compose + Hilt + MVVM (strict)  
**Theme:** Pure dark grayscale only (no accent colors, no light mode)  
**UI Inspiration:** As close as possible to the Grok app chat experience (message bubbles, input bar, attachment handling, markdown/code rendering, streaming feel) without being legally actionable.

## PROJECT_SIGNATURE
BlueprintAI is a local-first Android app for structured AI brainstorming, prompt engineering, and app-project management. Users create folders (projects), hold long back-and-forth conversations with local or remote Gemma models, attach documents/images, mark key decisions, and at any time generate or update:
- Structured Report
- [AppName]_Blueprint.md (living engineering log following the exact template below)
- tasks.md (phased checklist)
- Ready-to-paste prompts

The core loop is: stream-of-consciousness conversation → AI helps organize → user exports polished artifacts. All state is persisted so the user can switch folders and resume exactly where they left off.

## NON-NEGOTIABLE RULES FOR THE IMPLEMENTING AI
1. Maintain a living `blueprint.md` at the root of the generated project following the TEMPLATE at the end of this file. Append-only. Never delete or rewrite history.
2. Maintain a separate `tasks.md` that lists every phase and sub-task with checkboxes. Update it at the end of every work session.
3. Work in the exact phases listed below. Do not jump ahead. At the end of each phase, update both files and summarize what was completed.
4. Use Hilt for DI, strict MVVM (ViewModels + UseCases/Repositories), Room for all structured data, and Jetpack Compose exclusively.
5. Pure dark grayscale theme only.
6. Tools / function calling must always be enabled. The app must intercept model tool calls for `search_web` and `read_webpage` (and any others the model emits), execute them via HTTP or local proxy, and feed results back into the conversation.
7. Blueprints are append-only and must always reflect the current real structure of the app being discussed.
8. When generating a Report, the AI must read the entire conversation context (plus any tagged/starred messages) and organize everything that was decided, even if the user never explicitly marked it.

## SCREENS & UI REQUIREMENTS
- **Main Chat Screen** (Grok-like): infinite-scroll message list, streaming responses, markdown + code blocks with copy, attachment chips, star/highlight+tag on any message or text selection for “Key Decision”. Starred/tagged items are always injected into Reports/Blueprints (but the AI must also use full context).
- **Pull-out Sidebar** (phone only):
    - Search bar at top (global search across all folders, conversations, AI-extracted text from documents & photos).
    - When a folder is selected, search scopes to that folder.
    - List of folders. Long-press → Edit name / Merge / Delete / Export-Share.
- **New Folder Dialog**: text field for name + attachment icon (images, PDF, TXT, MD, code files, ZIP of projects).
- **Settings Icon** → Dropdown menu:
    - Top: Auto / Desktop / Phone toggle (Auto = prefer Desktop if reachable, else local).
    - 1. AI Models → dialog
    - 2. Logs → dialog
    - 3. Backup/Delete → dialog
- **AI Models Dialog**:
    - Scan + selectable list of local models (primary: .litertlm Gemma 4 E2B; alternate: .model Gemma 3). Default path suggestion: /Internal storage/Download/AI_Models/. Manual path picker.
    - Desktop: IP:Port field (app must append /v1 automatically), optional API key, Test Connection button, current active indicator.
    - Option to use Gemma 3 as a lightweight “organizer” model and Gemma 4 as main chat model.
- **Logs Dialog**: Diagnostic logs (model connection errors, prompt/response errors, folder/attachment errors) + LoRA-style training data export (prompt, response, model used, latency, tokens, any user ratings/tags). Exportable.
- **Backup/Delete Dialog**: Full system backup/restore as ZIP, selective delete, clear all.
- **Export/Share** (from folder long-press): Checkbox multi-select → Report / Blueprint / tasks.md / Prompt / Whole conversation / Document list. Then share or save as .txt / .md / ZIP.

## LOGIC_TREE (CORE MECHANISMS)
1. Model Router: Auto/Desktop/Phone. Desktop = OpenAI-compatible client to user-supplied IP:port/v1. Local = LiteRT-LM for .litertlm (Gemma 4 primary) + experimental support for .model (Gemma 3).
2. Tool Interception Loop: Always enable function calling. When model emits tool call JSON for search_web or read_webpage (or others), app executes via HTTP client / local proxy / SearXNG-style service, returns result to model, continues generation.
3. Multimodal Pipeline: Camera + Gallery images + document picker. After understanding, AI suggests clean filename; user confirms. Extracted text/descriptions indexed for search. AI renames photos in-app.
4. Conversation Persistence: Room + files. Full-text searchable. Infinite scroll. Key-decision stars/tags.
5. Artifact Generation: On demand (or at finalization) AI produces organized Report (using conversation context + tags), appends to Blueprint, updates tasks.md.
6. Folder Lifecycle: Create (name + optional attachments) → chat → long-press actions → export.
7. Search: Global + per-folder, covers folder names, messages, extracted document/photo text.

## DATA_SCHEMA (KEY ENTITIES)
- Folder: id, name, createdAt, updatedAt
- Conversation / Message: id, folderId, role, content, timestamp, isKeyDecision, tags, attachments
- Attachment: id, folderId/messageId, originalName, aiSuggestedName, type, extractedText, uri
- BlueprintEntry: append-only log entries
- Task: phase, description, completed, relatedFolder
- DiagnosticLog / LoRARecord: timestamp, type, details, model, latency, etc.
- Settings: activeModelMode, localModelPath, desktopBaseUrl, apiKey, etc.

## BUILD & TOOLING
- Language: Kotlin
- UI: Jetpack Compose only
- DI: Hilt
- Architecture: MVVM + UseCases + Repositories
- DB: Room (with FTS for search)
- Local Inference: LiteRT-LM (primary for Gemma 4 E2B .litertlm)
- Remote: OpenAI-compatible HTTP client (Ktor or OkHttp)
- Other: Coil (images), Markdown renderer, SAF / document picker, WorkManager if needed for background
- No ads. Google Drive support is future. Analytics local-only for now (future item).

## PHASED IMPLEMENTATION PLAN (tasks.md must track these)
**Phase 1 – Foundation**
- Project skeleton, Hilt, MVVM, pure dark grayscale theme, basic navigation (sidebar + empty chat)
- Room entities for Folder + Message
- Settings dropdown shell + Auto/Desktop/Phone toggle UI
- tasks.md and blueprint.md created and maintained

**Phase 2 – Model Integration**
- Local LiteRT-LM loading (selectable .litertlm path, Gemma 4 primary)
- Desktop OpenAI-compatible client (IP:port → /v1)
- Auto mode health-check / fallback
- Basic chat send/receive with streaming
- Gemma 3 marked as alternate / organizer

**Phase 3 – Folders, Persistence, Search**
- Full folder CRUD + long-press menu (edit/merge/delete)
- Conversation persistence + infinite scroll
- Global + per-folder search (Room FTS)
- New folder dialog with attachments
- Key-decision star / highlight+tag

**Phase 4 – Artifacts & Export**
- Structured Report generation (AI reads full context + tags; sections: Overview/Vision, Problem & Users, Key Features, Tech Stack & Architecture, Screens & Flows, Data Model, Risks/Open Questions, Next Steps)
- Blueprint append logic following exact template
- tasks.md management
- Export/Share dialog with checkboxes
- Prompt export (copyable [AppName]_prompt.md)

**Phase 5 – Multimodal + Tools + Logs**
- Image (camera/gallery) + document import (PDF/TXT/MD/code/ZIP)
- AI rename suggestions + text extraction + search indexing
- Tool interception for search_web / read_webpage (and extensible)
- Logs dialog (diagnostics + LoRA-style data)
- Backup/Restore ZIP

**Phase 6 – Polish & Future Hooks**
- Merge folders, advanced export, error resilience
- Placeholders for Google Drive, richer analytics, external MCP/search providers
- Final blueprint and tasks.md audit

## CURRENT_CONTEXT (to be updated by implementing AI)
Initial state: Master prompt created. Ready for Phase 1.

## FLOWCHART OF SYSTEMS (text representation)
User → Sidebar (Folders + Search) → Chat Screen (Messages + Attachments + Stars)
Chat Screen ↔ Model Router (Auto/Desktop/Local LiteRT-LM)
Model Router → Tool Interceptor → (search_web / read_webpage / …) → HTTP/Proxy → back to model
Chat Screen → Artifact Engine → Report / Blueprint (append) / tasks.md / Prompt
All data → Room + Filesystem ←→ Backup ZIP / Export
Settings → AI Models / Logs / Backup-Delete

## BLUEPRINT TEMPLATE (exact – use this for every generated project)
# <App Name> Project Blueprint

## PROJECT_SIGNATURE
<one paragraph: what the app is + its core architectural idea>

## SCREENS
<per-screen bullets: layout, controls, dialogs>

## LOGIC_TREE
<numbered core mechanisms: pipelines, orchestration flows, protocols>

## DATA_SCHEMA
<key data classes with fields and meaning>

## BUILD & TOOLING
<language/SDK versions, key dependencies, build + test commands>

## TIMELINE & DECISIONS
- **<date, HH:MM>**: <what was implemented>
  (append-only — history is never edited)

## CURRENT_CONTEXT
<two sentences: current state and next focus>

## FLOWCHART OF SYSTEMS
<text or mermaid-style flowchart showing each system, how they relate, and what they do>

---
**END OF MASTER PROMPT**

Instructions to the implementing AI:
- Read this entire file first.
- Create/update blueprint.md and tasks.md immediately.
- Proceed phase by phase only.
- At the end of every response/session, append a TIMELINE entry and refresh CURRENT_CONTEXT.
- When the user asks for code, generate only the current phase unless explicitly told otherwise.
- Always keep tools enabled and document how search_web / read_webpage interception works.
- This app will be part of my portfolio for hiring purposes, so make sure all code is clean and professional