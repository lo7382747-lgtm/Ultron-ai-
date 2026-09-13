package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.tools.ToolManager
import com.example.data.preferences.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ULTRON", appName)
  }

  @Test
  fun `test tool manager declaration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val toolManager = ToolManager(context)
    val toolsJson = toolManager.getToolsDeclarationJson()
    assertNotNull(toolsJson)
    assertTrue(toolsJson.length() > 0)
  }

  @Test
  fun `test preferences manager defaults`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesManager(context)
    assertEquals("Asik", prefs.settings.value.userName)
  }
}

