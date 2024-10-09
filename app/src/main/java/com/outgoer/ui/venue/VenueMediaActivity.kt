package com.outgoer.ui.venue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueMediaRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueMediaBinding
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.createevent.view.EventAdsAdapter
import com.outgoer.ui.login.bottomsheet.OtpVerificationBottomSheet
import com.outgoer.ui.login.bottomsheet.SignupBottomSheet
import com.outgoer.ui.suggested.SuggestedUsersActivity
import com.outgoer.ui.venue.viewmodel.CreateVenueViewModel
import java.io.File
import javax.inject.Inject

class VenueMediaActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueMediaBinding

    private lateinit var eventAdsAdapter: EventAdsAdapter
    private var listofmedia: ArrayList<String> = arrayListOf()
    private var registerVenueRequest: RegisterVenueRequest? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateVenueViewModel>
    private lateinit var createVenueViewModel: CreateVenueViewModel
    private var cloudFlareConfig: CloudFlareConfig? = null
    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""

    companion object {
        private val REQUEST_CODE_EVENT = 1890
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueMediaActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createVenueViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        initUI()
        listenToViewEvents()
    }

    private fun initUI() {
        intent?.let {
            registerVenueRequest = it.getParcelableExtra(VenueAvailabilityActivity.INTENT_REGISTER_VENUE)
        }

        handlePathOz = HandlePathOz(this, listener)
        createVenueViewModel.getCloudFlareConfig()

        binding.addLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermission("POST_TYPE_IMAGE")
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        eventAdsAdapter = EventAdsAdapter(this@VenueMediaActivity).apply {
            deleteActionState.subscribeAndObserveOnMainThread {
                listofmedia.remove(it)
                eventAdsAdapter.listOfMedia = listOfMedia
            }
        }

        val llm = GridLayoutManager(this@VenueMediaActivity, 3)

        binding.mediaRecyclerView.apply {
            adapter = eventAdsAdapter
            layoutManager = llm
        }

        eventAdsAdapter.isVenue = true
    }

    private fun listenToViewEvents() {
        registerVenueRequest = loggedInUserCache.getVenueRequest()

        registerVenueRequest?.let {
            var venueMedia = arrayListOf<String>()
            it.venueMedia?.forEach {
                venueMedia.add(it.media ?: "")
            }

            eventAdsAdapter.listOfMedia = venueMedia
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {

            var listofvenue: ArrayList<VenueMediaRequest> = arrayListOf()
            listofmedia?.forEach {
                listofvenue.add(VenueMediaRequest(it, 1))
            }
            registerVenueRequest?.venueMedia = listofvenue

            registerVenueRequest?.let {
                loggedInUserCache.setVenueRequest(it)

                createVenueViewModel.createGroup(it)
            }
        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    selectedImagePath = filePath
                }
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            createVenueViewModel.uploadImageToCloudFlare(this, cloudFlareConfig, File(selectedImagePath), loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        }
    }

    private fun listenToViewModel() {
        createVenueViewModel.groupState.subscribeAndObserveOnMainThread {
            when (it) {
                is CreateVenueViewModel.GroupViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is CreateVenueViewModel.GroupViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is CreateVenueViewModel.GroupViewState.UploadImageCloudFlareSuccess -> {
                    listofmedia.add(it.imageUrl)

                    var listofvenue: ArrayList<VenueMediaRequest> = arrayListOf()
                    listofmedia?.forEach {
                        listofvenue.add(VenueMediaRequest(it, 1))
                    }
                    registerVenueRequest?.venueMedia = listofvenue
//                    var requestInfo = loggedInUserCache.getVenueRequest()
//                    requestInfo?.venueMedia = listofvenue

                    loggedInUserCache.setVenueRequest(registerVenueRequest)

                    eventAdsAdapter.listOfMedia = listofmedia

                }
                is CreateVenueViewModel.GroupViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CreateVenueViewModel.GroupViewState.CreateVenueSuccess -> {
                    showToast(it.successMessage)
                    openVerificationBottomSheet(registerVenueRequest?.email ?: "")
                }
                is CreateVenueViewModel.GroupViewState.LoadingState -> {
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun openVerificationBottomSheet(email: String) {
        val otpVerificationBottomSheet = OtpVerificationBottomSheet.newInstance(email).apply {
            otpVerificationSuccessClick.subscribeAndObserveOnMainThread {
                dismissBottomSheet()
                startActivityWithFadeInAnimation(
                    SuggestedUsersActivity.getIntentWithData(
                        requireContext(),
                        MapVenueUserType.VENUE_OWNER.type
                    )
                )
            }.autoDispose()
        }

        otpVerificationBottomSheet.show(supportFragmentManager, SignupBottomSheet.TAG)
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
                            AddMediaEventActivity.getIntentWithData(this@VenueMediaActivity, type,3),
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
                listofmedia.addAll(data?.getStringArrayListExtra("MEDIA_URL") ?: arrayListOf())
                eventAdsAdapter.listOfMedia = listofmedia



                var listofvenue: ArrayList<VenueMediaRequest> = arrayListOf()
                listofmedia?.forEach {
                    listofvenue.add(VenueMediaRequest(it, 1))
                }
                registerVenueRequest?.venueMedia = listofvenue
//                    var requestInfo = loggedInUserCache.getVenueRequest()
//                    requestInfo?.venueMedia = listofvenue

                loggedInUserCache.setVenueRequest(registerVenueRequest)
            }
        }
    }
}