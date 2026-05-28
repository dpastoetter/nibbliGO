package com.nibbli.nibbligo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.navigation.NibbliApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NibbliTheme {
                NibbliApp()
            }
        }
    }
}
