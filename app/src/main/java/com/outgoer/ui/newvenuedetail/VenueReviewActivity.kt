package com.outgoer.ui.newvenuedetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.venue.model.AddReviewRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueReviewBinding
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.createevent.view.EventAdsAdapter
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewModel
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewState
import javax.inject.Inject

class VenueReviewActivity : BaseActivity() {

    private lateinit var eventAdsAdapter: EventAdsAdapter
    private var venueId: Int = 0

    companion object {
        private val REQUEST_CODE_EVENT = 1890
        private val INTENT_VENUE_ID = "INTENT_VENUE_ID"

        fun getIntent(context: Context, venueId: Int): Intent {
            val intent = Intent(context, VenueReviewActivity::class.java)
            intent.putExtra(INTENT_VENUE_ID, venueId)

            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueDetailViewModel>
    private lateinit var venueDetailViewModel: VenueDetailViewModel

    private lateinit var binding: ActivityVenueReviewBinding
    private var listofreview: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        venueDetailViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(binding.reviewTextAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_please_enter_review))
            } else {
                venueDetailViewModel.addReviews(AddReviewRequest(venueId, binding.reviewTextAppCompatEditText.text.toString(), binding.venueRatingBar.rating.toDouble(), listofreview))
            }

        }
    }

    private fun listenToViewModel() {
        venueDetailViewModel.venueDetailState.subscribeAndObserveOnMainThread {
            when(it) {
                is VenueDetailViewState.AddReviewSuccessMessage -> {
                    finish()
                    showToast(it.successMessage)
                }
                is VenueDetailViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueDetailViewState.LoadingState -> {
                    binding.progressBar.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                    binding.continueMaterialButton.visibility = if(it.isLoading) View.GONE else View.VISIBLE
                }

                else -> {}
            }
        }
    }

    private fun initUI() {
        intent?.let {
            venueId = it.getIntExtra(INTENT_VENUE_ID, 0)
        }


        binding.photoPickerAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermission("POST_TYPE_IMAGE")
        }

        eventAdsAdapter = EventAdsAdapter(this@VenueReviewActivity)

        binding.photosRecyclerView.apply {
            adapter = eventAdsAdapter
        }

        eventAdsAdapter.isVenue = true
    }

    private fun checkPermission(type: String) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO
                )
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        startActivityForResult(
                            AddMediaEventActivity.getIntentWithData(this@VenueReviewActivity, type),
                            REQUEST_CODE_EVENT
                        )
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_EVENT) {
                data?.let {
                    listofreview.addAll(it.getStringArrayListExtra("MEDIA_URL") ?: arrayListOf())
                    eventAdsAdapter.listOfMedia = listofreview
                }
            }
        }
    }
}