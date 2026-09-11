package com.example.blueprintai.testing.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.blueprintai.ui.BackupDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDialogsE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun backupDialogDisplaysTitleAndActionButtons() {
        composeTestRule.setContent {
            BackupDialog(
                onDismiss = {},
                onCreateBackup = {},
                onRestoreBackup = {}
            )
        }

        composeTestRule.onNodeWithText("Backup & Restore").assertExists()
        composeTestRule.onNodeWithText("Create Full Backup (.zip)").assertExists()
        composeTestRule.onNodeWithText("Restore from ZIP").assertExists()
    }
}
