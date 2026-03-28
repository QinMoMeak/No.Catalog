package com.nocatalog.app.core.common

/**
 * 统一结果包装，避免在骨架阶段直接泄漏异常细节到 UI 层。
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

/**
 * 应用级错误模型，先覆盖核心场景，后续可继续细化。
 */
sealed interface AppError {
    data class Validation(val message: String) : AppError
    data class Network(val message: String) : AppError
    data class Storage(val message: String) : AppError
    data class Security(val message: String) : AppError
    data class Unknown(val message: String, val cause: Throwable? = null) : AppError
}

