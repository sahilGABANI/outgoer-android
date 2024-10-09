package com.outgoer.ui.sponty

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.textChanges
import com.loper7.date_time_picker.DateTimeConfig
import com.loper7.date_time_picker.dialog.CardDatePickerDialog
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.sponty.model.CreateSpontyRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityCreateSpontyBinding
import com.outgoer.ui.chat.DisplayActivity
import com.outgoer.ui.chat.DisplayActivity.Companion.INTENT_EXTRA_MEDIA_VIDEO
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.createevent.view.EventAdsAdapter
import com.outgoer.ui.fullscreenimage.FullScreenImageActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.sponty.view.SpontyLocationAdapter
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.ui.tag.AddTagToPostActivity
import com.outgoer.ui.vennue_list.VenueListActivity
import com.outgoer.ui.venue.SpontyVenueLocationBottomSheet
import com.outgoer.ui.video_preview.VideoPreviewActivity
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateSpontyActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateSpontyBinding

    private lateinit var spontyLocationAdapter: SpontyLocationAdapter

    private var listoflocation: ArrayList<String> = arrayListOf()
    private var listOfPlaceId: ArrayList<String> = arrayListOf()
    private var taggedPeopleHashMap = HashMap<Int, String?>()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var initialListOfFollower: List<FollowUser> = listOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SpontyViewModel>
    private lateinit var spontyViewModel: SpontyViewModel
    private lateinit var commentTagPeopleAdapter: CommentTagPeopleAdapter

    private var listofmedia: ArrayList<String> = arrayListOf()
    private lateinit var mediaAdapter: EventAdsAdapter

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var uid: String? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var taggedVenueHashMap = HashMap<Int, String?>()
    private var spontyDateAndTime = ""
    private var uId: String? = null

    companion object {
        private const val REQUEST_CODE_LOCATION = 10002
        private const val REQUEST_CODE_EVENT = 1890
        private const val REQUEST_CODE = 1889
        private const val REQUEST_CODE_TAG_VENUE = 10011
        private const val REQUEST_CODE_TAG_PEOPLE = 10001

        private const val UPLOAD_MEDIA_OPTION = "UPLOAD_MEDIA_OPTION"
        private const val UPLOAD_MEDIA_TYPE = "UPLOAD_MEDIA_TYPE"

        fun getIntent(context: Context, mediaType: String, filePath: String? = null): Intent {
            var intent = Intent(context, CreateSpontyActivity::class.java)

            intent.putExtra(UPLOAD_MEDIA_TYPE, mediaType)
            if(filePath != null)
                intent.putExtra(UPLOAD_MEDIA_OPTION, filePath)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityCreateSpontyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        spontyViewModel = getViewModelFromFactory(viewModelFactory)

        spontyViewModel.getCloudFlareConfig()

        initUI()
        listenToViewModel()
    }

    private fun fetchCurrentLocation() {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        val task = fusedLocationProviderClient.lastLocation
                        task.addOnSuccessListener { location ->
                            location?.let {
                            }
                        }
                    } else {
                        showToast(getString(R.string.msg_location_permission_required_for_venue))
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    showToast(getString(R.string.msg_location_permission_required_for_venue))
                    if (never) {
                        XXPermissions.startPermissionActivity(
                            this@CreateSpontyActivity,
                            permissions
                        )
                    }
                }
            })
    }

    private fun initUI() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchCurrentLocation()
        val userInfo = loggedInUserCache.getLoggedInUser()?.loggedInUser

        Glide.with(this)
            .load(userInfo?.avatar)
            .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
            .into(binding.ivProfile)

        binding.userAppCompatTextView.text = userInfo?.name ?: ""

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
           onBackPressed()
        }

        binding.tagVenueRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                VenueListActivity.getIntent(this),
                REQUEST_CODE_TAG_VENUE
            )
        }

        binding.tagFriendRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                AddTagToPostActivity.launchActivity(this, hashMapOf()),
                REQUEST_CODE_TAG_PEOPLE
            )
        }

        binding.locationRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val spontyLocationBottomSheet = SpontyVenueLocationBottomSheet.newInstance().apply {
                venueLocationClick.subscribeAndObserveOnMainThread {
                    listoflocation.clear()
                    listOfPlaceId.clear()
                    it.formattedAddress?.let { it1 -> listoflocation.add(it1) }
                    it.placeId?.let { it1 -> listOfPlaceId.add(it1) }
                    spontyLocationAdapter.listOfSpontyLocation = listoflocation
                }.autoDispose()
                placeAvailableClick.subscribeAndObserveOnMainThread {
                    listoflocation.clear()
                    listOfPlaceId.clear()
                    it.description?.let { it1 -> listoflocation.add(it1) }
                    it.placeId?.let { it1 -> listOfPlaceId.add(it1) }
                    spontyLocationAdapter.listOfSpontyLocation = listoflocation
                }.autoDispose()

                googlePlacesClick.subscribeAndObserveOnMainThread {
                    listoflocation.clear()
                    listOfPlaceId.clear()
                    it.venueAddress?.let { it1 -> listoflocation.add(it1) }
//                    it.placeId?.let { it1 -> listOfPlaceId.add(it1) }
                    spontyLocationAdapter.listOfSpontyLocation = listoflocation
                }

            }
            spontyLocationBottomSheet.show(supportFragmentManager, SpontyVenueLocationBottomSheet.javaClass.name)
        }


        commentTagPeopleAdapter = CommentTagPeopleAdapter(this).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.captionAppCompatEditText.selectionStart
                val descriptionString = binding.captionAppCompatEditText.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                spontyViewModel.searchTagUserClicked(
                    binding.captionAppCompatEditText.text.toString(),
                    subString,
                    followUser
                )
            }.autoDispose()
        }

        binding.rlFollowerList.apply {
            layoutManager = LinearLayoutManager(this@CreateSpontyActivity)
            adapter = commentTagPeopleAdapter
        }

        mediaAdapter = EventAdsAdapter(this@CreateSpontyActivity).apply {
            addMediaActionState.subscribeAndObserveOnMainThread {
                if(listofmedia.filter { it.contains("imagedelivery") }.size == 3 && listofmedia.filter { it.contains("thumbnail") }.size == 1) {
                    showToast("You can select 3 images and 1 video")
                } else {
                    checkPermission("POST_TYPE_IMAGE")
                }
            }

            deleteActionState.subscribeAndObserveOnMainThread {
                listofmedia.remove(it)
                mediaAdapter.listOfMedia = listofmedia
            }

            mediaActionState.subscribeAndObserveOnMainThread {
                println("ite: " + it)
                println("ite: " + uId)
//                startActivity(FullScreenImageActivity.getIntent(this@CreateSpontyActivity, uId ?: ""))
                Observable.timer(2000, TimeUnit.MILLISECONDS)
                    .subscribeAndObserveOnMainThread { d ->
                        startActivity(DisplayActivity.launchActivity(this@CreateSpontyActivity, uId, INTENT_EXTRA_MEDIA_VIDEO, it))
                    }.autoDispose()
            }
        }

        binding.addMediaVideoRecyclerView.apply {
            adapter = mediaAdapter
        }

        if(intent.hasExtra(UPLOAD_MEDIA_OPTION)) {
            listofmedia.add(" ")
            mediaAdapter.listOfMedia = listofmedia
        } else {
            mediaAdapter.listOfMedia = arrayListOf()
        }

        val loggedInUserId = loggedInUserCache?.getLoggedInUser()?.loggedInUser?.id ?: 0
        binding.captionAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.rlFollowerList.visibility = View.GONE
                } else {
                    val lastChar = it.last().toString()
                    if (lastChar.contains("@")) {
                        commentTagPeopleAdapter.listOfDataItems = initialListOfFollower
                        binding.rlFollowerList.visibility = View.VISIBLE
                    } else {
                        val wordList = it.split(" ")
                        val lastWord = wordList.last()
                        if (lastWord.contains("@")) {
                            spontyViewModel.getFollowersList(
                                loggedInUserId,
                                lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

        spontyViewModel.getInitialFollowersList(loggedInUserId)

        binding.postMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.captionAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_please_enter_your_plan))
            } else {

                val taggedVenueList = taggedVenueHashMap.keys

                val mentionId = arrayListOf<Int>()

                taggedPeopleHashMap.forEach {
                    mentionId.add(it.key)
                }

                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val spontyDateFormate = SimpleDateFormat("yyyy-MM-dd hh:mm")
                var spontyDT = spontyDateFormate.format(utc.time)
                val newListOfMedia = arrayListOf<String>()
                listofmedia?.forEach { item ->
                    if(!item.contains("thumbnails")) {
                        newListOfMedia.add(item)
                    }
                }

                spontyViewModel.createSponty(
                    CreateSpontyRequest(
                        binding.captionAppCompatEditText.text.toString(),
                        if(listoflocation.size == 0) null else listoflocation.joinToString(","),
                        if(spontyDateAndTime.isNullOrEmpty()) spontyDT else spontyDateAndTime,
                        listOfPlaceId.joinToString(","),
                        latitude, longitude,
                        tagPeople = mentionId.joinToString(separator = ","),
                        venueId = if (taggedVenueList.isNotEmpty()) {
                            TextUtils.join(",", taggedVenueList)
                        } else {
                            null
                        },
                        uid = uid,
                        spontyImage = newListOfMedia
                    )
                )
            }
        }

        binding.addDateRelativeLayout.setOnClickListener {

            val calendar = Calendar.getInstance()
           // calendar.add(Calendar.MINUTE, 5)
            val tomorrow = calendar.time

            CardDatePickerDialog.builder(this)
                .setTitle(resources.getString(R.string.label_event_start_date_1)).setDisplayType(
                DateTimeConfig.YEAR,
                DateTimeConfig.MONTH,
                DateTimeConfig.DAY,
                DateTimeConfig.HOUR,
                DateTimeConfig.MIN
            ).setDefaultTime(System.currentTimeMillis())
                .setLabelText(" Y", " M", " D", " Hr", " Min").showBackNow(false)
                .showFocusDateInfo(false)
                .setOnChoose("Submit") {
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utc.timeInMillis = it
                    val format = SimpleDateFormat("dd'th' MMM yyyy 'at' hh:mm a")
                    val formatted: String = format.format(utc.time)
                   val spontyDateFormate = SimpleDateFormat("yyyy-MM-dd hh:mm")
                    spontyDateAndTime = spontyDateFormate.format(utc.time)
                    binding.dateAppCompatTextView.setText(formatted)
                }
                .setMinTime(tomorrow.time).setOnCancel("Cancel") {}.build().show()
        }

        binding.locationLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                AddPostLocationActivity.launchActivity(this),
                REQUEST_CODE_LOCATION
            )

        }.autoDispose()

        spontyLocationAdapter = SpontyLocationAdapter(this).apply {
            spontyLocationActionState.subscribeAndObserveOnMainThread {
            }
        }

        binding.locationRecyclerView.apply {
            adapter = spontyLocationAdapter
        }
    }


    private fun checkPermission(type: String) {
        XXPermissions.with(this).permission(
            listOf(
                Permission.CAMERA, Permission.RECORD_AUDIO, Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO
            )
        ).request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                if (all) {
                    startActivityForResult(
                        AddMediaEventActivity.getIntentWithData(
                            this@CreateSpontyActivity, type, -1
                        ), REQUEST_CODE_EVENT
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

    private fun listenToViewModel() {
        spontyViewModel.spontyDataState.subscribeAndObserveOnMainThread {
            when (it) {
                is SpontyViewModel.SpontyDataState.LoadingState -> {
                    binding.progressbar.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                }
                is SpontyViewModel.SpontyDataState.GetCloudFlareConfig -> {

                    if(intent.hasExtra(UPLOAD_MEDIA_OPTION)) {

                        val imageFile = File(intent.getStringExtra(UPLOAD_MEDIA_OPTION))

                        if(intent.getStringExtra(UPLOAD_MEDIA_TYPE).equals(CreateMediaType.sponty.name)) {
                            spontyViewModel.uploadImageToCloudFlare(
                                this,
                                it.cloudFlareConfig,
                                imageFile,
                                "POST_TYPE_IMAGE"
                            )
                        } else {
                            spontyViewModel.uploadVideoToCloudFlare(
                                this,
                                it.cloudFlareConfig,
                                imageFile,
                                "POST_TYPE_VIDEO"
                            )
                        }
                    }
                }

                is SpontyViewModel.SpontyDataState.UploadMediaCloudFlareSuccess -> {
                    listofmedia.clear()
                    listofmedia.add(it.mediaUrl)
                    mediaAdapter.listOfMedia = listofmedia
                }
                is SpontyViewModel.SpontyDataState.UploadMediaCloudFlareVideoSuccess -> {
//                    listofmedia.clear()
//                    listofmedia.add(it.mediaUrl)
//                    mediaAdapter.listOfMedia = listofmedia
                }
                is SpontyViewModel.SpontyDataState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("SpontyDataState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is SpontyViewModel.SpontyDataState.LoadingState -> {
                    if (it.isLoading) {
                        binding.progressbar.visibility = View.VISIBLE
                        binding.postMaterialButton.visibility = View.GONE
                    } else {
                        binding.progressbar.visibility = View.GONE
                        binding.postMaterialButton.visibility = View.VISIBLE
                    }
                }
                is SpontyViewModel.SpontyDataState.SpontyInfo -> {
                    finish()
                    startActivity(HomeActivity.getIntentWithSponty(this@CreateSpontyActivity, true))
                }
                is SpontyViewModel.SpontyDataState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is SpontyViewModel.SpontyDataState.FollowerList -> {
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is SpontyViewModel.SpontyDataState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.captionAppCompatEditText.setText(it.descriptionString)
                    binding.captionAppCompatEditText.setSelection(binding.captionAppCompatEditText.text.toString().length)
                }
                else -> {}
            }
        }
    }

    private fun mentionTagPeopleViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.rlFollowerList.visibility == View.GONE) {
            binding.rlFollowerList.visibility = View.VISIBLE
        } else if (!isVisibility && binding.rlFollowerList.visibility == View.VISIBLE) {
            binding.rlFollowerList.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_LOCATION) {
                data?.let {
                    if (it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_PLACE_ID)
                    ) {
                        val latitude = it.getDoubleExtra(
                            AddPostLocationActivity.INTENT_EXTRA_LATITUDE,
                            0.toDouble()
                        )
                        val longitude = it.getDoubleExtra(
                            AddPostLocationActivity.INTENT_EXTRA_LONGITUDE,
                            0.toDouble()
                        )

                        it.getStringExtra(AddPostLocationActivity.INTENT_PLACE_ID)?.let {
                            listOfPlaceId.add(it)
                        }

                        val location =
                            it.getStringExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION)
                        if (latitude != 0.toDouble() && longitude != 0.toDouble() && !location.isNullOrEmpty()) {
                            this.latitude = latitude
                            this.longitude = longitude
                            listoflocation.add(location)
                            spontyLocationAdapter.listOfSpontyLocation = listoflocation
                        }
                    }

                    if (it.hasExtra(AddPostLocationActivity.INTENT_PLACE_ID)) {

                    }
                }
            } else if (requestCode == REQUEST_CODE_TAG_PEOPLE) {
                data?.let {
                    if (it.hasExtra(AddTagToPostActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)) {
                        val taggedPeopleHashMap = it.getSerializableExtra(AddTagToPostActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)
                        if (taggedPeopleHashMap != null) {
                            this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, String?>
                            val taggedPeopleList = taggedPeopleHashMap.values
                            if (taggedPeopleList.isNotEmpty()) {
                                binding.tvSelectedFriends.text = TextUtils.join(", ", taggedPeopleList)
                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_TAG_VENUE) {
                data?.let {
                    if (it.hasExtra(VenueListActivity.INTENT_EXTRA_VENUE_NAME)) {
                        val tagVenueHashMap =
                            it.getSerializableExtra(VenueListActivity.INTENT_EXTRA_VENUE_NAME)
                        val venueId =
                            it.getSerializableExtra(VenueListActivity.INTENT_EXTRA_VENUE_ID)

                        if (tagVenueHashMap != null) {
                            this.taggedVenueHashMap = tagVenueHashMap as HashMap<Int, String?>
                            val taggedPeopleList = tagVenueHashMap.values

                            if (taggedPeopleList.isNotEmpty()) {
                                binding.tvSelectedVenueLocation.text =
                                    TextUtils.join(", ", taggedPeopleList)
                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE) {
                data?.let {
                    uid = it.getStringExtra("UID")
                }
            } else if (requestCode == REQUEST_CODE_EVENT) {
                data?.let {
                    val list =  it.getStringArrayListExtra("MEDIA_URL")
                    uId =  it.getStringExtra("MEDIA_UID")
                    println("uId video url: " + uId)

                    if(uid.isNullOrEmpty()) {
                        uid = it.getStringExtra("UID")
                    }

                    if(listofmedia.filter { it.contains("imagedelivery") }.size < 3) {
                        list?.forEach { item ->
                            listofmedia.add(item)
                        }
                    } else if(listofmedia.filter { it.contains("thumbnail") }.size < 1) {
                        list?.forEach { item ->
                            listofmedia.removeIf { it.contains("thumbnails") }
                            listofmedia.add(item)
                        }
                    }
//                    list?.forEach { item ->
//                        if(listofmedia.contains("thumbnails"))
//                        listofmedia.add(item)
//                    }
                    mediaAdapter.listOfMedia = listofmedia
                }
            }
        }
    }
}