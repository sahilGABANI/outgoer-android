package com.outgoer.ui.sponty.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.sponty.model.SpontyJoins
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SpontyUserViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val spontyUserActionStateSubject: PublishSubject<SpontyJoins> = PublishSubject.create()
    val spontyUserActionState: Observable<SpontyJoins> = spontyUserActionStateSubject.hide()

    private var binding: SpontyUserViewBinding? = null
    private lateinit var spontyJoins: SpontyJoins

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.sponty_user_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SpontyUserViewBinding.bind(view)

        binding?.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                spontyUserActionStateSubject.onNext(spontyJoins)
            }
        }
    }

    fun bind(joins: SpontyJoins) {
        this.spontyJoins = joins
        binding?.apply {

            usernameAppCompatTextView.text = joins.username

            Glide.with(context)
                .load(joins.avatar)
                .centerCrop()
                .placeholder(
                    resources.getDrawable(
                        R.drawable.ic_chat_user_placeholder,
                        null
                    )
                )
                .into(ivProfile)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}