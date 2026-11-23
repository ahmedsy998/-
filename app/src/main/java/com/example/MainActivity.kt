package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MasroofyRoot
import com.example.ui.theme.MasroofyTheme
import com.example.ui.viewmodel.AddEditTransactionViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MasroofyViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        MasroofyViewModelFactory((application as MasroofyApp).repository)
    }

    private val mainViewModel: MainViewModel by viewModels { viewModelFactory }
    private val addEditViewModel: AddEditTransactionViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            MasroofyTheme(themeMode = uiState.themeMode) {
                MasroofyRoot(
                    mainViewModel = mainViewModel,
                    addEditViewModel = addEditViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshRecurringOnResume()
    }
}
