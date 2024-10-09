package com.outgoer.ui.sponty.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SpontyLocationItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyLocationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val spontyLocationActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val spontyLocationActionState: Observable<String> = spontyLocationActionStateSubject.hide()

    private var binding: SpontyLocationItemBinding? = null
    private lateinit var locationInfo: String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.sponty_location_item, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = SpontyLocationItemBinding.bind(view)

        binding?.apply {
            locationMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                spontyLocationActionStateSubject.onNext(locationInfo)
            }
        }
    }

    fun bind(location: String) {
        this.locationInfo = location
        binding?.apply {
            locationAppCompatTextView.text = location
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}