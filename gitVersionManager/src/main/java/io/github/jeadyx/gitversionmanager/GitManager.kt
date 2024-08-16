package io.github.jeadyx.gitversionmanager

import android.content.Context
import android.content.Intent
import android.health.connect.datatypes.AppInfo
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import io.github.jeadyx.simplenetmanager.SimpleNetManager
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.net.URLEncoder

private val TAG = "[GitManager]"

/**
 * 无需自建服务器，使用git管理你的应用版本; 默认为gitee仓库;
 * * 如需自定义服务器地址，请使用 [setHost]
 * * 关于版本文件的编写参考 [versionFileSample]
 * @see setHost 设置自定义服务器地址
 * @see versionFileSample 版本文件示例
 */
class GitManager(private val repoOwner: String, private val repoName: String, private val accessToken: String) {
//    private val server = ServerManager.getInstance("https://api.gitcode.com", 30L)
    private var server = SimpleNetManager.getInstance("https://gitee.com", 120L)

    /**
     * 设置服务器地址. 如https://gitee.com
     */
    fun setHost(url: String): Boolean{
        if(!url.startsWith("http") && !url.startsWith("https")) return false
        server =  SimpleNetManager.getInstance(url)
        return true
    }

    /**
     * 判断指定版本号是否为新版本，如v1.0.1 > v1.0.0
     * @param newVersion 新版本号
     * @param oldVersion 旧版本号
     * @return true: 新版本; false: 不是新版本
     */
    fun isNewVersion(newVersion: String, oldVersion: String): Boolean{
        val newVersionInfo = newVersion.trim().trim('v', 'V', '.').split('.')
        val oldVersionInfo = oldVersion.trim().trim('v', 'V', '.').split('.')
        if(newVersionInfo.size>oldVersionInfo.size) return true
        if(newVersionInfo.size<oldVersionInfo.size) return false
        for(i in newVersionInfo.indices){
            val newVersionNum = newVersionInfo[i].toInt()
            val oldVersionNum = oldVersionInfo[i].toInt()
            if(newVersionNum>oldVersionNum) return true
            if(newVersionNum<oldVersionNum) return false
        }
        return false
    }

    /**
     * 从服务器获取最新版本信息
     * @param filePath git版本文件的路径
     * @param latestVersionCallback 回调函数
     */
    fun getLatestServerVersion(filePath: String, latestVersionCallback: (VersionInfo?)->Unit){
         server.get("api/v5/repos/${repoOwner}/${repoName}/contents/" + URLEncoder.encode(filePath, "utf-8"),
                "access_token=${accessToken}", ResponseInfo::class.java
            ) { data, errMsg->
                data?.content?.let{
                    val content = String(Base64.decode(it, Base64.DEFAULT))
                    Gson().fromJson(content, AppVersionsInfo::class.java)?.let { appInfo->
//                        Log.d(TAG, "getLatestServerVersion: get versions $appInfo")
                        val versionInfo = appInfo.versions.find { vInfo->
                            return@find vInfo.version == appInfo.latestVersion
                        }
                        latestVersionCallback(versionInfo)
                    }
                }?:run{
                    Log.e(TAG, "getLatestServerVersion: err $errMsg", )
                    latestVersionCallback(null)
                }
            }
    }

    /**
     * 从服务器获取测试版本信息
     * @param filePath git版本文件的路径
     * @param latestVersionCallback 回调函数
     */
    fun getTestServerVersion(filePath: String, latestVersionCallback: (VersionInfo?)->Unit){
        server.get("api/v5/repos/${repoOwner}/${repoName}/contents/" + URLEncoder.encode(filePath, "utf-8"),
            "access_token=${accessToken}", ResponseInfo::class.java
        ) { data, errMsg->
            data?.content?.let{
                val content = String(Base64.decode(it, Base64.DEFAULT))
                Gson().fromJson(content, AppVersionsInfo::class.java)?.let { appInfo->
//                        Log.d(TAG, "getTestServerVersion: get versions $appInfo")
                    val versionInfo = appInfo.versions.find { vInfo->
                        return@find vInfo.version == appInfo.testVersion
                    }
                    latestVersionCallback(versionInfo)
                }
            }
        }
    }

    /**
     * 从服务器获取全部版本信息
     * @param filePath git版本文件的路径
     * @param appVersions 回调函数; return null if file not exist
     * @see AppVersionsInfo
     */
    fun getVersions(filePath: String, appVersions: (AppVersionsInfo?)->Unit){
        server.get("api/v5/repos/${repoOwner}/${repoName}/contents/" + URLEncoder.encode(filePath, "utf-8"),
            "access_token=${accessToken}", ResponseInfo::class.java
        ) { data, errMsg->
            data?.content?.let{
                val content = String(Base64.decode(it, Base64.DEFAULT))
                Gson().fromJson(content, AppVersionsInfo::class.java)?.let { appInfo->
//                    Log.d(TAG, "getServerVersionInfo: get versions $appInfo")
                    appVersions(appInfo)
                }
            }?:run { appVersions(null) }
        }
    }

    /**
     * 从服务器获取全部app信息
     * @param filePath git 上的app目录文件路径
     * @param appCategoryCallback 回调函数; return null if file not exist
     * @see AppCategory
     */
    fun getCategory(filePath: String, appCategoryCallback: (AppCategory?)->Unit){
        server.get("api/v5/repos/${repoOwner}/${repoName}/contents/" + URLEncoder.encode(filePath, "utf-8"),
            "access_token=${accessToken}", ResponseInfo::class.java
        ) { data, errMsg->
            data?.content?.let{
                val content = String(Base64.decode(it, Base64.DEFAULT))
                Gson().fromJson(content, AppCategory::class.java)?.let { appCategory->
//                    Log.d(TAG, "getServerVersionInfo: get versions $appInfo")
                    appCategoryCallback(appCategory)
                }
            }?:run{ appCategoryCallback(null) }
        }
    }

    /**
     * 从服务器下载指定文件
     * @param filePath git文件路径
     * @param fileSavePath 文件本地保存路径
     * @param downloadCallback 下载回调
     */
    fun downloadFile(filePath: String, fileSavePath: String, downloadCallback: (DownloadStatus)->Unit){
        if(filePath.startsWith("http") || filePath.startsWith("https")){
            downloadFileByUrl(filePath, fileSavePath, downloadCallback)
            return
        }
        val encodedPath = URLEncoder.encode(filePath, "utf-8")
        server.get("api/v5/repos/${repoOwner}/${repoName}/contents/$encodedPath",
                "access_token=${accessToken}", ResponseInfo::class.java
            ) { data, errMsg->
//                Log.i(TAG, "downloadFile: get contents res: ${data?.message} ${data?.content?.length} / ${data?.size} ${data?.sha}; \nerr:$errMsg")
//                Log.d(TAG, "downloadFile: ${data?.content?.length} ${data?.content}")
                data?.let { contentInfo ->
                    if (contentInfo.content.isNotBlank() && contentInfo.content.length == contentInfo.size.toInt()){
                        downloadCallback(DownloadStatus(total = contentInfo.size.toInt(), current = 0, progress = 0))
                        saveByteArrayToFile(
                            fileSavePath,
                            Base64.decode(contentInfo.content, Base64.DEFAULT)
                        )
                        downloadCallback(DownloadStatus(total = contentInfo.size.toInt(), current = contentInfo.size.toInt(), progress = 100))
                    } else {
                        downloadBlobs(contentInfo.sha, fileSavePath, downloadCallback)
                    }
                }?: run{
                    downloadCallback(DownloadStatus(errMsg=if(errMsg=="[]") "File $repoOwner/$repoName/$filePath not found!" else errMsg?:"UNKNOWN ERR", total = 0, current = 0, progress = 0))
                }
            }
    }

    /**
     * 下载文件
     */
    fun downloadOnly(apkPath: String, fileSavePath: String, downloadCallback: (DownloadStatus)->Unit){
        val encodedPath = URLEncoder.encode(apkPath, "utf-8")
        server.get(
            "api/v5/repos/${repoOwner}/${repoName}/contents/$encodedPath",
            "access_token=${accessToken}", String::class.java
        ) { res, errMsg ->
            res?.let{
                saveByteArrayToFile(fileSavePath, it.toByteArray())
            }
        }
    }

    /**
     * 下载文件
     */
    fun downloadBlobs(sha:String, fileSavePath: String, downloadCallback: (DownloadStatus)->Unit){
        server.get(
            "api/v5/repos/${repoOwner}/${repoName}/git/blobs/$sha",
            "access_token=${accessToken}", ResponseInfo::class.java
        ) { data, errMsg ->
            Log.i(
                TAG,
                "downloadBlobs: get blobs res: ${data?.content?.length} ; \nerr:$errMsg"
            )
            val file = File(fileSavePath)
            if(file.exists()) file.delete()
            data?.let { blobsInfo->
                blobsInfo.content?.let {content->
                    Log.d(TAG, "downloadFile2: ${content.length} ${content}")
                    var currentIdx = 1
                    val readOneLen = 1024 * 500
                    var readLen = 0
                    val bufferrr = Base64.decode(content, Base64.DEFAULT)
                    while (content.length > readLen){
                        var buffer = byteArrayOf()
                        if(content.length>readLen+readOneLen){
                            buffer = Base64.decode(content.substring(readLen, readLen+readOneLen), Base64.DEFAULT)
                            readLen += readOneLen
                            currentIdx++
                        }else{
                            buffer = Base64.decode(content.substring(readLen, content.length), Base64.DEFAULT)
                            readLen += content.length-readLen
                            currentIdx++
                        }
                        appendByteArrayToFile(
                            fileSavePath,
                            buffer,
                            downloadCallback
                        )
                        downloadCallback(DownloadStatus(progress = (readLen.toFloat()/content.length*100).toInt(),
                            current = readLen, total = content.length, savePath = fileSavePath))
                    }
                }?:run {
                    downloadCallback(DownloadStatus(errMsg = "下载失败: ${blobsInfo.error_msg}"))
                }
            }
        }
    }

    /**
     * 将字节数组保存到文件
     */
    private fun saveByteArrayToFile(fileSavePath: String, content: ByteArray){
        if(content.isEmpty()) return
        Log.d(TAG, "saveByteArrayToFile: save ${content.size} bytes to file")
        val file = File(fileSavePath)
        if (file.exists()){
            file.delete()
        }else if(file.parent?.let { File(it).exists() } == false){
            file.parent?.let { File(it).mkdirs() }
        }
        try {
            val outputStream = FileOutputStream(file)
            outputStream.write(content)
            outputStream.close()
        }catch (e:Exception){
            Log.e(TAG, "downloadAPK: $e")
        }
    }

    /**
     * 将字节数组追加到文件
     */
    private fun appendByteArrayToFile(fileSavePath: String, content: ByteArray, saveCallback: (DownloadStatus)->Unit){
        if(content.isEmpty()) return
        val file = File(fileSavePath)
        if (!file.exists() && file.parent?.let { File(it).exists() } == false){
            file.parent?.let { File(it).mkdirs() }
        }
        try {
            val outputStream = FileOutputStream(file, true)
            outputStream.write(content)
            outputStream.close()
        }catch (e:Exception){
            Log.e(TAG, "downloadAPK: $e")
        }
    }

    /**
     * 从url下载文件
     */
    fun downloadFileByUrl(url: String, fileSavePath: String, downloadCallback: (DownloadStatus)->Unit){
        if(!url.startsWith("http") && !url.startsWith("https")){
            downloadCallback(DownloadStatus(errMsg = "error Url"))
            return
        }
        val apkFile = File(fileSavePath)
        if (apkFile.exists()){
            apkFile.delete()
        }
        apkFile.createNewFile()
        val inputStream = URL(url).openStream()
        val outputStream = FileOutputStream(apkFile)
        val buffer = ByteArray(1024)
        var len = inputStream.read(buffer)
        Log.d(TAG, "downloadAPK: 开始下载")
        while (len != -1){
            outputStream.write(buffer, 0, len)
            len = inputStream.read(buffer)
            Log.d(TAG, "downloadAPK: 下载中 $len / ${apkFile.length()}")
        }
        Log.d(TAG, "downloadAPK: 下载完成")
        inputStream.close()
        outputStream.close()
        downloadCallback(DownloadStatus(savePath = apkFile.absolutePath, progress = 100))
    }

    /**
     * 打开apk文件进行安装，请注意已经注册相关权限
     * 注1: 在AndroidManifest.xml中，需要添加以下权限 <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     * 注2: 文件授权 "${context.packageName}.fileprovider"
     *
     * @sample
     * 1.
     * ```xml/file_path_provider.xml
     * <?xml version="1.0" encoding="utf-8"?>
     * <paths>
     *     <external-path
     *         name="apk_download_path"
     *         path="Download" />
     * </paths>
     * ```
     * 2.
     * ```在AndroidManifest.xml node application
     *<provider
     *    android:name="androidx.core.content.FileProvider"
     *    android:authorities="${applicationId}.fileprovider"
     *    android:grantUriPermissions="true"
     *    android:exported="false" >
     *    <meta-data
     *        android:name="android.support.FILE_PROVIDER_PATHS"
     *        android:resource="@xml/file_path_provider" />
     *</provider>
     * ```
     */
    fun openAPK(context: Context, apkPath:String){
        if(apkPath.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW)
        Log.d(TAG, "openAPK: get packageNmae ${context.packageName}")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(apkPath)
        )
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    /**
     * 上传文件到指定git路径
     * @param filePath 本地文件路径
     * @param newFilePath git文件路径
     * @param commitMsg git提交信息
     * @param uploadCallback 回调
     */
    fun uploadFile(filePath: String, newFilePath: String, commitMsg: String, uploadCallback: (String?)->Unit){
        val encodedPath = URLEncoder.encode(newFilePath, "utf-8")
        //base 64编码文件内容
        val file = File(filePath)
        if(!file.exists()){
            uploadCallback("文件不存在")
            return
        }
        val content = Base64.encodeToString(file.readBytes(), Base64.DEFAULT)
        if(content.isBlank()){
            uploadCallback("文件内容为空")
            return
        }
        server.post(
            "api/v5/repos/${repoOwner}/${repoName}/contents/$encodedPath",
            "{\"access_token\":\"${accessToken}\",\"content\": \"$content\", \"message\": \"$commitMsg\"}", String::class.java
        ) { res, errMsg ->
            Log.d(TAG, "uploadFile: response is $res err: $errMsg")
            errMsg?.let{
                uploadCallback(errMsg)
            }?:run{
                uploadCallback(res)
            }
        }
    }

    /**
     * 格式化文件大小
     * @param size 文件大小
     * @param scale 保留小数位数，默认2位
     * @param roundingMode 小数取舍模式，默认四舍五入
     * @return 格式化后的文件大小表示
     */
    fun formatSize(size: Long, scale: Int=2, roundingMode: RoundingMode=RoundingMode.HALF_UP): String{
        return if(size < 1024){
            "${size}B"
        }else if(size < 1024*1024){
            BigDecimal(size.toDouble()/1024).setScale(2, roundingMode).toString() + "KB"
        }else if(size < 1024*1024*1024){
            BigDecimal(size.toDouble()/1024/1024).setScale(2, roundingMode).toString() + "MB"
        }else{
            BigDecimal(size.toDouble()/1024/1024/1024).setScale(2, roundingMode).toString() + "GB"
        }
    }

    data class ResponseInfo(
        val timestamp: String,
        val status: Int,
        val type: String,
        val encoding: String,
        val size: String,
        val name: String,
        val path: String,
        val content: String,
        val sha: String,
        val url: String,
        val html_url: String,
        val download_url: String,
        val _links: ContentsInfoLinks,
        val error_msg: String,
        val error_code: String,
        val request_id: String,
        val message: String
    )
    data class ContentsInfoLinks(val self: String, val html: String)

    /**
     * App目录
     * @sample categoryFileSample
     */
    data class AppCategory(
        /**
         * 目录版本
         */
        val version: String,
        /**
         * 目录标题
         */
        val title: String,
        /**
         * app目录列表
         */
        val apps: List<AppInfo>
    )

    data class AppInfo(
        /**
         * app 标记
         */
        val name: String,
        /**
         * app version file路径
         */
        val versionFile: String
    )

    /**
     * App版本信息
     * @sample versionFileSample
     */
    data class AppVersionsInfo(
        val appId: String,
        val appName: String,
        val latestVersion: String,
        val testVersion: String,
        val description: String,
        val mark: String,
        val versions: List<VersionInfo>,
    )
    data class VersionInfo(
        val appName: String,
        val version: String,
        val versionCode: Int,
        val description: String,
        val mark: String,
        val iconUrl: String,
        val detailUrl: String,
        val downloadUrl: String,
        val pictureUrls: String,
        val publishTime: String,
        val publisher: PublisherInfo,
        val error_msg: String=""
    )
    data class PublisherInfo(
        val name: String,
        val timestamp: String,
        val contact: String,
        val mark: String,
    )
    data class DownloadStatus(
        val msg: String = "",
        val errMsg: String? = null,
        val savePath: String = "",
        val progress: Int=0,
        val total: Int=0,
        val current: Int=0,
    )
    /**
     * 版本控制的json文件示例，所有版本信息从此文件读取
     * ```
     * {
     *  "appId": "com.jeady.test",
     *  "latestVersion": "1.0.1",
     *  "appName": "廸哥的版本测试程序",
     *  "description": "这只是一个测试程序，仅供测试者测试使用，没有实际用处",
     *  "testVersion": "1.0.1",
     *  "mark": "release",
     *  "versions": [
     *      {
     *          "appName": "版本测试程序",
     *          "version": "1.0.1",
     *          "versionCode": 1,
     *          "description": "新增了版本测试内容",
     *          "mark": "release",
     *          "iconUrl": "",
     *          "detailUrl": "",
     *          "downloadUrl": "timetodo/v1.0.1.apk",
     *          "pictureUrls": "",
     *          "publishTime": "2024/08/09 14:32:55",
     *          "publisher": {
     *              "name": "jeady",
     *              "avatorUrl": "",
     *              "timestamp": "",
     *              "contact": "",
     *              "mark": ""
     *          }
     *      },
     *      {
     *          "appLabel": "版本测试程序",
     *          "version": "1.0.0",
     *          "versionCode": 0,
     *          "description": "test版本",
     *          "mark": "",
     *          "iconUrl": "",
     *          "detailUrl": "",
     *          "downloadUrl": "test/v1.0.0.apk",
     *          "pictureUrls": "",
     *          "publisher": {
     *              "name": "jeady",
     *              "avatorUrl": "",
     *              "timestamp": "",
     *              "contact": "",
     *              "mark": ""
     *          }
     *      }
     *  ]
     * }
     * ```
    */
    private val versionFileSample = """
{
 "appId": "com.jeady.test",
 "latestVersion": "1.0.1",
 "appName": "廸哥的版本测试程序",
 "description": "这只是一个测试程序，仅供测试者测试使用，没有实际用处",
 "testVersion": "1.0.1",
 "mark": "release",
 "versions": [
     {
         "appName": "版本测试程序",
         "version": "1.0.1",
         "versionCode": 1,
         "description": "新增了版本测试内容",
         "mark": "release",
         "iconUrl": "",
         "detailUrl": "",
         "downloadUrl": "timetodo/v1.0.1.apk",
         "pictureUrls": "",
         "publishTime": "2024/08/09 14:32:55",
         "publisher": {
             "name": "jeady",
             "avatorUrl": "",
             "timestamp": "",
             "contact": "",
             "mark": ""
         }
     },
     {
         "appLabel": "版本测试程序",
         "version": "1.0.0",
         "versionCode": 0,
         "description": "test版本",
         "mark": "",
         "iconUrl": "",
         "detailUrl": "",
         "downloadUrl": "test/v1.0.0.apk",
         "pictureUrls": "",
         "publisher": {
             "name": "jeady",
             "avatorUrl": "",
             "timestamp": "",
             "contact": "",
             "mark": ""
         }
     }
 ]
}
"""
    private val categoryFileSample = """
{
 "version": "1.0",
 "title": "版本测试",
 "apps": [
     {
         "name": "版本测试",
         "versionFile": "test/versions.json"
     }
 ]
}
"""
}
