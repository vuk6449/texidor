package com.example

import com.example.data.model.EditorPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun autoSave_defaultsToTrue() {
    val prefs = EditorPreferences()
    assertTrue("Auto-save must be enabled by default", prefs.autoSaveEnabled)
  }

  @Test
  fun editorPreferences_canToggleAutoSave() {
    val prefs = EditorPreferences()
    val toggled = prefs.copy(autoSaveEnabled = !prefs.autoSaveEnabled)
    assertFalse("Auto-save can be turned off in settings", toggled.autoSaveEnabled)
  }
}
