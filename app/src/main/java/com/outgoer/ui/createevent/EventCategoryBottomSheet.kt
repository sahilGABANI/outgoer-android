package com.outgoer.ui.createevent

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.outgoer.R
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.EventCategoryBottomSheetBinding
import com.outgoer.ui.createevent.view.EventCategoryAdapter
import com.outgoer.ui.createevent.viewmodel.EventCategoryViewModel
import com.outgoer.ui.createevent.viewmodel.EventCategoryViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


class EventCategoryBottomSheet: BaseBottomSheetDialogFragment()  {
    private var _binding: EventCategoryBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun getIntent() = EventCategoryBottomSheet()
    }

    private val venueClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueClick: Observable<VenueCategory> = venueClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<EventCategoryViewModel>
    private lateinit var eventCategoryViewModel: EventCategoryViewModel

    private lateinit var eventCategoryAdapter: EventCategoryAdapter

    private var listItems: ArrayList<VenueCategory> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        eventCategoryViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = EventCategoryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
        listenToViewModel()

    }

    private fun listenToViewEvents() {
        eventCategoryViewModel.getAllEventCategory()
        initAdapter()
    }

    private fun initAdapter() {
        eventCategoryAdapter = EventCategoryAdapter(requireContext()).apply {
            eventCategoryActionState.subscribeAndObserveOnMainThread {
                var index = listItems.indexOf(it)

                listItems.get(index).isSelected = !listItems.get(index).isSelected
                eventCategoryAdapter.listOfCategory = listItems

                object : CountDownTimer(3000, 1000) {
                    override fun onFinish() {
                        venueClickSubject.onNext(it)
                        dismissBottomSheet()
                    }

                    override fun onTick(millisUntilFinished: Long) {}
                }.start()
            }.autoDispose()
        }

        binding.categoryRecyclerView.apply {
            adapter = eventCategoryAdapter
            layoutManager = GridLayoutManager(context, 3)
        }
    }

    private fun listenToViewModel() {
        eventCategoryViewModel.eventsCategoryViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventCategoryViewState.LoadingState -> {

                }
                is EventCategoryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is EventCategoryViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is EventCategoryViewState.VenueMapList -> {
                    listItems = it.event
                    eventCategoryAdapter.listOfCategory = listItems
                }
            }
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}