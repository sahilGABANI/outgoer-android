package com.outgoer.ui.music.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.MusicItemViewBinding
import com.outgoer.databinding.TagPeopleViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddMusicView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val musicClickSubject: PublishSubject<MusicResponse> = PublishSubject.create()
    val musicClick: Observable<MusicResponse> = musicClickSubject.hide()

    private val selectMusicClickSubject: PublishSubject<MusicResponse> = PublishSubject.create()
    val selectMusicClick: Observable<MusicResponse> = selectMusicClickSubject.hide()

    private lateinit var binding: MusicItemViewBinding
    private lateinit var musicResponse: MusicResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.music_item_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MusicItemViewBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                selectMusicClickSubject.onNext(musicResponse)
            }.autoDispose()

            playAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                musicResponse.isPlaying = !musicResponse.isPlaying
                if(musicResponse.isPlaying) {
                    playAppCompatImageView.setImageDrawable(resources.getDrawable(cn.jzvd.R.drawable.jz_pause_normal, null))
                } else {
                    playAppCompatImageView.setImageDrawable(resources.getDrawable(cn.jzvd.R.drawable.jz_play_normal, null))
                }
                musicClickSubject.onNext(musicResponse)
            }.autoDispose()
        }
    }

    fun bind(musicRes: MusicResponse) {
        this.musicResponse = musicRes
        binding.apply {
            progressProgressBar.setProgress(0)
            if(musicResponse.isPlaying) {
                playAppCompatImageView.setImageDrawable(resources.getDrawable(cn.jzvd.R.drawable.jz_pause_normal, null))
            } else {
                playAppCompatImageView.setImageDrawable(resources.getDrawable(cn.jzvd.R.drawable.jz_play_normal, null))
            }

            Glide.with(context)
                .load(musicResponse.songImage)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivProfile)


            musicTitleAppCompatTextView.text = musicResponse.songTitle
            singerNameAppCompatTextView.text = musicResponse.songSubtitle

            singerNameAppCompatTextView.visibility = if(musicResponse.songSubtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }
}