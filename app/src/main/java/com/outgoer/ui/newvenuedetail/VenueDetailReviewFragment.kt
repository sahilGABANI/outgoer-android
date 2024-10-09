package com.outgoer.ui.newvenuedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentVenueDetailReviewBinding
import com.outgoer.ui.newvenuedetail.view.VenueDetailReviewAdapter
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewModel
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewState
import timber.log.Timber
import javax.inject.Inject

class VenueDetailReviewFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = VenueDetailReviewFragment()

        private const val VENUE_DETAILS = "venueDetail"

        @JvmStatic
        fun newInstanceWithData(venueDetail: VenueDetail): VenueDetailReviewFragment {
            val venueDetailReviewFragment = VenueDetailReviewFragment()
            val bundle = Bundle()
            bundle.putParcelable(VENUE_DETAILS, venueDetail)
            venueDetailReviewFragment.arguments = bundle
            return venueDetailReviewFragment
        }
    }

    private var _binding: FragmentVenueDetailReviewBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueDetailViewModel>
    private lateinit var venueDetailViewModel: VenueDetailViewModel
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var venueDetailReviewAdapter: VenueDetailReviewAdapter
    private var venueId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        venueDetailViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVenueDetailReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        listenToViewModel()
        binding.fiveAppCompatSeekBar.isEnabled = false
        binding.fourAppCompatSeekBar.isEnabled = false
        binding.threeAppCompatSeekBar.isEnabled = false
        binding.twoAppCompatSeekBar.isEnabled = false
        binding.oneAppCompatSeekBar.isEnabled = false
    }

    private fun listenToViewModel() {
        venueDetailViewModel.venueDetailState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueDetailViewState.ErrorMessage -> {
//                    showToast(it.errorMessage)
//                    binding.noReviewAppCompatTextView.visibility = View.VISIBLE
//                    binding.ratingLinearLayout.visibility = View.GONE
                }
                is VenueDetailViewState.VenueReview -> {
                    if (it.listofvenue.size > 0) {
//                        binding.noReviewAppCompatTextView.visibility = View.GONE
//                        binding.ratingLinearLayout.visibility = View.VISIBLE
                        venueDetailReviewAdapter.listOfReviews = it.listofvenue

                    } else {
//                        binding.noReviewAppCompatTextView.visibility = View.VISIBLE
//                        binding.ratingLinearLayout.visibility = View.GONE
                    }
                }
                is VenueDetailViewState.VenueReviewGroupCount -> {
                    if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id?.equals(venueId) == false) {
                        binding.addReviewMaterialButton.visibility =
                            if (it.review) View.GONE else View.VISIBLE
                    } else {
                        binding.addReviewMaterialButton.visibility = View.GONE
                    }

                    binding.fiveAppCompatSeekBar.progress =
                        if (it.listofreview[4].ratingCount > 0) ((it.listofreview[4].ratingCount.toFloat() / it.totalReviews.toFloat()) * 100).toInt() else it.listofreview[4].ratingCount
                    binding.fourAppCompatSeekBar.progress =
                        if (it.listofreview[3].ratingCount > 0) ((it.listofreview[3].ratingCount.toFloat() / it.totalReviews.toFloat()) * 100).toInt() else it.listofreview[3].ratingCount
                    binding.threeAppCompatSeekBar.progress =
                        if (it.listofreview[2].ratingCount > 0) ((it.listofreview[2].ratingCount.toFloat() / it.totalReviews.toFloat()) * 100).toInt() else it.listofreview[2].ratingCount
                    binding.twoAppCompatSeekBar.progress =
                        if (it.listofreview[1].ratingCount > 0) ((it.listofreview[1].ratingCount.toFloat() / it.totalReviews.toFloat()) * 100).toInt() else it.listofreview[1].ratingCount
                    binding.oneAppCompatSeekBar.progress =
                        if (it.listofreview[0].ratingCount > 0) ((it.listofreview[0].ratingCount.toFloat() / it.totalReviews.toFloat()) * 100).toInt() else it.listofreview[0].ratingCount
                }
                is VenueDetailViewState.VenueReviewCount -> {
                    binding.tvVenueRatingCount.text = String.format("%.2f", it.reviewAvg)
                    binding.venueRatingBar.rating = String.format("%.2f", it.reviewAvg).toFloat()
                    binding.totalAppCompatTextView.text = "(${it.totalReviews})"
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun initUI() {
        arguments?.let {
            val venue = it.getParcelable<VenueDetail>(VENUE_DETAILS)
            venue?.let { v ->
                venueId = v.id
                Timber.tag("venueId").i("$venueId")
                venueDetailViewModel.getReviews(venueId)
            }
            binding.addReviewMaterialButton.visibility = if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id?.equals(venue?.id) == true) View.GONE else View.VISIBLE
        }
        venueDetailReviewAdapter = VenueDetailReviewAdapter(requireContext()).apply {}

        binding.reviewsRecyclerView.apply {
            adapter = venueDetailReviewAdapter
        }

        binding.addReviewMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            Timber.tag("venueId").i("$venueId")
            startActivity(VenueReviewActivity.getIntent(requireContext(), venueId))
        }
    }
}