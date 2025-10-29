// Models.kt
package ru.sonic.zabbix.zbx

data class ZabbixServer(
    val id: Long,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val useApiKey: Boolean = false,
    val apiKey: String = ""
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "English"
)