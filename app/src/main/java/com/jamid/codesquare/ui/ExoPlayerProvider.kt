package com.jamid.codesquare.ui

/*
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ExoPlayerProvider {

    const val TAG = "ExoPlayerProvider"

    private val exoPlayers = mutableMapOf<String, ExoPlayer>()
    private var lastVideoPlaybackPosition = -1L
    private var lastMediaItem: MediaItem? = null
    private var lastUrl: String? = null
    private val mediaMap = mutableMapOf<String, Long>()
    var isVolumeMuted = false

    fun initialize(context: Context) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        val exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()

        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

        exoPlayers["DEFAULT"] = exoPlayer
    }

    fun toggleVolumeState(tag: String = "DEFAULT") {
        isVolumeMuted = !isVolumeMuted
        if (isVolumeMuted) {
            exoPlayers[tag]?.volume = 0f
        } else {
            exoPlayers[tag]?.volume = 1f
        }
    }

    private fun seekTo(position: Long, tag: String = "DEFAULT") {
        exoPlayers[tag]?.seekTo(position)
    }

    fun currentlyPlayingPlayer(): ExoPlayer? {
        var player: ExoPlayer? = null
        for (p in exoPlayers) {
            if (p.value.isPlaying) {
                player = p.value
                break
            }
        }
        return player
    }

    fun pause(url: String? = null, tag: String = "DEFAULT") {
        Log.d(TAG, "pause: Pausing video.")
        val player = exoPlayers[tag]
        player?.apply {
            pause()
            if (url != null)
                mediaMap[url] = currentPosition
            playBackStateListener?.let { it1 -> removeListener(it1) }
        }

        val player1 = currentlyPlayingPlayer()
        player1?.pause()
    }

    fun play(url: String, tag: String = "DEFAULT", listener: Player.Listener? = null) {
        Log.d(TAG, "play: Playing video $url")
        setMediaItem(url)
        if (mediaMap.containsKey(url)) {
            Log.d(TAG, "play: Video was played before")
            mediaMap[url]?.let {
                seekTo(it)
            }
        }

        lastUrl = url
        exoPlayers[tag]?.playWhenReady = true
        listener?.let { exoPlayers[tag]?.addListener(it) }
    }

    private fun setMediaItem(url: String, tag: String = "DEFAULT") {
        if (lastUrl != url) {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()

            exoPlayers[tag]?.setMediaItem(mediaItem)
            exoPlayers[tag]?.prepare()
        } else {
            Log.d(TAG, "setMediaItem: Same video being played")
        }
    }

    fun provide(tag: String = "DEFAULT"): ExoPlayer? {
        return exoPlayers[tag]
    }

    private var playBackStateListener: Player.Listener? = null

    fun releasePlayer(tag: String = "DEFAULT") {
        exoPlayers[tag]?.let {
            lastVideoPlaybackPosition = it.currentPosition
            playBackStateListener?.let { it1 -> it.removeListener(it1) }
            it.release()
        }
        lastMediaItem = null
        exoPlayers.remove(tag)
    }

    fun setPlayBackStateListener(mListener: Player.Listener?, tag: String = "DEFAULT") {
        Log.d(TAG, "setPlayBackStateListener: Settings Listener")
        playBackStateListener = mListener
        playBackStateListener?.let { exoPlayers[tag]?.addListener(it) }
    }

}*/
