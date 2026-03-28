package com.nocatalog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.nocatalog.app.core.security.AppLockManager
import com.nocatalog.app.domain.repository.SecurityRepository
import com.nocatalog.app.presentation.navigation.AppNavGraph
import com.nocatalog.app.ui.theme.NoCatalogTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 单 Activity 容器，承载 Compose 导航。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var securityRepository: SecurityRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoCatalogTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            appLockManager.onAppForegrounded(securityRepository.isPasswordSet())
        }
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onAppBackgrounded()
    }
}
