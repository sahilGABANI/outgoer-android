package com.outgoer.ui.vennue_list

import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueListBinding
import com.outgoer.ui.createevent.view.NearVenueAdapter
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventViewState
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VenueListActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueListBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    private lateinit var nearVenueAdapter: NearVenueAdapter

    companion object {
        const val INTENT_EXTRA_VENUE_ID = "INTENT_EXTRA_VENUE_ID"
        const val INTENT_EXTRA_VENUE_NAME = "INTENT_EXTRA_VENUE_NAME"

        fun getIntent(context: Context): Intent {
            return Intent(context, VenueListActivity::class.java)
        }
    }
    private var taggedVenueHashMap = HashMap<Int, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityVenueListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createEventsViewModel = getViewModelFromFactory(viewModelFactory)
        createEventsViewModel.resetPagination()

        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {
        initAdapter()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
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
            }, {
                Timber.e(it)
            }).autoDispose()
        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun initAdapter() {
        nearVenueAdapter = NearVenueAdapter(this).apply {
            venueClick.subscribeAndObserveOnMainThread {

                taggedVenueHashMap[it.id] = it.name

                setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_VENUE_NAME, taggedVenueHashMap).putExtra(
                    INTENT_EXTRA_VENUE_ID, it.id))
                finish()
            }.autoDispose()
        }
        binding.rvNearVenueList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
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
                    Timber.tag("ErrorMessage").e("EventViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(findViewById(R.id.content))
                    } else {
                        showToast(it.errorMessage)
                    }
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
}