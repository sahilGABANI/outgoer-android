package com.outgoer.videoplayer

interface VideoPlayCallBack {
    fun onVideoPlay(challengeId: Int)
}

interface VideoDoubleClick {
    fun onDoubleClick()
    fun onSingleClick()
}