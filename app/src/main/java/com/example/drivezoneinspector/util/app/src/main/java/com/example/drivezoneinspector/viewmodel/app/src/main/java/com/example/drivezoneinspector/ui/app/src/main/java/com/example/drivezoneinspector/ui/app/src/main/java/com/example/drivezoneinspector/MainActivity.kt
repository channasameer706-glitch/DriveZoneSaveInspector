package com.example.drivezoneinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.drivezoneinspector.ui.AppTheme
import com.example.drivezoneinspector.ui.InspectorDashboard
import com.example.drivezoneinspector.viewmodel.InspectorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InspectorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                InspectorDashboard(viewModel = viewModel)
            }
        }
    }
}
