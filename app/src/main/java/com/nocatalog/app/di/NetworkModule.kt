package com.nocatalog.app.di

import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.core.webdav.WebDavClient
import com.nocatalog.app.core.webdav.WebDavFileItem
import com.nocatalog.app.domain.model.WebDavConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideWebDavClient(okHttpClient: OkHttpClient): WebDavClient {
        return object : WebDavClient {
            override suspend fun ensureDir(config: WebDavConfig, path: String): AppResult<Unit> {
                val request = baseRequest(config, path)
                    .method("MKCOL", ByteArray(0).toRequestBody())
                    .build()
                return okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 405) {
                        AppResult.Success(Unit)
                    } else {
                        AppResult.Error(AppError.Network("创建远端目录失败: ${response.code}"))
                    }
                }
            }

            override suspend fun list(
                config: WebDavConfig,
                path: String,
            ): AppResult<List<WebDavFileItem>> {
                val request = baseRequest(config, path)
                    .header("Depth", "1")
                    .method("PROPFIND", ByteArray(0).toRequestBody("application/xml".toMediaType()))
                    .build()
                return okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 207) {
                        val body = response.body?.string().orEmpty()
                        AppResult.Success(parsePropFind(path, body))
                    } else {
                        AppResult.Error(AppError.Network("列出远端目录失败: ${response.code}"))
                    }
                }
            }

            override suspend fun upload(
                config: WebDavConfig,
                remotePath: String,
                bytes: ByteArray,
                contentType: String,
            ): AppResult<Unit> {
                val request = baseRequest(config, remotePath)
                    .put(bytes.toRequestBody(contentType.toMediaType()))
                    .build()
                return okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        AppResult.Success(Unit)
                    } else {
                        AppResult.Error(AppError.Network("上传远端文件失败: ${response.code}"))
                    }
                }
            }

            override suspend fun download(
                config: WebDavConfig,
                remotePath: String,
            ): AppResult<ByteArray> {
                val request = baseRequest(config, remotePath).get().build()
                return okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AppResult.Error(AppError.Network("下载远端文件失败: ${response.code}"))
                    } else {
                        AppResult.Success(response.body?.bytes() ?: byteArrayOf())
                    }
                }
            }

            private fun baseRequest(config: WebDavConfig, path: String): Request.Builder {
                val url = "${config.baseUrl.trimEnd('/')}/${path.trimStart('/')}"
                return Request.Builder()
                    .url(url)
                    .header("Authorization", Credentials.basic(config.username, config.password))
            }

            private fun parsePropFind(path: String, xml: String): List<WebDavFileItem> {
                if (xml.isBlank()) return emptyList()
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser().apply {
                    setInput(StringReader(xml))
                }
                val items = mutableListOf<WebDavFileItem>()
                var eventType = parser.eventType
                var href: String? = null
                var contentLength: Long? = null
                var lastModified: String? = null
                var isDirectory = false
                var currentTag: String? = null
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name.substringAfter(':')
                            if (currentTag == "response") {
                                href = null
                                contentLength = null
                                lastModified = null
                                isDirectory = false
                            }
                            if (currentTag == "collection") {
                                isDirectory = true
                            }
                        }

                        XmlPullParser.TEXT -> {
                            when (currentTag) {
                                "href" -> href = parser.text
                                "getcontentlength" -> contentLength = parser.text.toLongOrNull()
                                "getlastmodified" -> lastModified = parser.text
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            val tag = parser.name.substringAfter(':')
                            val currentHref = href
                            if (tag == "response" && currentHref != null) {
                                val normalizedPath = decodeHref(currentHref)
                                val cleanedBase = "/${path.trim('/')}/"
                                if (!normalizedPath.trimEnd('/').endsWith(cleanedBase.trimEnd('/'))) {
                                    val name = normalizedPath.trimEnd('/').substringAfterLast('/')
                                    items += WebDavFileItem(
                                        path = normalizedPath,
                                        name = name,
                                        isDirectory = isDirectory,
                                        contentLength = contentLength,
                                        lastModified = lastModified,
                                    )
                                }
                            }
                            currentTag = null
                        }
                    }
                    eventType = parser.next()
                }
                return items.distinctBy { it.path }
            }

            private fun decodeHref(value: String): String {
                return java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
            }
        }
    }
}
