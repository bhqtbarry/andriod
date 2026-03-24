package cn.syphotos.android.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private const val APP_NAME = "SY Photos"

@Immutable
class AppStrings(
    val appLanguage: AppLanguage,
    private val values: Map<String, String>,
) {
    val appName get() = APP_NAME
    val appTagline get() = text("app_tagline")
    val navAllPhotos get() = text("nav_all_photos")
    val navMap get() = text("nav_map")
    val navUpload get() = text("nav_upload")
    val navCategory get() = text("nav_category")
    val navMy get() = text("nav_my")
    val allPhotosTitle get() = text("all_photos_title")
    val allPhotosSubtitle get() = text("all_photos_subtitle")
    val searchHint get() = text("search_hint")
    val author get() = text("author")
    val airline get() = text("airline")
    val aircraftModel get() = text("aircraft_model")
    val camera get() = text("camera")
    val lens get() = text("lens")
    val registration get() = text("registration")
    val location get() = text("location")
    val mapTitle get() = text("map_title")
    val mapSubtitle get() = text("map_subtitle")
    val uploadTitle get() = text("upload_title")
    val uploadSubtitle get() = text("upload_subtitle")
    val uploadFieldTitle get() = text("upload_field_title")
    val uploadFieldRegistration get() = text("upload_field_registration")
    val uploadFieldAircraftModel get() = text("upload_field_aircraft_model")
    val uploadFieldAirline get() = text("upload_field_airline")
    val uploadFieldShootingTime get() = text("upload_field_shooting_time")
    val uploadFieldShootingTimeHint get() = text("upload_field_shooting_time_hint")
    val uploadFieldShootingLocation get() = text("upload_field_shooting_location")
    val uploadFieldShootingLocationHint get() = text("upload_field_shooting_location_hint")
    val uploadFieldCamera get() = text("upload_field_camera")
    val uploadFieldLens get() = text("upload_field_lens")
    val uploadWatermarkSettings get() = text("upload_watermark_settings")
    val uploadWatermarkSize get() = text("upload_watermark_size")
    val uploadWatermarkOpacity get() = text("upload_watermark_opacity")
    val uploadWatermarkColor get() = text("upload_watermark_color")
    val uploadAuthorStyle get() = text("upload_author_style")
    val uploadStyleDefault get() = text("upload_style_default")
    val uploadStyleSimple get() = text("upload_style_simple")
    val uploadStyleBold get() = text("upload_style_bold")
    val uploadColorWhite get() = text("upload_color_white")
    val uploadColorBlack get() = text("upload_color_black")
    val uploadWatermarkPosition get() = text("upload_watermark_position")
    val uploadPositionTopLeft get() = text("upload_position_top_left")
    val uploadPositionTopCenter get() = text("upload_position_top_center")
    val uploadPositionTopRight get() = text("upload_position_top_right")
    val uploadPositionMiddleLeft get() = text("upload_position_middle_left")
    val uploadPositionMiddleCenter get() = text("upload_position_middle_center")
    val uploadPositionMiddleRight get() = text("upload_position_middle_right")
    val uploadPositionBottomLeft get() = text("upload_position_bottom_left")
    val uploadPositionBottomCenter get() = text("upload_position_bottom_center")
    val uploadPositionBottomRight get() = text("upload_position_bottom_right")
    val uploadTermsTitle get() = text("upload_terms_title")
    val uploadTermsDesc get() = text("upload_terms_desc")
    val uploadSubmit get() = text("upload_submit")
    val chooseImage get() = text("choose_image")
    val exifEnabled get() = text("exif_enabled")
    val watermarkEnabled get() = text("watermark_enabled")
    val categoryTitle get() = text("category_title")
    val categorySubtitle get() = text("category_subtitle")
    val myTitle get() = text("my_title")
    val mySubtitle get() = text("my_subtitle")
    val languageTitle get() = text("language_title")
    val languageSubtitle get() = text("language_subtitle")
    val account get() = text("account")
    val userLabel get() = text("user_label")
    val emailLabel get() = text("email_label")
    val passwordField get() = text("password_field")
    val signIn get() = text("sign_in")
    val signOut get() = text("sign_out")
    val loginLabel get() = text("login_label")
    val myWorks get() = text("my_works")
    val myLikes get() = text("my_likes")
    val pending get() = text("pending")
    val rejected get() = text("rejected")
    val devices get() = text("devices")
    val emailVerified get() = text("email_verified")
    val emailVerificationRequired get() = text("email_verification_required")
    val passwordChange get() = text("password_change")
    val currentDevice get() = text("current_device")
    val revocable get() = text("revocable")
    val back get() = text("back")
    val like get() = text("like")
    val unlike get() = text("unlike")
    val share get() = text("share")
    val language get() = text("language")

    fun pageTitle(route: String?): String = when (route) {
        "all_photos" -> allPhotosTitle
        "map" -> mapTitle
        "upload" -> uploadTitle
        "category" -> categoryTitle
        "my" -> myTitle
        else -> appName
    }

    fun pageSubtitle(route: String?): String = when (route) {
        "all_photos" -> allPhotosSubtitle
        "map" -> mapSubtitle
        "upload" -> uploadSubtitle
        "category" -> categorySubtitle
        "my" -> mySubtitle
        else -> appTagline
    }

    fun navLabel(route: String): String = when (route) {
        "all_photos" -> navAllPhotos
        "map" -> navMap
        "upload" -> navUpload
        "category" -> navCategory
        "my" -> navMy
        else -> appName
    }

    fun photosCount(count: Int): String = format("photos_count", "count" to count)
    fun worksCount(count: Int): String = format("works_count", "count" to count)
    fun likesCount(count: Int): String = format("likes_count", "count" to count)
    fun editableCount(count: Int): String = format("editable_count", "count" to count)
    fun reason(value: String): String = format("reason_value", "value" to value)
    fun admin(value: String): String = format("admin_value", "value" to value)

    private fun text(key: String): String = values[key] ?: english[key] ?: key

    private fun format(key: String, vararg values: Pair<String, Any>): String {
        var result = text(key)
        values.forEach { (name, value) ->
            result = result.replace("{$name}", value.toString())
        }
        return result
    }

    companion object {
        private val english = mapOf(
            "app_tagline" to "Aviation photography, rebuilt for a sharper mobile flow.",
            "nav_all_photos" to "Explore",
            "nav_map" to "Map",
            "nav_upload" to "Upload",
            "nav_category" to "Airlines",
            "nav_my" to "Profile",
            "all_photos_title" to "All Photos",
            "all_photos_subtitle" to "Fast search, richer metadata, and a layout that feels editorial instead of utilitarian.",
            "search_hint" to "Keyword, airport, or title",
            "author" to "Author",
            "airline" to "Airline",
            "aircraft_model" to "Aircraft model",
            "camera" to "Camera",
            "lens" to "Lens",
            "registration" to "Registration",
            "location" to "Location / IATA",
            "map_title" to "Photo Map",
            "map_subtitle" to "Use geography as the starting point, then jump back into the full archive with shared filters.",
            "upload_title" to "Upload",
            "upload_subtitle" to "Use the same web upload flow and fields, without the extra filler text.",
            "upload_field_title" to "Title",
            "upload_field_registration" to "Registration",
            "upload_field_aircraft_model" to "Aircraft model",
            "upload_field_airline" to "Airline",
            "upload_field_shooting_time" to "Shooting time",
            "upload_field_shooting_time_hint" to "YYYY-MM-DDTHH:MM",
            "upload_field_shooting_location" to "Airport / IATA",
            "upload_field_shooting_location_hint" to "CGK",
            "upload_field_camera" to "Camera",
            "upload_field_lens" to "Lens",
            "upload_watermark_settings" to "Watermark",
            "upload_watermark_size" to "Size",
            "upload_watermark_opacity" to "Opacity",
            "upload_watermark_color" to "Color",
            "upload_author_style" to "Author style",
            "upload_style_default" to "Default",
            "upload_style_simple" to "Simple",
            "upload_style_bold" to "Bold",
            "upload_color_white" to "White",
            "upload_color_black" to "Black",
            "upload_watermark_position" to "Position",
            "upload_position_top_left" to "Top left",
            "upload_position_top_center" to "Top center",
            "upload_position_top_right" to "Top right",
            "upload_position_middle_left" to "Middle left",
            "upload_position_middle_center" to "Middle center",
            "upload_position_middle_right" to "Middle right",
            "upload_position_bottom_left" to "Bottom left",
            "upload_position_bottom_center" to "Bottom center",
            "upload_position_bottom_right" to "Bottom right",
            "upload_terms_title" to "Terms",
            "upload_terms_desc" to "I confirm I own the image and allow the platform to use it according to the site rules.",
            "upload_submit" to "Submit Upload",
            "choose_image" to "Choose Image",
            "exif_enabled" to "EXIF auto extraction is enabled",
            "watermark_enabled" to "Website watermark rules will be reused",
            "category_title" to "Airlines",
            "category_subtitle" to "Scan the archive by airline, with a denser ranking layout inspired by the web airline directory.",
            "my_title" to "My Space",
            "my_subtitle" to "Account controls, review queue, likes, and devices in one place.",
            "language_title" to "Language",
            "language_subtitle" to "Switch the app interface instantly without leaving the current screen.",
            "account" to "Account",
            "user_label" to "User",
            "email_label" to "Email",
            "password_field" to "Password",
            "sign_in" to "Sign in",
            "sign_out" to "Sign out",
            "login_label" to "Login",
            "my_works" to "My Works",
            "my_likes" to "My Likes",
            "pending" to "Pending",
            "rejected" to "Rejected",
            "devices" to "Devices",
            "email_verified" to "Email verified",
            "email_verification_required" to "Email verification required",
            "password_change" to "Password changes remain available in the auth flow.",
            "current_device" to "Current device",
            "revocable" to "Revocable",
            "back" to "Back",
            "like" to "Like",
            "unlike" to "Unlike",
            "share" to "Share",
            "language" to "Language",
            "photos_count" to "{count} photos",
            "works_count" to "Current loaded works: {count}",
            "likes_count" to "Liked photos: {count}",
            "editable_count" to "Editable items: {count}",
            "reason_value" to "Reason: {value}",
            "admin_value" to "Admin: {value}",
        )

        private val translations = mapOf(
            AppLanguage.ZH to english + mapOf(
                "app_tagline" to "为航空摄影重新设计的更锐利移动端体验。",
                "nav_all_photos" to "探索",
                "nav_map" to "地图",
                "nav_upload" to "上传",
                "nav_category" to "航司",
                "nav_my" to "我的",
                "all_photos_title" to "全部照片",
                "all_photos_subtitle" to "更快的搜索、更完整的元数据，以及更像杂志编排的布局。",
                "search_hint" to "关键词、机场或标题",
                "author" to "作者",
                "airline" to "航司",
                "aircraft_model" to "机型",
                "camera" to "相机",
                "lens" to "镜头",
                "registration" to "注册号",
                "location" to "地点 / IATA",
                "map_title" to "照片地图",
                "map_subtitle" to "先按地理位置浏览，再带着筛选条件回到完整图库。",
                "upload_title" to "上传",
                "upload_subtitle" to "直接复用网页 upload.php 的字段和流程，去掉多余说明文字。",
                "upload_field_title" to "标题",
                "upload_field_registration" to "注册号",
                "upload_field_aircraft_model" to "机型",
                "upload_field_airline" to "航司",
                "upload_field_shooting_time" to "拍摄时间",
                "upload_field_shooting_time_hint" to "YYYY-MM-DDTHH:MM",
                "upload_field_shooting_location" to "拍摄地点 / IATA",
                "upload_field_shooting_location_hint" to "CGK",
                "upload_field_camera" to "相机",
                "upload_field_lens" to "镜头",
                "upload_watermark_settings" to "水印设置",
                "upload_watermark_size" to "大小",
                "upload_watermark_opacity" to "透明度",
                "upload_watermark_color" to "颜色",
                "upload_author_style" to "作者样式",
                "upload_style_default" to "默认",
                "upload_style_simple" to "简洁",
                "upload_style_bold" to "加粗",
                "upload_color_white" to "白色",
                "upload_color_black" to "黑色",
                "upload_watermark_position" to "位置",
                "upload_position_top_left" to "左上",
                "upload_position_top_center" to "上中",
                "upload_position_top_right" to "右上",
                "upload_position_middle_left" to "左中",
                "upload_position_middle_center" to "正中",
                "upload_position_middle_right" to "右中",
                "upload_position_bottom_left" to "左下",
                "upload_position_bottom_center" to "下中",
                "upload_position_bottom_right" to "右下",
                "upload_terms_title" to "使用条款",
                "upload_terms_desc" to "我确认拥有图片权利，并同意按网站规则授权平台使用。",
                "upload_submit" to "提交上传",
                "choose_image" to "选择图片",
                "exif_enabled" to "已启用 EXIF 自动提取",
                "watermark_enabled" to "将沿用网站水印规则",
                "category_title" to "航司",
                "category_subtitle" to "参考网站 airline 页重做为高密度航司列表，用照片量快速浏览图库。",
                "my_title" to "我的空间",
                "my_subtitle" to "账号、审核队列、收藏和设备统一收纳。",
                "language_title" to "语言",
                "language_subtitle" to "无需离开当前页面即可即时切换应用界面语言。",
                "account" to "账号",
                "user_label" to "用户",
                "email_label" to "邮箱",
                "password_field" to "密码",
                "sign_in" to "登录",
                "sign_out" to "退出登录",
                "login_label" to "登录",
                "my_works" to "我的作品",
                "my_likes" to "我的喜欢",
                "pending" to "待审核",
                "rejected" to "已拒绝",
                "devices" to "设备",
                "email_verified" to "邮箱已验证",
                "email_verification_required" to "需要验证邮箱",
                "password_change" to "密码修改仍在认证流程中支持。",
                "current_device" to "当前设备",
                "revocable" to "可撤销",
                "back" to "返回",
                "like" to "喜欢",
                "unlike" to "取消喜欢",
                "share" to "分享",
                "language" to "语言",
                "photos_count" to "{count} 张照片",
                "works_count" to "当前已加载作品：{count}",
                "likes_count" to "喜欢的照片：{count}",
                "editable_count" to "可编辑项目：{count}",
                "reason_value" to "原因：{value}",
                "admin_value" to "管理员：{value}",
            ),
            AppLanguage.EN to english,
            AppLanguage.FR to english + mapOf("language" to "Langue", "nav_my" to "Profil", "choose_image" to "Choisir une image", "back" to "Retour"),
            AppLanguage.DE to english + mapOf("language" to "Sprache", "nav_my" to "Profil", "choose_image" to "Bild waehlen", "back" to "Zurueck"),
            AppLanguage.IT to english + mapOf("language" to "Lingua", "nav_my" to "Profilo", "choose_image" to "Scegli immagine", "back" to "Indietro"),
            AppLanguage.ID to english + mapOf("language" to "Bahasa", "nav_my" to "Saya", "choose_image" to "Pilih Gambar", "back" to "Kembali"),
            AppLanguage.ES to english + mapOf("language" to "Idioma", "nav_my" to "Perfil", "choose_image" to "Elegir imagen", "back" to "Atras"),
            AppLanguage.PT to english + mapOf("language" to "Idioma", "nav_my" to "Perfil", "choose_image" to "Escolher imagem", "back" to "Voltar"),
            AppLanguage.TH to english + mapOf("language" to "ภาษา", "nav_my" to "โปรไฟล์", "choose_image" to "เลือกรูปภาพ", "back" to "กลับ"),
            AppLanguage.KO to english + mapOf("language" to "언어", "nav_my" to "내 정보", "choose_image" to "이미지 선택", "back" to "뒤로"),
            AppLanguage.JA to english + mapOf("language" to "言語", "nav_my" to "マイページ", "choose_image" to "画像を選択", "back" to "戻る"),
            AppLanguage.RU to english + mapOf("language" to "Язык", "nav_my" to "Профиль", "choose_image" to "Выбрать изображение", "back" to "Назад"),
            AppLanguage.VI to english + mapOf("language" to "Ngon ngu", "nav_my" to "Ho so", "choose_image" to "Chon anh", "back" to "Quay lai"),
            AppLanguage.HI to english + mapOf("language" to "भाषा", "nav_my" to "प्रोफ़ाइल", "choose_image" to "छवि चुनें", "back" to "वापस"),
        )

        fun forLanguage(language: AppLanguage): AppStrings = AppStrings(language, translations[language] ?: english)
    }
}

val LocalAppStrings = staticCompositionLocalOf { AppStrings.forLanguage(AppLanguage.EN) }

@Composable
fun rememberAppStrings(language: AppLanguage): AppStrings = remember(language) {
    AppStrings.forLanguage(language)
}
