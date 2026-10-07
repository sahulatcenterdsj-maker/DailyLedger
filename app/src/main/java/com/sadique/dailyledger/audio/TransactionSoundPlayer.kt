package com.sadique.dailyledger.audio

import android.content.Context
import android.media.MediaPlayer
import com.sadique.dailyledger.R

object TransactionSoundPlayer {
    fun play(context: Context, type: String) {
        val sound = if (type.equals("INCOME", ignoreCase = true)) R.raw.retro_income else R.raw.retro_expense
        playResource(context, sound)
    }

    fun playBatchSuccess(context: Context) {
        playResource(context, R.raw.retro_success)
    }

    private fun playResource(context: Context, resource: Int) {
        runCatching {
            MediaPlayer.create(context.applicationContext, resource)?.apply {
                setVolume(0.45f, 0.45f)
                setOnCompletionListener { player -> player.release() }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                start()
            }
        }
    }
}
