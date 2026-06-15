package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Dynamic SDK version
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun `read string from context verify TransKey name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TransKey", appName)
    }

    @Test
    fun `verify MainViewModel state properties initialization`() {
        // Instantiate ViewModel
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = MainViewModel(app)
        
        // Assert initialized state models
        assertEquals(AppScreen.TRANSLATE, viewModel.currentScreen.value)
        assertEquals("Bengali", viewModel.sourceLang.value)
        assertEquals("English", viewModel.targetLang.value)
        assertEquals("", viewModel.inputText.value)
        assertEquals("Waiting for input...", viewModel.translatedText.value)
        assertEquals(0, viewModel.accentColorIndex.value)
    }
}
