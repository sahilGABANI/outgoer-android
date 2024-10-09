package com.outgoer.ui.deepar.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.api.effects.model.EffectResponse
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.EffectItemListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EffectView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val effectItemClicksSubject: PublishSubject<EffectResponse> = PublishSubject.create()
    val effectItemClicks: Observable<EffectResponse> = effectItemClicksSubject.hide()

    private lateinit var binding: EffectItemListBinding
    private lateinit var effectResponse: EffectResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.effect_item_list, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = EffectItemListBinding.bind(view)


        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                effectItemClicksSubject.onNext(effectResponse)
            }.autoDispose()
        }
    }

    fun bind(effectRes: EffectResponse) {
        this.effectResponse = effectRes

        Glide.with(this)
            .load(resources.getDrawable(effectRes.effectImageName, null))
            .error(R.drawable.ic_new_empty_profile_image)
            .placeholder(R.drawable.ic_new_empty_profile_image)
            .into(binding.ivUserProfileImage)
    }

}