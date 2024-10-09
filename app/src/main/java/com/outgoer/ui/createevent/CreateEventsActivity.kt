package com.outgoer.ui.createevent

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.loper7.date_time_picker.DateTimeConfig
import com.loper7.date_time_picker.dialog.CardDatePickerDialog
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.CreateEventResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityCreateEventsBinding
import com.outgoer.ui.createevent.view.EventAdsAdapter
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventViewState
import com.outgoer.ui.login.AddUsernameEmailSocialLoginBottomSheet
import com.outgoer.ui.postlocation.AddPostLocationActivity
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class CreateEventsActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateEventsBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var listofadsmedia: ArrayList<String> = arrayListOf()
    private lateinit var eventAdsAdapter: EventAdsAdapter

    private var listofmedia: ArrayList<String> = arrayListOf()
    private lateinit var mediaAdapter: EventAdsAdapter

    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()
    private var location: String? = null
    private var placeId: String? = "Online"
    private var venueId: Int? = 0
    private var isPrivate: Int = 0

    private var uid: String? = null
    private var categoryId: Int = -1
    private var venueDetail: VenueDetail? = null
    companion object {

        private val REQUEST_CODE = 1889
        private val REQUEST_CODE_EVENT = 1890
        private val REQUEST_CODE_LOCATION = 1891
        private val VENUE_DETAILS = "VENUE_DETAILS"

        fun getIntent(context: Context, venueDetail: VenueDetail?): Intent {
            var intent = Intent(context, CreateEventsActivity::class.java)
            intent.putExtra(VENUE_DETAILS, venueDetail)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityCreateEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createEventsViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewEvents()
        listenToViewModel()
    }

    @SuppressLint("ResourceAsColor")
    private fun listenToViewEvents() {
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType == MapVenueUserType.VENUE_OWNER.type) {
            binding.cardContainer.isVisible = true
            binding.locationAppCompatImageView.setImageResource(R.drawable.ic_edit_venue)
            binding.locationAppCompatImageView.setBackgroundColor(android.R.color.transparent)
            binding.tvPlaceName.text = venueDetail?.name
            binding.tvPickEventLocation.text = resources.getText(R.string.label_event_location)
            binding.tvPlaceDescription.text = loggedInUserCache.getLoggedInUser()?.loggedInUser?.venueAddress
            longitude = venueDetail?.longitude?.toDouble() ?: 0.0
            latitude = venueDetail?.latitude?.toDouble() ?: 0.0
            location = venueDetail?.venueAddress
            Glide.with(this@CreateEventsActivity).load(venueDetail?.avatar).placeholder(R.drawable.ic_chat_user_placeholder).into(binding.ivPlaceImage)
            binding.tvPlaceRatingCount.text = venueDetail?.reviewAvg?.toString()
//            binding.distanceAppCompatTextView.text =
//                0.0.toBigDecimal().setScale(1, RoundingMode.UP)?.toDouble().toString().plus(" miles")
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                binding.distanceAppCompatTextView.text = if (venueDetail?.distance != 0.00) {
                    venueDetail?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                } else {
                    "0 ".plus(getString(R.string.label_miles))
                }
            } else {
                binding.distanceAppCompatTextView.text = if (venueDetail?.distance != 0.00) {
                    venueDetail?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                } else {
                    "0 ".plus(getString(R.string.label_kms))
                }
            }
            venueId = venueDetail?.id
            placeId ="Offline"
        }
        eventAdsAdapter = EventAdsAdapter(this@CreateEventsActivity).apply {
            addMediaActionState.subscribeAndObserveOnMainThread {
                checkPermissions("POST_TYPE_VIDEO")
            }

            deleteActionState.subscribeAndObserveOnMainThread {
                listofadsmedia.clear()
                eventAdsAdapter.listOfMedia = null
            }

            mediaActionState.subscribeAndObserveOnMainThread {}
        }

        binding.adsVideoRecyclerView.apply {
            adapter = eventAdsAdapter
        }

        eventAdsAdapter.isAds = true

        binding.locationAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheetFragment = VenueLocationBottomSheet().apply {
                venueClick.subscribeAndObserveOnMainThread {
                    binding.cardContainer.isVisible = true
                    binding.locationAppCompatImageView.setImageResource(R.drawable.ic_edit_venue)
                    binding.locationAppCompatImageView.setBackgroundColor(android.R.color.transparent)
                    binding.tvPlaceName.text = it.name
                    binding.tvPickEventLocation.text = resources.getText(R.string.label_event_location)
                    binding.tvPlaceDescription.text = it.venueAddress
                    longitude = it.longitude?.toDouble() ?: 0.0
                    latitude = it.latitude?.toDouble() ?: 0.0
                    location = it.venueAddress
                    Glide.with(this@CreateEventsActivity).load(it.avatar).placeholder(R.drawable.ic_chat_user_placeholder).into(binding.ivPlaceImage)
                    binding.tvPlaceRatingCount.text = it.reviewAvg.toString()
//                    binding.distanceAppCompatTextView.text =
//                        it.distance?.toBigDecimal()?.setScale(1, RoundingMode.UP)?.toDouble().toString().plus(" miles")
                    if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                        binding.distanceAppCompatTextView.text = if (it.distance != 0.00) {
                            it.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                        } else {
                            "0 ".plus(getString(R.string.label_miles))
                        }
                    } else {
                        binding.distanceAppCompatTextView.text = if (it.distance != 0.00) {
                            it.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                        } else {
                            "0 ".plus(getString(R.string.label_kms))
                        }
                    }
                    venueId = it.id
                    placeId ="Offline"
                }.autoDispose()
            }
            bottomSheetFragment.show(supportFragmentManager, AddUsernameEmailSocialLoginBottomSheet::class.java.name)
        }.autoDispose()

        binding.categoryLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val eventCategoryBottomSheet = EventCategoryBottomSheet.getIntent()
            eventCategoryBottomSheet.venueClick.subscribeAndObserveOnMainThread {
                categoryId = it.id

                Glide.with(this@CreateEventsActivity)
                    .load(it.thumbnailImage)
                    .placeholder(R.drawable.venue_placeholder)
                    .error(R.drawable.venue_placeholder)
                    .into(binding.categoryImageAppCompatImageView)

                binding.categoryNameAppCompatTextView.text = it.categoryName

            }
            eventCategoryBottomSheet.show(supportFragmentManager, EventCategoryBottomSheet.javaClass.simpleName)
        }

        eventAdsAdapter.listOfMedia = arrayListOf()


        mediaAdapter = EventAdsAdapter(this@CreateEventsActivity).apply {
            addMediaActionState.subscribeAndObserveOnMainThread {
                checkPermission("POST_TYPE_IMAGE")
            }

            deleteActionState.subscribeAndObserveOnMainThread {
                listofmedia.remove(it)
                mediaAdapter.listOfMedia = listofmedia
            }

            mediaActionState.subscribeAndObserveOnMainThread {}
        }

        binding.addMediaVideoRecyclerView.apply {
            adapter = mediaAdapter
        }

        mediaAdapter.listOfMedia = arrayListOf()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.LoadingState -> {}
                is EventViewState.ErrorMessage -> {}
                is EventViewState.SuccessMessage -> {}
                is EventViewState.EventDetails -> {
                    finish()
                }

                else -> {}
            }
        }
    }

    private fun checkPermission(type: String) {
        XXPermissions.with(this).permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO, Permission.READ_MEDIA_VIDEO, Permission.READ_MEDIA_IMAGES
                )
            ).request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResult(
                            AddMediaEventActivity.getIntentWithData(
                                this@CreateEventsActivity, type
                            ), REQUEST_CODE_EVENT
                        )
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)

                    XXPermissions.startPermissionActivity(this@CreateEventsActivity, permissions);
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    private fun checkPermissions(type: String) {
        XXPermissions.with(this).permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO, Permission.READ_MEDIA_VIDEO
                )
            ).request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResult(
                            AddMediaEventActivity.getIntentWithData(
                                this@CreateEventsActivity, type
                            ), REQUEST_CODE
                        )
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)

                    XXPermissions.startPermissionActivity(this@CreateEventsActivity, permissions);
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    private fun initUI() {
        intent?.let {
            venueDetail = it.getParcelableExtra<VenueDetail>(VENUE_DETAILS)
        }

        binding.dateAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {

            val calendar = Calendar.getInstance()
            //calendar.add(Calendar.MINUTE, 5)
            val tomorrow = calendar.time

            CardDatePickerDialog.builder(this).setTitle(resources.getString(R.string.label_event_start_date_1)).setDisplayType(
                    DateTimeConfig.YEAR, DateTimeConfig.MONTH, DateTimeConfig.DAY, DateTimeConfig.HOUR, DateTimeConfig.MIN
                ).setDefaultTime(System.currentTimeMillis()).setLabelText(" Y", " M", " D", " Hr", " Min").showBackNow(false).showFocusDateInfo(false)
                .setOnChoose("Submit") {
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utc.timeInMillis = it
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
                    val formatted: String = format.format(utc.time)
                    binding.dateAppCompatTextView.text = formatted
                }.setMinTime(tomorrow.time).setOnCancel("Cancel", {}).build().show()
        }

        binding.timeAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            CardDatePickerDialog.builder(this).setTitle(resources.getString(R.string.label_event_end_date_1)).setDisplayType(
                    DateTimeConfig.YEAR, DateTimeConfig.MONTH, DateTimeConfig.DAY, DateTimeConfig.HOUR, DateTimeConfig.MIN
                ).setDefaultTime(System.currentTimeMillis()).setLabelText(" Y", " M", " D", " Hr", " Min").showBackNow(false).showFocusDateInfo(false)
                .setOnChoose("Submit") {
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utc.timeInMillis = it
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
                    val formatted: String = format.format(utc.time)
                    binding.timeAppCompatTextView.text = formatted
                }.setOnCancel("Cancel", {}).build().show()
        }

        binding.createEventMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.eventNameAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_event_name))
            } else if (binding.descriptionAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_event_description))
            } else if (binding.dateAppCompatTextView.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_event_date))
            } else if (binding.timeAppCompatTextView.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_event_time))
            }
            else if (venueId == 0) {
                showToast(resources.getString(R.string.error_select_venue))
            }
            else if (categoryId == -1) {
                showToast(resources.getString(R.string.please_select_category))
            }
            else if (listofmedia.size < 1) {
                showToast(resources.getString(R.string.error_event_add_media))
            } else {
                createEventsViewModel.createEvents(
                    CreateEventResponse(
                        binding.eventNameAppCompatEditText.text.toString(),
                        binding.descriptionAppCompatEditText.text.toString(),
                        binding.dateAppCompatTextView.text.toString(),
                        binding.timeAppCompatTextView.text.toString(),
                        0,
                        location,
                        latitude.toString(),
                        longitude.toString(),
                        placeId,
                        uid,
                        listofmedia,
                        venueId,
                        categoryId,
                        isPrivate
                    )
                )
            }
        }

        binding.modeOfEventSwitchCompat.setOnCheckedChangeListener { compoundButton, b ->
            binding.locationLinearLayout.visibility = if (b) View.GONE else View.VISIBLE
            if (b) {
                binding.locationAppCompatImageView.setImageResource(R.drawable.location_icon)
                binding.locationAppCompatImageView.background = resources.getDrawable(R.drawable.new_login_edittext_background,null)
                binding.cardContainer.isVisible = false
                binding.tvPickEventLocation.text = resources.getText(R.string.label_pick_event_location)
                venueId = 0
                placeId = "Online"
            }
        }
        binding.isPrivateSwitch.setOnCheckedChangeListener { compoundButton, b ->
            isPrivate = if (b) 1 else 0
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE) {
                data?.let {
                    uid = it.getStringExtra("UID")
                    listofadsmedia.add(it.getStringExtra("MEDIA_URL") ?: "")
                    eventAdsAdapter.listOfMedia = listofadsmedia
                }
            } else if (requestCode == REQUEST_CODE_EVENT) {
                data?.let {
                    val list =  it.getStringArrayListExtra("MEDIA_URL")
                    list?.forEach { item ->
                        listofmedia.add(item)
                    }
                    mediaAdapter.listOfMedia = listofmedia
                }
            } else if (requestCode == REQUEST_CODE_LOCATION) {
                data?.let {
                    latitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE, 0.0)
                    longitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE, 0.0)
                    location = it.getStringExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION)
                    placeId = it.getStringExtra(AddPostLocationActivity.INTENT_PLACE_ID)

                }
            }
        }
    }
}