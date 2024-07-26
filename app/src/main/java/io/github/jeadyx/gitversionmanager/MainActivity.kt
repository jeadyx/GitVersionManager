package io.github.jeadyx.gitversionmanager

import android.content.Context
import android.icu.lang.UCharacter.LineBreak.H3
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.jeady.jxcompose.manager.software.GitManager
import io.github.jeadyx.gitversionmanager.ui.theme.GitVersionManagerTheme
import java.io.File
import kotlin.concurrent.thread

private val TAG = "MainActivity"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitVersionManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                        GitManagerExample(Modifier.padding(innerPadding))
                        Dialog1()
                    }
                }
            }
        }
    }
}


@Composable
fun GitManagerExample(modifier: Modifier = Modifier){
    val context = LocalContext.current
    val downloadDir: String= run{
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        var saveDir = downloadDir + "/" + context.packageName
        if(!File(saveDir).exists()){
            if(!File(saveDir).mkdirs()){
                saveDir = downloadDir
            }
        }
        saveDir
    }
    var apkPath by remember {
        mutableStateOf("$downloadDir/release.apk")
    }
    val gitManager = remember {
//            GitManager("jeadyu", "healthcare-publisher", "QnPoZQzjJDER4Kv6wKx8VLVQ") // gitcode
//            GitManager("jeadyu", "healthcare-publisher", "af19696ba3697a0d2831598268441d79") // gitee
        GitManager("jeady5", "publisher", "e8d83e715fa7b44d2d89b3cf7d7554e0")
    }

    fun downloadFinish(state: String){
        val validState = state.endsWith(".apk") && File(state).exists()
        showDialog1(DialogActions("下载结束: $state",
            cancelText = if(validState) "忽略" else "知道了",
            confirmText = if(validState) "安装" else ""
        ){
                gitManager.openAPK(context, state)
        })
    }
    Column(modifier) {
        ButtonText("检查新版本") {
            gitManager.getLatestServerVersion("test/versions.json") {
                Log.d(TAG, "GitManagerExample: get servrer version inofo $it")
                it?.let { v ->
                    val currentVersion = context.packageManager.getPackageInfo(context.packageName,0).versionName
                    if (gitManager.isNewVersion(v.version, currentVersion)) {
                        val downloadPath = "$downloadDir/${v.appName}_${v.version}.apk"
                        val hasDownloaded = File(downloadPath).exists()
                        showDialog1(DialogActions(
                            "《${v.appName}》$currentVersion->${v.version}\n检测到新版本\n更新描述：${v.description}",
                            cancelText = if(hasDownloaded) "直接安装" else "忽略",
                            confirmText = if(hasDownloaded) "重新下载" else "下载",
                            onCancel = {
                                if(hasDownloaded){
                                    gitManager.openAPK(context, downloadPath)
                                }
                            }
                        ) {
                            gitManager.downloadFile(v.downloadUrl, downloadPath) { res ->
                                Log.d(TAG, "GitManagerExample: 下载信息：$res")
                                if(res.progress==100) {
                                    apkPath = res.savePath
                                    downloadFinish(apkPath)
                                }
                            }
                        })
                    }
                } ?: showToast(context, "检查失败，可能信息有误")
            }
        }
        ButtonText("获取版本信息") {
            thread {
                gitManager.getVersions("test/versions.json") { res ->
                    showDialog1("$res")
                }
            }
        }
    }
}

fun showToast(context: Context, title: String){
    Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
}