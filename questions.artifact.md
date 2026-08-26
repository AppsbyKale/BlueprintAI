# Clarification Questions for BlueprintAI

1. **Gemma Versioning**: The prompt mentions "Gemma 4 E2B". As of the current date, Gemma 2 is the latest widely known release. Is "Gemma 4" a specific custom model or a placeholder for a future version?
2. **LiteRT-LM Integration**: For local inference, do you prefer the use of MediaPipe LLM Inference API (which uses LiteRT under the hood) or the low-level LiteRT (TFLite) C++/Java API directly?
3. **Tool Interception Backend**: For `search_web` and `read_webpage`, should I implement a specific integration (like Serper, Google Search API, or a self-hosted SearXNG instance), or just the infrastructure to handle the tool call and return a result?
4. **Markdown Rendering**: Do you have a preferred Jetpack Compose Markdown library, or should I choose a standard one (e.g., `com.github.jeziellago:compose-markdown`)?
5. **Multimodal Extraction**: For extracting text from PDF and images, should I use Google's ML Kit (local) or a specific cloud-based service?
6. **"Auto" Mode Heartbeat**: For the Auto mode toggle, should the app maintain a background heartbeat to the Desktop IP to determine availability, or check only when a message is sent?
7. **Artifact File Locations**: Should `blueprint.md` and `tasks.md` be located at the root of the Android project (folder containing `settings.gradle.kts`) or inside the `:app` module?
8. **Initial Model Path**: You provided paths like `/Internal storage/Download/AI_Models/`. In Android, "Internal storage" usually translates to `/storage/emulated/0/`. Should I assume this base path for the default picker?
9. **Gemma 3 as "Organizer"**: How should Gemma 3 function as an "organizer"? Should it run in the background to summarize conversations into the Blueprint, or is it a user-selectable option for chat?
10. **Theme Details**: "Pure dark grayscale" — should I avoid *any* elevation shadows (using borders instead) to keep it truly flat and "Grok-like", or is standard Material 3 elevation (in grayscale) acceptable?
