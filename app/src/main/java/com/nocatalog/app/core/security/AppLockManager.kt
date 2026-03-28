package com.nocatalog.app.core.security

import android.os.SystemClock
import com.nocatalog.app.core.common.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理当前会话的解锁态，超时逻辑后续叠加到此类。
 */
@Singleton
class AppLockManager @Inject constructor() {
    private val unlocked = MutableStateFlow(false)
    private var backgroundedAt: Long? = null

    fun markUnlocked() {
        unlocked.value = true
        backgroundedAt = null
    }

    fun markLocked() {
        unlocked.value = false
    }

    fun onAppBackgrounded() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded(passwordSet: Boolean) {
        if (!passwordSet) {
            markUnlocked()
            return
        }
        val backgroundAt = backgroundedAt ?: return
        val elapsed = SystemClock.elapsedRealtime() - backgroundAt
        if (elapsed >= Constants.DEFAULT_LOCK_TIMEOUT_SECONDS * 1000L) {
            markLocked()
        }
        backgroundedAt = null
    }

    fun observeUnlocked(): StateFlow<Boolean> = unlocked.asStateFlow()
}
