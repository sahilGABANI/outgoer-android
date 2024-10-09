package com.outgoer.ui.venue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueCategoryBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import com.outgoer.ui.venue.view.VenueCategoryAdapter
import javax.inject.Inject

class VenueCategoryActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueCategoryBinding

    private lateinit var venueCategoryAdapter: VenueCategoryAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MapVenueViewModel>
    private lateinit var mapVenueViewModel: MapVenueViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var venuecategorylist: ArrayList<VenueCategory> = arrayListOf()
    private var registerVenueRequest: RegisterVenueRequest? = null

    companion object {
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueCategoryActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapVenueViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
    }

    private fun initUI() {
        intent?.let {
            registerVenueRequest = it.getParcelableExtra<RegisterVenueRequest>(VenueInfoActivity.INTENT_REGISTER_VENUE)
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
           onBackPressed()
        }

        mapVenueViewModel.getVenueCategoryList()

        venueCategoryAdapter = VenueCategoryAdapter(this@VenueCategoryActivity).apply {
            venueCategoryClick.subscribeAndObserveOnMainThread { item ->
                venuecategorylist.find { it.id == item.id }?.apply {
                    isSelected = !isSelected
                }

                venueCategoryAdapter.listOfDataItems = venuecategorylist
            }
        }

        binding.categoryRecyclerView.apply {
            adapter = venueCategoryAdapter
            layoutManager = GridLayoutManager(this@VenueCategoryActivity, 3)
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            var listofvenue = arrayListOf<Int>()
            venuecategorylist.filter { it.isSelected == true }?.forEach {
                listofvenue.add(it.id)
            }

            registerVenueRequest = loggedInUserCache.getVenueRequest()

            if(listofvenue.size > 0) {
                registerVenueRequest?.venueCategory = listofvenue.joinToString(",")
                registerVenueRequest?.let {
                    loggedInUserCache.setVenueRequest(it)

                    startActivity(VenueInfoActivity.getIntent(this@VenueCategoryActivity, it,))
                }
            } else {
                showToast(resources.getString(R.string.error_v_venue_category))
            }
        }
    }

    private fun listenToViewModel() {
        mapVenueViewModel.venueCategoryState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueCategoryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueCategoryViewState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is VenueCategoryViewState.VenueCategoryList -> {
                    venuecategorylist = it.venueCategoryList as ArrayList<VenueCategory>

                    var category = loggedInUserCache.getVenueRequest()

                    var selectionArray = category?.venueCategory?.split(",")

                    selectionArray?.forEach {
                        venuecategorylist.find { it1 -> it1.id == it.toInt() }?.isSelected = true
                    }

                    venueCategoryAdapter.listOfDataItems = venuecategorylist

                }
                else -> {

                }
            }
        }.autoDispose()
    }
}