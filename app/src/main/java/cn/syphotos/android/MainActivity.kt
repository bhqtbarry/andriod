package cn.syphotos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.syphotos.android.ui.SyPhotosApp
import cn.syphotos.android.ui.theme.SyPhotosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyPhotosTheme {
                SyPhotosApp()
            }
        }
    }
}

