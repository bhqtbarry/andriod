package cn.syphotos.android.ui.i18n

enum class AppLanguage(
    val code: String,
    val nativeName: String,
) {
    ZH("zh", "简体中文"),
    EN("en", "English"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    IT("it", "Italiano"),
    ID("id", "Bahasa Indonesia"),
    ES("es", "Español"),
    PT("pt", "Português"),
    TH("th", "ไทย"),
    KO("ko", "한국어"),
    JA("ja", "日本語"),
    RU("ru", "Русский"),
    VI("vi", "Tiếng Việt"),
    HI("hi", "हिन्दी");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}
