package com.example.telegram.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File

fun createAudioFilePath(context: Context): String {
    val audioDir = context.cacheDir
    val audioFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
    return audioFile.absolutePath
}

fun Context.hasAudioPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}