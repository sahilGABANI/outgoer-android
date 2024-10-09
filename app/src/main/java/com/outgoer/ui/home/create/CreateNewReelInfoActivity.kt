package com.outgoer.ui.home.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityCreateNewReelInfoBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.newReels.viewmodel.AddNewReelViewModel
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.progress_dialog.ProgressDialogFragment
import com.outgoer.ui.tag.AddTagToPostActivity
import com.outgoer.ui.vennue_list.VenueListActivity
import com.outgoer.ui.venue.SpontyVenueLocationBottomSheet
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject


class CreateNewReelInfoActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_AUDIO_PATH = "INTENT_EXTRA_AUDIO_PATH"

        private const val REQUEST_CODE_TAG_PEOPLE = 10001
        private const val REQUEST_CODE_TAG_VENUE = 10011
        private const val REQUEST_CODE_LOCATION = 10002

        fun launchActivity(context: Context, videoPath: String, audioPath: String? = null): Intent {
            val intent = Intent(context, CreateNewReelInfoActivity::class.java)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)

            if(audioPath != null)
                intent.putExtra(INTENT_EXTRA_AUDIO_PATH, audioPath)

            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewReelViewModel>
    private lateinit var addNewReelViewModel: AddNewReelViewModel

    private lateinit var binding:ActivityCreateNewReelInfoBinding

    private var cloudFlareConfig: CloudFlareConfig? = null

    private var videoPath = ""
    private var audioPath = ""
    private var videoId = ""

    private var latitude = 0.0
    private var longitude = 0.0
    private var location = ""
    private var placeId = ""

    private var hashtags = ""
    private var taggedPeopleHashMap = HashMap<Int, String?>()

    private var addedHashtagArrayList = ArrayList<String>()
    private var taggedVenueHashMap = HashMap<Int, String?>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        addNewReelViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityCreateNewReelInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {

            if (it.hasExtra(INTENT_EXTRA_AUDIO_PATH)) {
                val audio = it.getStringExtra(INTENT_EXTRA_AUDIO_PATH)

                if (!audio.isNullOrEmpty()) {
                    this.audioPath = audio
                    getCloudConfig()
                } else {
                    onBackPressed()
                }
            }

            if (it.hasExtra(INTENT_EXTRA_VIDEO_PATH)) {
                val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                if (!videoPath.isNullOrEmpty()) {
                    this.videoPath = videoPath
                    getCloudConfig()
                } else {
                    onBackPressed()
                }
            } else {
                onBackPressed()
            }

        } ?: onBackPressed()
    }

    private fun getCloudConfig() {
        Glide.with(this)
            .load(videoPath)
            .into(binding.ivSelectedMedia)

        listenToViewEvents()
        listenToViewModel()
        addNewReelViewModel.getCloudFlareConfig()
    }
    object Utils {
        fun getConvertedFile(directoryPath: String, fileName: String): File {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                directory.mkdirs()
            }


            return File(directory, fileName)
        }
    }
    private fun listenToViewEvents() {
        val outputFileName = "merge_video_file.mp4"

        val mp4UriAfterTrim = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
            Utils.getConvertedFile(
                it.absolutePath, outputFileName
            )
        }

        val cmd = arrayOf(
            "-y", "-i", videoPath, "-i", audioPath, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", mp4UriAfterTrim?.path
        )

        Thread {
            val result: Int = FFmpeg.execute(cmd)
            Log.d("VideoTrim", "result: $result")
            if (result == 0) {
                Log.i("VideoTrim", "result: Success")
            } else if (result == 255) {
                Log.d("VideoTrim", "result: Canceled")
            } else {
                Log.e("VideoTrim", "result: Failed")
            }
        }.start()


        binding.rlTag.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(AddTagToPostActivity.launchActivity(this, taggedPeopleHashMap),
                REQUEST_CODE_TAG_PEOPLE)
        }.autoDispose()

        binding.rlVenueTag.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                VenueListActivity.getIntent(this),
                REQUEST_CODE_TAG_VENUE
            )

        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.rlLocation.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivityForResultWithDefaultAnimation(AddPostLocationActivity.launchActivity(this), REQUEST_CODE_LOCATION)
            var spontyVenueLocationBottomSheet = SpontyVenueLocationBottomSheet.newInstance().apply {
                venueLocationClick.subscribeAndObserveOnMainThread {
                    if(!it.placeId.isNullOrEmpty()) {
                        this@CreateNewReelInfoActivity.placeId = it.placeId ?: ""
                    }
                    if (it.geometry?.location?.lat != 0.toDouble() && it.geometry?.location?.lng != 0.toDouble() && !it.formattedAddress.isNullOrEmpty()) {
                        this@CreateNewReelInfoActivity.latitude = (it.geometry?.location?.lat ?: 0L) as Double
                        this@CreateNewReelInfoActivity.longitude = (it.geometry?.location?.lng ?: 0L) as Double
                        this@CreateNewReelInfoActivity.location = it.formattedAddress
                        binding.tvSelectedLocation.text = location
                    }
                }.autoDispose()

                googlePlacesClick.subscribeAndObserveOnMainThread {
                    if(!it.id.isNullOrEmpty()) {
                        this@CreateNewReelInfoActivity.placeId = it.id ?: ""
                    }
                    if (it.latitude != 0.toDouble() && it.longitude != 0.toDouble() && !it.venueAddress.isNullOrEmpty()) {
                        this@CreateNewReelInfoActivity.latitude = (it.latitude ?: 0L) as Double
                        this@CreateNewReelInfoActivity.longitude = (it.longitude ?: 0L) as Double
                        this@CreateNewReelInfoActivity.location = it.venueAddress ?: ""
                        binding.tvSelectedLocation.text = location
                    }
                }.autoDispose()
            }
            spontyVenueLocationBottomSheet.show(supportFragmentManager, SpontyVenueLocationBottomSheet.javaClass.name)
        }.autoDispose()

        binding.btnPost.throttleClicks().subscribeAndObserveOnMainThread {
            cloudFlareConfig?.let {
                buttonVisibility(true)
                compressVideoFile(mp4UriAfterTrim)
            } ?: addNewReelViewModel.getCloudFlareConfig()
        }.autoDispose()

        binding.rlHashTags.throttleClicks().subscribeAndObserveOnMainThread {
            openAddHashTagBottomSheet()
        }
    }

    private fun listenToViewModel() {
        addNewReelViewModel.addNewReelState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewReelViewModel.AddNewReelViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is AddNewReelViewModel.AddNewReelViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is AddNewReelViewModel.AddNewReelViewState.UploadVideoCloudFlareSuccess -> {
                    videoId = it.videoId
                    createReel()
                }
                is AddNewReelViewModel.AddNewReelViewState.CreateReelSuccessMessage -> {
                    startActivityWithFadeInAnimation(HomeActivity.getIntent(this@CreateNewReelInfoActivity))
                    finish()
                }
                is AddNewReelViewModel.AddNewReelViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is AddNewReelViewModel.AddNewReelViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnPost.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnPost.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_TAG_PEOPLE) {
                data?.let {
                    if (it.hasExtra(AddTagToPostActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)) {
                        val taggedPeopleHashMap = it.getSerializableExtra(AddTagToPostActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)
                        if (taggedPeopleHashMap != null) {
                            this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, String?>
                            val taggedPeopleList = taggedPeopleHashMap.values
                            if (taggedPeopleList.isNotEmpty()) {
                                binding.tvSelectedPeople.text = TextUtils.join(", ", taggedPeopleList)
                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_TAG_VENUE) {
                data?.let {
                    if (it.hasExtra(VenueListActivity.INTENT_EXTRA_VENUE_NAME)) {
                        val tagVenueHashMap = it.getSerializableExtra(VenueListActivity.INTENT_EXTRA_VENUE_NAME)
                        val venueId = it.getSerializableExtra(VenueListActivity.INTENT_EXTRA_VENUE_ID)

                        if (tagVenueHashMap != null) {
                            this.taggedVenueHashMap = tagVenueHashMap as HashMap<Int, String?>
                            val taggedPeopleList = tagVenueHashMap.values

                            if (taggedPeopleList.isNotEmpty()) {
                                binding.tvVenueSelectedPeople.text = TextUtils.join(", ", taggedPeopleList)
                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_LOCATION) {
                data?.let {
                    if (it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION) &&
                        it.hasExtra(AddPostLocationActivity.INTENT_PLACE_ID)
                    ) {
                        val latitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE, 0.toDouble())
                        val longitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE, 0.toDouble())
                        val location = it.getStringExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION)
                        val placeId = it.getStringExtra(AddPostLocationActivity.INTENT_PLACE_ID)

                        if(!placeId.isNullOrEmpty()) {
                            this.placeId = placeId
                        }
                        if (latitude != 0.toDouble() && longitude != 0.toDouble() && !location.isNullOrEmpty()) {
                            this.latitude = latitude
                            this.longitude = longitude
                            this.location = location
                            binding.tvSelectedLocation.text = location
                        }
                    }
                }
            }
        }
    }

    private fun openAddHashTagBottomSheet(){
        val bottomSheet = AddHashtagBottomSheet(addedHashtagArrayList)
        bottomSheet.addedHashTagsClicks.subscribeAndObserveOnMainThread {
            bottomSheet.dismissBottomSheet()
            addedHashtagArrayList = it
            binding.tvHashtag.text = addedHashtagArrayList.joinToString()
            hashtags = binding.tvHashtag.text.toString()
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, AddHashtagBottomSheet::class.java.name)
    }

    private fun createReel() {
        val request = CreateReelRequest()
        request.caption = binding.etCaption.text.toString()
        request.latitude = latitude
        request.longitude = longitude
        request.reelLocation = location
        request.hashTags = hashtags
        request.placeId = placeId

        val taggedPeopleList = taggedPeopleHashMap.keys
        request.tagPeople = if (taggedPeopleList.isNotEmpty()) {
            TextUtils.join(",", taggedPeopleList)
        } else {
            null
        }

        val taggedVenueList = taggedVenueHashMap.keys
        request.tagVenue = if (taggedVenueList.isNotEmpty()) {
            TextUtils.join(",", taggedVenueList)
        } else {
            null
        }

        request.uid = videoId
        addNewReelViewModel.createReel(request)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivityWithDefaultAnimation(HomeActivity.getIntent(this))
    }

    private fun compressVideoFile(mp4UriAfterTrim: File?) {
        val videoUris = listOf(Uri.fromFile(mp4UriAfterTrim))
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = "outgoer"
                ),
                configureWith = Configuration(
                    quality = VideoQuality.HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = false,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {

                    }

                    override fun onStart(index: Int) {

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        runOnUiThread {
                            addNewReelViewModel.uploadVideoToCloudFlare(this@CreateNewReelInfoActivity, cloudFlareConfig!!, File(path!!))
                            var progressDialogFragment = ProgressDialogFragment.newInstance()
                            progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                                progressDialogFragment.dismiss()
                            }
                            progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment.javaClass.name)
                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Timber.wtf(failureMessage)
                    }

                    override fun onCancelled(index: Int) {
                        Timber.wtf("compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
            )
        }
    }
}