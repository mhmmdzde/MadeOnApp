package ir.madeon

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ir.madeon.ui.theme.MadeOnTheme

class MainActivity : ComponentActivity() {

    // لانچر برای درخواست مجوزها در زمان اجرا
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val readStorageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: true // در اندروید ۱۳+ متفاوت است

        if (!fineLocationGranted && !coarseLocationGranted) {
            Toast.makeText(this, "دسترسی به موقعیت مکانی داده نشد. برخی امکانات سایت ممکن است کار نکنند.", Toast.LENGTH_LONG).show()
        }
        if (!readStorageGranted) {
            Toast.makeText(this, "دسترسی به فایل‌ها داده نشد. آپلود فایل ممکن است با مشکل مواجه شود.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // بررسی و درخواست مجوزها قبل از لود برنامه
        checkAndRequestPermissions()

        setContent {
            MadeOnTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    WebViewScreen(
                        url = "https://madeon.ir", // فاصله اضافی حذف شد
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    /**
     * بررسی مجوزهای Location و Storage و درخواست آن‌ها در صورت نیاز
     */
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // اضافه کردن مجوزهای لوکیشن
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // اضافه کردن مجوز استوریج (برای اندروید ۱۲ و پایین‌تر)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            // برای اندروید ۱۳ به بالا (اختیاری - اگر سایت نیاز به دسترسی مستقیم به مدیا دارد)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        // درخواست مجوزها اگر لیست خالی نبود
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // استفاده از mutableStateOf برای مدیریت رفرنس WebView
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    // مدیریت دکمه بازگشت سیستم: اگر وب‌ویو تاریخچه داشت، به عقب برود
    BackHandler(enabled = webViewState.value?.canGoBack() == true) {
        webViewState.value?.goBack()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewState.value = this // ذخیره رفرنس

                // --- تنظیمات پایه ---
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true

                // --- تنظیمات لوکیشن ---
                settings.setGeolocationEnabled(true)
                // تنظیم Origin برای لوکیشن (اختیاری اما توصیه شده)
                settings.setGeolocationDatabasePath(ctx.filesDir.path)

                // باز شدن لینک‌ها درون خود WebView
                webViewClient = WebViewClient()

                // --- مدیریت تعاملات پیشرفته (فایل و لوکیشن) ---
                webChromeClient = object : WebChromeClient() {

                    // ۱. مدیریت درخواست آپلود فایل (دسترسی به گالری/فایل منیجر)
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        // اجازه می‌دهد سایت فایل‌سلکتور اندروید را باز کند
                        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                    }

                    // ۲. مدیریت درخواست دسترسی به موقعیت مکانی توسط سایت
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        // اگر کاربر مجوز اندروید را داده باشد، ما هم از سمت WebView تایید می‌کنیم
                        // پارامترها: origin, allow (true), retain (false - دیگر نپرس)
                        callback?.invoke(origin, true, false)
                    }
                }

                loadUrl(url.trim()) // حذف فاصله‌های احتمالی از آدرس
            }
        },
        modifier = modifier
    )
}