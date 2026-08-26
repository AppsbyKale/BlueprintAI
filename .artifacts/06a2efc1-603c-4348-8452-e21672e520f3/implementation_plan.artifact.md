# Implementation Plan - UI Refinement & Model Permissions

This plan addresses several UI improvements, permission handling for local models, and expanded AI model configuration (Gemini API).

## Proposed Changes

### 1. Permissions & Manifest
- [MODIFY] [AndroidManifest.xml](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/AndroidManifest.xml)
  - Add `android.permission.MANAGE_EXTERNAL_STORAGE` for local model access on Android 11+.

### 2. Data & Models
- [MODIFY] [Settings.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/data/Settings.kt)
  - Add `geminiApiKey` and `isModelReady` flag (local storage).
- [MODIFY] [SettingsViewModel.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/ui/SettingsViewModel.kt)
  - Add functions to update Gemini API key and trigger "All Files" permission intent.

### 3. UI Components
- [MODIFY] [ChatScreen.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/ui/ChatScreen.kt)
  - Move `ChatInputBar` to the top of the screen.
  - Add logic to show a "File attached: [Name]" message in the chat immediately after selection.
- [MODIFY] [SidebarContent.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/ui/SidebarContent.kt)
  - Ensure "New Folder" button text is explicitly white.
  - Update long-press menu to include: Edit, Merge, Export, and Delete.
- [MODIFY] [MainActivity.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/MainActivity.kt)
  - Change the Auto/Desktop/Phone selection from a dropdown list to a segmented button/toggle UI.
  - Integrate the new "AI Models Dialog".
- [MODIFY] [SettingsDialogs.kt](file:///C:/Users/ekwha/AndroidStudioProjects/BlueprintAI/app/src/main/java/com/example/blueprintai/ui/SettingsDialogs.kt)
  - [NEW] `AiModelsDialog`: Includes fields for Local Path (with permission button), OpenAI IP/Port (auto-appends `/v1`), and Gemini API Key.
  - Update `LogsDialog`: Add status indicators at the top for each model's availability.

### 4. Git Integration
- Provide a summary of shell commands to initialize and push the project to the specified GitHub repository.

## Verification Plan

### Automated Tests
- Build and run the app to ensure no regressions in chat or folder logic.

### Manual Verification
- **Permissions**: Verify that tapping the "Local Storage Permission" button opens the system settings for "All files access".
- **UI Layout**: Confirm `ChatInputBar` is at the top and doesn't obscure the list.
- **Attachment**: Verify a system message or bubble appears when a file is selected.
- **Diagnostics**: Check if the top of the logs dialog shows current model status (Ready/Not Initialized).
