package com.outgoer.ui.home.profile.newprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.GetVenueFollowersRequest
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.api.venue.model.VenueViewClickState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentNewMyFavouriteVenueBinding
import com.outgoer.ui.home.profile.newprofile.view.NewMyFavouriteVenueAdapter
import com.outgoer.ui.home.profile.viewmodel.MyFavouriteVenueViewModel
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class NewMyFavouriteVenueFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = NewMyFavouriteVenueFragment()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MyFavouriteVenueViewModel>
    private lateinit var myFavouriteVenueViewModel: MyFavouriteVenueViewModel

    private var _binding: FragmentNewMyFavouriteVenueBinding? = null
    private val binding get() = _binding!!

    private lateinit var myFavouriteVenueAdapter: NewMyFavouriteVenueAdapter
    private var listOfVenueInfo: List<VenueListInfo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        myFavouriteVenueViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewMyFavouriteVenueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvent()
    }



    private fun listenToViewModel() {
        myFavouriteVenueViewModel.myFavouriteVenueListState.subscribeAndObserveOnMainThread {
            when (it) {
                is MyFavouriteVenueViewModel.MyFavouriteVenueViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("MyFavouriteVenueViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is MyFavouriteVenueViewModel.MyFavouriteVenueViewState.MyFavouriteVenueInfoList -> {
                    listOfVenueInfo = it.listOfVenueInfo
                    myFavouriteVenueAdapter.listOfDataItems = it.listOfVenueInfo
                    hideShowNoData(listOfVenueInfo)
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        myFavouriteVenueAdapter = NewMyFavouriteVenueAdapter(requireContext())
        myFavouriteVenueAdapter.venueViewClick.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is VenueViewClickState.VenueViewClick -> {
                    startActivityWithDefaultAnimation(
                        NewVenueDetailActivity.getIntent(requireContext(), state.venueListInfo.categoryId ?: 0, state.venueListInfo.id)
                    )
                }
                is VenueViewClickState.AddRemoveVenueFavClick -> {
                    val mPos = listOfVenueInfo.indexOfFirst { venueListInfo ->
                        venueListInfo.id == state.venueListInfo.id
                    }
                    if (mPos != -1) {
                        val tempList = listOfVenueInfo.toMutableList()
                        tempList.removeAt(mPos)
                        listOfVenueInfo = tempList
                        myFavouriteVenueAdapter.listOfDataItems = tempList
                        hideShowNoData(listOfVenueInfo)
                    }
                    myFavouriteVenueViewModel.addRemoveFavouriteVenue(state.venueListInfo.id)
                }
            }
        }.autoDispose()

        binding.rvMyFavouriteVenue.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = myFavouriteVenueAdapter
        }
    }

    private fun hideShowNoData(listOfVenueInfo: List<VenueListInfo>) {
        if (listOfVenueInfo.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        myFavouriteVenueViewModel.getVenueFollowersList(GetVenueFollowersRequest(loggedInUserCache.getUserId()))

    }

}