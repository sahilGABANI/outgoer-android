package com.outgoer.ui.story.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.EmojiViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EmojiView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val emojiActionStateSubject: PublishSubject<Int> = PublishSubject.create()
    val emojiActionState: Observable<Int> = emojiActionStateSubject.hide()

    private var binding: EmojiViewBinding? = null
    private var emoji: Int = 0

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.emoji_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = EmojiViewBinding.bind(view)

        binding?.apply {
            emojiAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                emojiActionStateSubject.onNext(emoji)
            }
        }
    }

    fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }
    fun bind(emojiInfo: Int) {
        this.emoji = emojiInfo
        binding?.apply {
            emojiAppCompatTextView.setText(getEmojiByUnicode(emoji))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}