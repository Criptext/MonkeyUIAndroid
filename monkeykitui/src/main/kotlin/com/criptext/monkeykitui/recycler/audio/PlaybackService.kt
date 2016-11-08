package com.criptext.monkeykitui.recycler.audio

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.criptext.monkeykitui.recycler.MonkeyItem

/**
 * Created by gesuwall on 10/27/16.
 */

class PlaybackService: Service()  {

    //media player
    private val player : DefaultVoiceNotePlayer by lazy {
        val newPlayer = DefaultVoiceNotePlayer(this)
        newPlayer.initPlayer()

        newPlayer
    }

    private var sensorHandler: SensorHandler? = null
    private val binder  = VoiceNoteBinder()


    override fun onBind(p0: Intent?): IBinder? {
        //Log.d("MusicService", "bind service ")
        player.onPlaybackStopped = null
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(!player.isPlayingAudio) {
            player.releasePlayer()
            stopSelf()
        } else {
            player.onPlaybackStopped = { it: MonkeyItem ->
                stopSelf()
            }
        }

        if(!(sensorHandler?.isProximityOn ?: false)) {
            sensorHandler?.onDestroy()
            sensorHandler = null
        }

        return true

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        isRunning = true

        if(intent.getBooleanExtra(togglePlayback, false)) {
            if(player.isPlayingAudio)
                player.onPauseButtonClicked()
            else
                player.onPlayButtonClicked(player.currentlyPlayingItem!!.item)
        } else
            player.initPlayer();

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    inner class VoiceNoteBinder : Binder() {
        fun getVoiceNotePlayer(act: Activity?) : DefaultVoiceNotePlayer {
            if(sensorHandler == null && act != null) {
                sensorHandler = SensorHandler(player, act)
            }
            return player;
        }
    }

    companion object {
        var isRunning: Boolean = false
        val togglePlayback = "MonkeyKitUI.PlaybackService.togglePlayback"
    }
}

