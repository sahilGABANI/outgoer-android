package com.outgoer.ui.createevent

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.VenueLocationBottomSheetBinding
import com.outgoer.ui.createevent.view.NearVenueAdapter
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventViewState
import com.outgoer.utils.UiUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VenueLocationBottomSheet: BaseBottomSheetDialogFragment()  {
    private var _binding: VenueLocationBottomSheetBinding? = null
    private val binding get() = _binding!!


    private val venueClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueClick: Observable<VenueMapInfo> = venueClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    private lateinit var nearVenueAdapter: NearVenueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        createEventsViewModel = getViewModelFromFactory(viewModelFactory)
        createEventsViewModel.resetPagination()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = VenueLocationBottomSheetBinding.inflate(inflater, container, false)
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
        initAdapter()
        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
            }.autoDispose()

        binding.etSearch.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.isNotEmpty()) {
                    createEventsViewModel.searchVenueList(it.toString())
                } else if (it.isEmpty()) {
                    createEventsViewModel.resetPagination()
                }
                UiUtils.hideKeyboard(requireContext())
            }, {
                Timber.e(it)
            }).autoDispose()
        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun initAdapter() {
        nearVenueAdapter = NearVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                venueClickSubject.onNext(it)
                dismissBottomSheet()
            }.autoDispose()
        }
        binding.rvNearVenueList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvNearVenueList.apply {
            adapter = nearVenueAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (binding.etSearch.text.toString().isNullOrEmpty())
                                    createEventsViewModel.loadMore()
                                else createEventsViewModel.loadMoreVenueList()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun listenToViewModel() {
        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.LoadingState -> {

                }
                is EventViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is EventViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is EventViewState.EventDetails -> {

                }
                is EventViewState.VenueMapList -> {
                    nearVenueAdapter.listOfDataItems = it.event
                }
                is EventViewState.VenueInfoList -> {
                    nearVenueAdapter.listOfDataItems = it.listOfVenueInfo
                }
                else -> {

                }
            }
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}