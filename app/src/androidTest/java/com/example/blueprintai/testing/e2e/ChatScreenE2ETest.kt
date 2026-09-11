package com.example.blueprintai.testing.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.blueprintai.data.Attachment
import com.example.blueprintai.ui.AttachmentNoticeBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun attachmentNoticeBarDisplaysAttachmentNames() {
        val testAttachments = listOf(
            Attachment(
                id = 1,
                folderId = 1,
                originalName = "architecture_doc.pdf",
                aiSuggestedName = "ArchDoc.pdf",
                type = "application/pdf",
                uri = "content://test/1"
            )
        )

        composeTestRule.setContent {
            AttachmentNoticeBar(attachments = testAttachments)
        }

        composeTestRule.onNodeWithText("Attachments").assertExists()
        composeTestRule.onNodeWithText("ArchDoc.pdf").assertExists()
    }
}
