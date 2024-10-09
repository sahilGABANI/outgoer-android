package com.outgoer.ui.postlocation.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PlaceLocationItemBinding
import com.outgoer.ui.sponty.location.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlacesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val placesActionStateSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val placesActionState: Observable<ResultResponse> = placesActionStateSubject.hide()

    private var binding: PlaceLocationItemBinding? = null
    private lateinit var resultResponse: ResultResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.place_location_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = PlaceLocationItemBinding.bind(view)

        binding?.apply {
            userLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                placesActionStateSubject.onNext(resultResponse)
            }
        }
    }

    fun bind(result: ResultResponse) {
        resultResponse = result
        binding?.apply {
            usernameAppCompatTextView.text = result.name
            addressAppCompatTextView.text = result.formattedAddress
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}