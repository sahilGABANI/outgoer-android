package com.outgoer.ui.venue.view

import android.app.TimePickerDialog
import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.api.venue.model.VenueTimeSelectionClickState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueAvailabilityTimeBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*

class VenueAvailabilityTimeView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueAvailableClickSubject: PublishSubject<VenueTimeSelectionClickState> = PublishSubject.create()
    val venueAvailableClick: Observable<VenueTimeSelectionClickState> = venueAvailableClickSubject.hide()

    private lateinit var binding: ViewVenueAvailabilityTimeBinding

    private lateinit var venueTimeInfo: VenueAvailabilityRequest
    lateinit var mTimePicker: TimePickerDialog

    val mcurrentTime = Calendar.getInstance()
    var lstHour = mcurrentTime.get(Calendar.HOUR_OF_DAY)
    var lstMinute = mcurrentTime.get(Calendar.MINUTE)

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_availability_time, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueAvailabilityTimeBinding.bind(view)

        binding.apply {
            ivRemove.throttleClicks().subscribeAndObserveOnMainThread {
                venueAvailableClickSubject.onNext(VenueTimeSelectionClickState.RemoveClicks(venueTimeInfo))
            }.autoDispose()
        }
    }

    fun bind(venueTimeInfo: VenueAvailabilityRequest) {
        this.venueTimeInfo = venueTimeInfo
        binding.apply {

            opensAtAppCompatEditText.throttleClicks().subscribeAndObserveOnMainThread {
                timePicker(opensAtAppCompatEditText)
                mTimePicker.show()
            }.autoDispose()

            closesAppCompatEditText.throttleClicks().subscribeAndObserveOnMainThread {
                closeTimePicker(closesAppCompatEditText)
                mTimePicker.show()
            }.autoDispose()

            opensAtAppCompatEditText.text = venueTimeInfo.openAt?.first()
            closesAppCompatEditText.text = venueTimeInfo.closeAt?.first()
            ivRemove.isVisible = venueTimeInfo.id != 0
        }
    }

    private fun timePicker(timeTextView: AppCompatTextView) {
        mTimePicker = TimePickerDialog(context,
            { view, hourOfDay, minute ->
                val time = Time(hourOfDay, minute, 0)
                lstHour = hourOfDay + 12
                lstMinute = minute
                val simpleDateFormat = SimpleDateFormat("h:mma")
                val s: String = simpleDateFormat.format(time)
                timeTextView.text = s
                venueTimeInfo.openAt?.clear()
                venueTimeInfo.openAt?.add(timeTextView.text.toString())
                venueAvailableClickSubject.onNext(VenueTimeSelectionClickState.OpensAtClick(venueTimeInfo))

            }, 8, 0, false)
    }

    private fun closeTimePicker(timeTextView: AppCompatTextView) {
        mTimePicker = TimePickerDialog(context,
            { view, hourOfDay, minute ->
                val time = Time(hourOfDay, minute, 0)
                val simpleDateFormat = SimpleDateFormat("h:mma")
                val s: String = simpleDateFormat.format(time)
                timeTextView.text = s
                venueTimeInfo.closeAt?.clear()
                venueTimeInfo.closeAt?.add(timeTextView.text.toString())
                venueAvailableClickSubject.onNext(VenueTimeSelectionClickState.CloseAtClicks(venueTimeInfo))
            }, lstHour, lstMinute, false)
    }
}