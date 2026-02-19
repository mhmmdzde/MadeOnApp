package ir.madeon

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import ir.madeon.ui.theme.MadeOnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MadeOnTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // فراخوانی کامپوزابل وب‌ویو
                    WebViewScreen(
                        url = "https://madeon.ir",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // مدیریت دکمه بازگشت (Back) برای مرور تاریخچه وب‌سایت
    var webView: WebView? = null
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this // ذخیره رفرنس برای مدیریت دکمه بازگشت
                webViewClient = WebViewClient() // باز شدن لینک‌ها داخل خود اپلیکیشن
                settings.javaScriptEnabled = true // فعال‌سازی جاوا اسکریپت
                settings.domStorageEnabled = true // فعال‌سازی ذخیره‌سازی لوکال
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}