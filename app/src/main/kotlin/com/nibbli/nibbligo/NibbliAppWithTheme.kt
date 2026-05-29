package com.nibbli.nibbligo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.navigation.NibbliApp
import com.nibbli.nibbligo.presentation.MainViewModel

@Composable
fun NibbliAppWithTheme(viewModel: MainViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    NibbliTheme(themeMode = themeMode) {
        NibbliApp()
    }
}
