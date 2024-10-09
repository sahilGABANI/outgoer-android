package com.outgoer.ui.music.view

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri


class PlayAudioManager {
    private var mediaPlayer: MediaPlayer? = null


    constructor() {}

    @Throws(Exception::class)
    fun playAudio(context: Context?, url: String?) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, Uri.parse(url))
        }

        mediaPlayer?.let {
            it.setOnCompletionListener { killMediaPlayer() }
            it.start()
        }
    }

    fun killMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer?.pause()
            try {
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}