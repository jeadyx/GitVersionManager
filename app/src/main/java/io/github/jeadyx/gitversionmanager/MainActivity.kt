package io.github.jeadyx.gitversionmanager

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.jeadyx.gitversionmanager.ui.theme.GitVersionManagerTheme
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
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
    var gitManager = remember {
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
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        var ownerName by remember { mutableStateOf("") }
        var repoName by remember { mutableStateOf("") }
        var accessToken by remember { mutableStateOf("") }
        TextField(value = ownerName, onValueChange = { ownerName = it }, placeholder = { Text("ownerName") })
        TextField(value = repoName, onValueChange = { repoName = it }, placeholder = { Text("repoName") })
        TextField(value = accessToken, onValueChange = { accessToken = it }, placeholder = { Text("accessToken") })
        ButtonText("初始化") {
            gitManager = GitManager(ownerName, repoName, accessToken)
        }
        ButtonText("检查新版本") {
            gitManager.getLatestServerVersion("test/versions.json") {
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
        ButtonText("获取应用列表信息") {
            thread {
                gitManager.getCategory("category.json") { res ->
                    showDialog1("$res")
                }
            }
        }
        Row {
//            var path by remember { mutableStateOf("timetodo/version.json") }
//            var path by remember { mutableStateOf("test/mm.jpg") }
            var path by remember { mutableStateOf("test/test-1.0.apk") }
            var current by remember { mutableStateOf(-1) }
            var total by remember { mutableStateOf(-1) }
            var progress by remember { mutableStateOf(-1) }
            TextField(value = path, onValueChange = { path = it })
            ButtonText(if(progress!=-1) "($progress%)下载指定文件 ${gitManager.formatSize(current.toLong())} / ${gitManager.formatSize(total.toLong())}" else "下载到下载目录") {
                current = 0
                total = 0
                progress = 0
                thread {
                    gitManager.downloadFile(path, "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/$path") { res ->
                        total = res.total
                        current = res.current
                        progress = res.progress
                        if(res.progress == 100) {
                            val isApk = res.savePath.endsWith(".apk")
                            showDialog1(DialogActions("$res", cancelText = if(isApk) "忽略" else "", confirmText = if(isApk) "安装" else "确定"){
                                if(isApk){
                                    gitManager.openAPK(context, res.savePath)
                                }
                            })
                        }else if(res.errMsg!=null){
                            showDialog1(res.errMsg!!)
                        }
                    }
                }
            }
        }
    }
}

fun showToast(context: Context, title: String){
    Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
}