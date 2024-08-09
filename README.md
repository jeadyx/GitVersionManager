Free Git Version Manager
---
A dependency for manage your app version.  

You can upload and download executable file across the dependency.

# Simple to use
0. add dependency  
```kotlin
implementation("io.github.jeadyx.compose:gitVersionManager:1.4")
```
`sync` project  

1. init  
```kotlin
val gitManager = GitManager("ownerName", "repoName", "accessToken")
// GitManager("jeady", "publisher", "af19696ba36ab23ef32268441d79")
```

2. get latest version  
```kotlin
gitManager.getLatestServerVersion("test/versions.json") {
    // it.version
}
```

3. get all versions  
```kotlin
gitManager.getVersions("test/versions.json") {
    // all version info
}
```

4. download file  
```kotlin
gitManager.downloadFile(v.downloadUrl, downloadPath) {
    // it.progress
}
```

5. open apk (if is apk file)  
```kotlin
gitManager.openAPK(context, downloadPath)
```

6. upload file  
```kotlin
gitManager.uploadFile(
    filePath, gitFilePath,
    "commit message"
) {
    // success if it contains string `download_url`
    if(it?.contains("download_url")?:false){
        // success
    }else{
        // fail
    }
}
```

# Sample
[Module app](app)

# Donate
![donate.png](imgs/donate.png)  
