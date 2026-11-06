package com.carrot233.travelmemory.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

fun newCaptureUri(context: Context, extension: String): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(directory, "${System.currentTimeMillis()}.$extension")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun rememberPermissionedAction(
    permission: String = Manifest.permission.CAMERA,
    onDenied: () -> Unit,
    onGranted: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val grantedCallback = rememberUpdatedState(onGranted)
    val deniedCallback = rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) grantedCallback.value() else deniedCallback.value()
    }
    return {
        val already = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (already) grantedCallback.value() else launcher.launch(permission)
    }
}
