package com.example.telegram.data.model

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


@Composable
fun VoiceMessagePlayer(audioUrl: String) {
    var isPlaying by remember { mutableStateOf(false) }
    LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(onClick = {
            if (isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(audioUrl)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnPreparedListener {
                        it.start()
                        isPlaying = true
                    }
                    mediaPlayer.setOnCompletionListener {
                        isPlaying = false
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        Text(text = if (isPlaying) "Playing..." else "Play voice message")
    }
}

