package io.github.jeadyx.gitversionmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Button1
import androidx.compose.ui.input.key.Key.Companion.Button2
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private var displayDlg1 by mutableStateOf(false)
private var dlg1Content by mutableStateOf("hello dialog")
private val dlg1ContentCache = mutableStateListOf<DialogActions>()
private var dlg1actions: DialogActions? = null
@Composable
fun Dialog1() {
    if(displayDlg1) {
        Dialog(onDismissRequest = { }) {
            Column(
                Modifier
                    .fillMaxWidth(0.6f)
            ) {
                val radius = 10.dp
                val scrollState = rememberScrollState()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(200.dp, 500.dp)
                        .verticalScroll(scrollState)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(topStart = radius, topEnd = radius)
                        )
                        .padding(10.dp), contentAlignment = Alignment.Center
                ){
                    dlg1actions?.content?.let {
                        it()
                    }?: run {
                        Text(dlg1Content)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dlg1actions?.cancelText?.ifBlank { null }?.let {
                        ButtonText(it) {
                            if (dlg1actions?.autoClose != false) closeDialog1()
                            dlg1actions?.onCancel?.invoke()
                        }
                    }
                    dlg1actions?.confirmText?.ifBlank { null }?.let {
                        ButtonText(it) {
                            if (dlg1actions?.autoClose != false) closeDialog1()
                            dlg1actions?.onConfirm?.invoke()
                        }
                    }
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    if(dlg1ContentCache.isNotEmpty()){
                        showDialog1(dlg1ContentCache.removeFirst())
                    }
                }
            }
        }
    }
}
fun showDialog1(title: String){
    val dialogActions = DialogActions(title)
    if(!displayDlg1) {
        dlg1Content = dialogActions.title ?:""
        dlg1actions = dialogActions
    }else{
        dlg1ContentCache.add(dialogActions)
    }
    displayDlg1 = true
}
fun showDialog1(dialogActions: DialogActions?=null){
    if(!displayDlg1) {
        dlg1Content = dialogActions?.title?:""
        dlg1actions = dialogActions
    }else{
        dialogActions?.let { dlg1ContentCache.add(it) }
    }
    displayDlg1 = true
}
fun closeDialog1(){
    displayDlg1 = false
}

data class DialogActions(
    val title: String,
    val content: @Composable (() -> Unit)?=null,
    val autoClose: Boolean=true,
    val confirmText: String="确定",
    val cancelText: String="",
    val onCancel: (()->Unit)? = null,
    val onConfirm: (() -> Unit)? = null
)