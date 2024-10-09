package com.outgoer.ui.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.api.reels.model.UidRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityForResultWithDefaultAnimation
import com.outgoer.base.extension.startActivityWithFadeInAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAddNewPostInfoBinding
import com.outgoer.service.UploadingPostReelsService
import com.outgoer.ui.add_hashtag.HashtagActivity
import com.outgoer.ui.add_hashtag.HashtagActivity.Companion.HASHTAG_LIST
import com.outgoer.ui.add_location.AddLocationActivity
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.create_story.model.SelectedMedia
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.music.AddMusicActivity
import com.outgoer.ui.post.viewmodel.AddNewPostViewModel
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.progress_dialog.ProgressDialogFragment
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.ui.tag.AddTagToPostActivity
import com.outgoer.ui.vennue_list.VenueListActivity
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddNewPostInfoActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_POST_TYPE = "INTENT_EXTRA_POST_TYPE"
        private const val INTENT_EXTRA_LIST_OF_POST = "INTENT_EXTRA_LIST_OF_POST"
        const val POST_TYPE_IMAGE = "POST_TYPE_IMAGE"
        const val POST_TYPE_VIDEO = "POST_TYPE_VIDEO"
        const val POST_TYPE_REELS = "POST_TYPE_REELS"

        private const val INTENT_EXTRA_IMAGE_PATH_LIST = "INTENT_EXTRA_IMAGE_PATH_LIST"
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_AUDIO_RESPONSE = "INTENT_EXTRA_AUDIO_RESPONSE"
        private const val INTENT_EXTRA_AUDIO_PATH = "INTENT_EXTRA_AUDIO_PATH"

        private const val REQUEST_CODE_TAG_PEOPLE = 10001
        private const val REQUEST_CODE_TAG_VENUE = 10011
        private const val REQUEST_CODE_LOCATION = 10002
        private const val REQUEST_CODE_HASHTAG = 10012
        private const val REQUEST_CODE_MUSIC = 11012

        fun launchActivity(
            context: Context,
            postType: String,
            imagePathList: ArrayList<String>? = null,
            videoPath: String? = null,
            audioResponse: MusicResponse? = null,
            audioPath: String? = null,
        ): Intent {
            val intent = Intent(context, AddNewPostInfoActivity::class.java)

            intent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            intent.putStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST, imagePathList)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            if (audioResponse != null) {
                intent.putExtra(INTENT_EXTRA_AUDIO_RESPONSE, audioResponse)
            }
            intent.putExtra(INTENT_EXTRA_AUDIO_PATH, audioPath)
            return intent
        }


        fun getIntent(
            context: Context,
            postType: String,
            listOfSelectedFiles: java.util.ArrayList<SelectedMedia>,
        ): Intent {
            val intent = Intent(context, AddNewPostInfoActivity::class.java)

            intent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            intent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
            return intent
        }
    }

    private var listOfSelectedFiles: ArrayList<SelectedMedia>? = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var addNewPostViewModel: AddNewPostViewModel


    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var initialListOfFollower: List<FollowUser> = listOf()

    private lateinit var binding: ActivityAddNewPostInfoBinding

    private var cloudFlareConfig: CloudFlareConfig? = null

    private var postType = ""
    private var imagePathList = ArrayList<String>()
    private var videoPath = ""
    private var thumbnailPath = ""
    private var audioPath = ""

    private var imageUrlList = ArrayList<String>()
    private var videoId = ""
    private var thumbnail: String? = null

    private var latitude = 0.0
    private var longitude = 0.0
    private var location = ""
    private var taggedPeopleHashMap = HashMap<Int, String?>()
    private var taggedVenueHashMap = HashMap<Int, String?>()
    private var placeId = ""
    private var addedHashtagArrayList = ArrayList<String>()
    private var hashtags = ""
    private lateinit var musicResponse: MusicResponse


    private lateinit var commentTagPeopleAdapter: CommentTagPeopleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        addNewPostViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityAddNewPostInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
        initUI()
    }

    private fun initUI() {

        addNewPostViewModel.getInitialFollowersList(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)

        commentTagPeopleAdapter = CommentTagPeopleAdapter(this).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.etCaption.selectionStart
                val descriptionString = binding.etCaption.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                addNewPostViewModel.searchTagUserClicked(
                    binding.etCaption.text.toString(),
                    subString,
                    followUser
                )
            }.autoDispose()
        }

        binding.etCaption.textChanges()
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
                            addNewPostViewModel.getFollowersList(
                                loggedInUserCache.getUserId() ?: 0,
                                lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

        binding.rlFollowerList.apply {
            layoutManager = LinearLayoutManager(this@AddNewPostInfoActivity)
            adapter = commentTagPeopleAdapter
        }

        binding.hashtagRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            if(binding.hashtagItemAppCompatTextView.text.toString().equals(resources.getString(R.string.add_hashtag))) {
                startActivityForResultWithDefaultAnimation(HashtagActivity.getIntent(this@AddNewPostInfoActivity), REQUEST_CODE_HASHTAG)
            } else {
                startActivityForResultWithDefaultAnimation(HashtagActivity.getIntent(this@AddNewPostInfoActivity, binding.hashtagItemAppCompatTextView.text.toString()), REQUEST_CODE_HASHTAG)
            }
        }.autoDispose()

        binding.nextHashtag.throttleClicks().subscribeAndObserveOnMainThread {
            if (!resources.getString(R.string.add_hashtag).equals(binding.hashtagItemAppCompatTextView.text.toString())) {
                binding.nextHashtag.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                binding.hashtagItemAppCompatTextView.setText(resources.getString(R.string.add_hashtag))
            }
        }.autoDispose()
        binding.nextPeopleTag.throttleClicks().subscribeAndObserveOnMainThread {
            if (!resources.getString(R.string.label_tag_people).equals(binding.peopletagItemAppCompatTextView.text.toString())) {
                binding.nextPeopleTag.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                binding.peopletagItemAppCompatTextView.setText(resources.getString(R.string.label_tag_people))
            }
        }.autoDispose()
        binding.nextLocation.throttleClicks().subscribeAndObserveOnMainThread {
            if (!resources.getString(R.string.label_tag_venue).equals(binding.locationItemAppCompatTextView.text.toString())) {
                binding.nextLocation.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                binding.locationItemAppCompatTextView.setText(resources.getString(R.string.label_tag_venue))
            }
        }.autoDispose()
        binding.nextVenueTagAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!resources.getString(R.string.label_add_location).equals(binding.venueLocationItemAppCompatTextView.text.toString())) {
                binding.nextVenueTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                binding.venueLocationItemAppCompatTextView.setText(resources.getString(R.string.label_add_location))
            }
        }.autoDispose()
        binding.musicTagAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!resources.getString(R.string.add_music).equals(binding.musicTitleItemAppCompatTextView.text.toString())) {
                binding.musicTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                binding.musicTitleItemAppCompatTextView.setText(resources.getString(R.string.add_music))
                binding.musicSubtitleItemAppCompatTextView.visibility = View.GONE
            }
        }.autoDispose()
    }

    private fun loadDataFromIntent() {

        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_POST_TYPE)) {
                val postT = it.getStringExtra(INTENT_EXTRA_POST_TYPE)

                if (!postT.isNullOrEmpty()) {
                    this.postType = postT

                    when (postType) {
                        CreateMediaType.post.name -> {
                            if (it.hasExtra(INTENT_EXTRA_LIST_OF_POST)) {
                                listOfSelectedFiles = it.getParcelableArrayListExtra<SelectedMedia>(INTENT_EXTRA_LIST_OF_POST)

                                if (listOfSelectedFiles?.isNotEmpty() == true) {
                                    if (listOfSelectedFiles?.firstOrNull()?.isVideo() == true) {
                                        postType = CreateMediaType.post_video.name
                                        this.videoPath = listOfSelectedFiles?.firstOrNull()?.filePath.toString()
                                        getCloudConfig()
                                    } else {
                                        postType = CreateMediaType.post.name
                                        this.imagePathList = arrayListOf(listOfSelectedFiles?.firstOrNull()?.filePath.toString())
                                        getCloudConfig()
                                    }
                                } else {
                                    onBackPressed()
                                }
                            } else {
                                val imagePathList = it.getStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST)

                                if (!imagePathList.isNullOrEmpty()) {
                                    this.imagePathList = imagePathList
                                    getCloudConfig()
                                } else {
                                    onBackPressed()
                                }
                            }
                        }
                        CreateMediaType.post_video.name, CreateMediaType.sponty_video.name -> {
                            val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                            val audioPath = it.getStringExtra(INTENT_EXTRA_AUDIO_PATH)
                            if (!videoPath.isNullOrEmpty()) {
                                this.videoPath = videoPath
                                if (audioPath != null) {
                                    this.audioPath = audioPath
                                    mergeVideoAndAudio()
                                }
                                getCloudConfig()
                                binding.playButton.isVisible = true
                            } else {
                                onBackPressed()
                            }
                        }
                        CreateMediaType.reels.name, CreateMediaType.reels_video.name -> {
                            val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                            val audioPath = it.getStringExtra(INTENT_EXTRA_AUDIO_PATH)
                            if (!videoPath.isNullOrEmpty()) {
                                this.videoPath = videoPath
                                if (audioPath != null) {
                                    this.audioPath = audioPath
                                    mergeVideoAndAudio()
                                }
                                getCloudConfig()
                                binding.tvUsername.text = resources.getString(R.string.label_upload_reels)
                                binding.btnDone.text = resources.getString(R.string.label_upload_reel)
                                binding.playButton.isVisible = true
                            } else {
                                onBackPressed()
                            }
                        }
                        else -> {
                            onBackPressed()
                        }
                    }
                } else {
                    onBackPressed()
                }

                binding.musicRelativeLayout.visibility = if (postType.equals(CreateMediaType.post.name) ||  postType.equals(CreateMediaType.post_video.name)) View.GONE else View.VISIBLE
                binding.venueTagView.visibility = if (postType.equals(CreateMediaType.post.name) ||  postType.equals(CreateMediaType.post_video.name)) View.GONE else View.VISIBLE

                if (it.hasExtra(INTENT_EXTRA_AUDIO_RESPONSE)) {
                    musicResponse = it.getParcelableExtra(INTENT_EXTRA_AUDIO_RESPONSE) ?: return
                    binding.musicSubtitleItemAppCompatTextView.visibility = View.VISIBLE
                    binding.musicTitleItemAppCompatTextView.text = musicResponse.songTitle
                    binding.musicSubtitleItemAppCompatTextView.text = musicResponse.songSubtitle
                    binding.musicTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))
                } else {
                    binding.musicTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.angle_double_small_right_1, null))
                    binding.musicTitleItemAppCompatTextView.setText(resources.getString(R.string.add_music))
                    binding.musicSubtitleItemAppCompatTextView.visibility = View.GONE
                }
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun mergeVideoAndAudio() {
        val dire: File = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES)
        val outputFileName = "merge_video_file.mp4"

        val outgoerDir = File(dire, "outgoer")

        Timber.tag("VideoTrim").d("Movies/outgoer: %s", outgoerDir.exists())
        Timber.tag("VideoTrim").d("Movies/outgoer: %s", outgoerDir.path)
        if (outgoerDir.exists()) {
            val make = outgoerDir.mkdirs()
            Timber.tag("VideoTrim").d("Dire Created: %s", make)
            MediaScannerConnection.scanFile(
                this, arrayOf<String>(outgoerDir.path), null
            ) { path, uri ->
                Timber.tag("VideoTrim").i("Scanned $path:")
                Timber.tag("VideoTrim").i("-> uri=$uri")
//                        videoPath  = it.thumbnail
            }
            val filePath = outgoerDir.let {
                Utils.getConvertedFile(
                    it.absolutePath, outputFileName
                )
            }
            Timber.tag("VideoTrim").d("filePath :$filePath")
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

        Timber.tag("VideoTrim").d("DIRECTORY_DOWNLOADS: %s", dir?.exists())
        if (dir?.exists() == false) {
            val make = dir.mkdir()
            Timber.tag("VideoTrim").d("Dire Created: %s", make)
        }
        val mp4UriAfterTrim = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
            Utils.getConvertedFile(
                it.absolutePath, outputFileName
            )
        }

//        Timber.tag("VideoTrim").d("result result: %s", result)
        Timber.tag("VideoTrim").d("result: %s", mp4UriAfterTrim?.path)
        Timber.tag("VideoTrim").d("result: %s", File(mp4UriAfterTrim?.path).exists())

        Handler(Looper.getMainLooper()).postDelayed({
            val cmd = arrayOf(
                "-y", "-i", videoPath, "-i", audioPath, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", mp4UriAfterTrim?.path
            )
            Thread {
                val result: Int = FFmpeg.execute(cmd)
                Timber.tag("VideoTrim").d("result: %s", result)
                if (result == 0) {
                    Timber.tag("VideoTrim").i("result: Success")
                    videoPath = mp4UriAfterTrim?.path.toString()
                    Timber.tag("VideoTrim").i("videoPath: $videoPath")
                    Timber.tag("VideoTrim").i("videoPath: ${mp4UriAfterTrim?.path}")
                } else if (result == 255) {
                    Timber.tag("VideoTrim").d("result: Canceled")
                } else {
                    Timber.tag("VideoTrim").e("result: Failed")
                }
            }.start()
        }, 10)

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

    private fun getCloudConfig() {
        thumbnailPath = videoPath
        if (postType == CreateMediaType.post.name) {
            if (imagePathList.isNotEmpty()) {
                Glide.with(this).load(imagePathList.last()).into(binding.ivSelectedMedia)
            }
        } else if (postType == CreateMediaType.post_video.name) {
            Glide.with(this).load(videoPath).into(binding.ivSelectedMedia)
        } else if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
            Glide.with(this).load(videoPath).into(binding.ivSelectedMedia)
        }

        listenToViewEvents()
        listenToViewModel()
        addNewPostViewModel.getCloudFlareConfig()
    }

    private fun listenToViewEvents() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.peopletagRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(AddTagToPostActivity.launchActivity(this, taggedPeopleHashMap), REQUEST_CODE_TAG_PEOPLE)
        }.autoDispose()

        binding.locationRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(VenueListActivity.getIntent(this), REQUEST_CODE_TAG_VENUE)
        }.autoDispose()

        binding.venueTagRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(AddLocationActivity.getIntent(this), REQUEST_CODE_LOCATION)
        }

        binding.musicRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                AddMusicActivity.getIntent(this@AddNewPostInfoActivity, mediaType = postType), REQUEST_CODE_MUSIC
            )
        }

        binding.ivSelectedMedia.throttleClicks().subscribeAndObserveOnMainThread {

        }.autoDispose()

        binding.btnDone.throttleClicks().subscribeAndObserveOnMainThread {

            val dir: File = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES)

            val outgoerDir = File(dir, "outgoer")
            if (outgoerDir.isDirectory && outgoerDir.exists()) {

                for (c in outgoerDir.listFiles()) {
                    c.delete()
                }
            }

            MediaScannerConnection.scanFile(
                this, arrayOf<String>(videoPath), null
            ) { path, uri ->
                Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
            }

            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                val createReelRequest: CreateReelRequest = createReelsData()
                val intent = Intent(this, UploadingPostReelsService::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE, postType)
                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH, videoPath)
                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                intent.putExtra("createReelRequest", createReelRequest)
                startService(intent)


                startActivity(HomeActivity.getIntent(this@AddNewPostInfoActivity))
//                finish()
            } else {
                if (listOfSelectedFiles?.isNotEmpty() == true) {
                    val createPostData: CreatePostRequest = createPostData()
                    val intent = Intent(this, UploadingPostReelsService::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                    intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE, postType)
                    intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH, listOfSelectedFiles?.firstOrNull()?.filePath)
                    intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                    intent.putExtra("createPostData", createPostData)
                    startService(intent)

                    startActivity(HomeActivity.getIntent(this@AddNewPostInfoActivity))
                } else {
                    if (postType == CreateMediaType.post.name) {
                        cloudFlareConfig?.let {
                            uploadMediaToCloudFlare(it)
                        } ?: addNewPostViewModel.getCloudFlareConfig()
                    } else {
                        val createPostData: CreatePostRequest = createPostData()
                        val intent = Intent(this, UploadingPostReelsService::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE, postType)
                        intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH, videoPath)
                        intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                        intent.putExtra("createPostData", createPostData)
                        startService(intent)

                        startActivity(HomeActivity.getIntent(this@AddNewPostInfoActivity))
//                    finish()
                    }
                }

            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        addNewPostViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewModel.AddNewPostViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is AddNewPostViewModel.AddNewPostViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is AddNewPostViewModel.AddNewPostViewState.UploadImageCloudFlareSuccess -> {
                    imageUrlList.add(it.imageUrl)
                    imagePathList.removeFirstOrNull()
                    cloudFlareConfig?.let { cConfig ->
                        uploadMediaToCloudFlare(cConfig)
                    }
                }
                is AddNewPostViewModel.AddNewPostViewState.UploadVideoCloudFlareSuccess -> {
                    videoId = it.videoId
                    thumbnail = it.thumbnail

                    createPost()
                }
                is AddNewPostViewModel.AddNewPostViewState.CreatePostSuccessMessage -> {
                    startActivityWithFadeInAnimation(HomeActivity.getIntent(this))
                    finish()
                }
                is AddNewPostViewModel.AddNewPostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is AddNewPostViewModel.AddNewPostViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is AddNewPostViewModel.AddNewPostViewState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is AddNewPostViewModel.AddNewPostViewState.FollowerList -> {
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is AddNewPostViewModel.AddNewPostViewState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.etCaption.setText(it.descriptionString)
                    binding.etCaption.setSelection(binding.etCaption.text.toString().length)
                }
                else -> {}
            }
        }.autoDispose()
    }


    private fun mentionTagPeopleViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.rlFollowerList.visibility == View.GONE) {
            binding.rlFollowerList.visibility = View.VISIBLE
        } else if (!isVisibility && binding.rlFollowerList.visibility == View.VISIBLE) {
            binding.rlFollowerList.visibility = View.GONE
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnDone.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnDone.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun uploadMediaToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (postType == CreateMediaType.post.name) {
            if (imagePathList.isNotEmpty()) {
                buttonVisibility(true)
                val imageFile = File(imagePathList.first())
                addNewPostViewModel.uploadImageToCloudFlare(this, cloudFlareConfig, imageFile)
            } else {
                createPost()
            }
        } else if (postType == CreateMediaType.post_video.name) {
            buttonVisibility(true)
            compressVideoFile()
        }
    }


    private fun compressVideoFile() {
        val videoUris = listOf(Uri.fromFile(File(videoPath)))

        var sizeM = File(videoPath).absoluteFile.length()
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies, subFolderName = "outgoer"
                ),
                configureWith = Configuration(
                    quality = VideoQuality.VERY_HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = false,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {

                    }

                    override fun onStart(index: Int) {

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        cloudFlareConfig?.let {
                            addNewPostViewModel.uploadVideoToCloudFlare(
                                this@AddNewPostInfoActivity, it, File(videoPath)
                            )
                        }
                        var progressDialogFragment = ProgressDialogFragment.newInstance()
                        progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                            progressDialogFragment.dismiss()
                        }
                        progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment.javaClass.name)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_HASHTAG) {
                data?.let {
                    if (it.hasExtra(HASHTAG_LIST)) {
                        binding.hashtagItemAppCompatTextView.text = it.getStringExtra(HASHTAG_LIST)
                        binding.nextHashtag.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))

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
                                binding.peopletagItemAppCompatTextView.text = TextUtils.join(", ", taggedPeopleList)
                                binding.nextPeopleTag.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))

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
                                binding.locationItemAppCompatTextView.text = TextUtils.join(", ", taggedPeopleList)
                                binding.nextLocation.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))

                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_LOCATION) {
                data?.let {
                    if (it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE) && it.hasExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE) && it.hasExtra(
                            AddPostLocationActivity.INTENT_EXTRA_LOCATION
                        ) && it.hasExtra(AddPostLocationActivity.INTENT_PLACE_ID)
                    ) {
                        val latitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE, 0.toDouble())
                        val longitude = it.getDoubleExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE, 0.toDouble())
                        val location = it.getStringExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION)
                        val placeId = it.getStringExtra(AddPostLocationActivity.INTENT_PLACE_ID)

                        if (!placeId.isNullOrEmpty()) {
                            this.placeId = placeId
                        }
                        if (latitude != 0.toDouble() && longitude != 0.toDouble() && !location.isNullOrEmpty()) {
                            this.latitude = latitude
                            this.longitude = longitude
                            this.location = location
                            binding.venueLocationItemAppCompatTextView.text = location
                            binding.nextVenueTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))

                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_MUSIC) {
                data?.let {
                    if (it.hasExtra("INTENT_ADD_MUSIC_INFO")) {
                        var musicInfo = it.getParcelableExtra<MusicResponse>("INTENT_ADD_MUSIC_INFO")
                        binding.musicTitleItemAppCompatTextView.text = musicInfo?.songTitle

                        binding.musicSubtitleItemAppCompatTextView.visibility = View.VISIBLE
                        binding.musicSubtitleItemAppCompatTextView.text = musicInfo?.songSubtitle

                        binding.musicTagAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_close_24, null))
                    }
                }
            }
        }
    }

    private fun createPost() {
        val request = CreatePostRequest()
        request.caption = binding.etCaption.text.toString()
        request.latitude = latitude
        request.longitude = longitude
        request.postLocation = location
        request.placeId = placeId
        request.hashTags = hashtags

        val taggedPeopleList = taggedPeopleHashMap.keys
        val taggedVenueList = taggedVenueHashMap.keys
        request.tagPeople = if (taggedPeopleList.isNotEmpty()) {
            TextUtils.join(",", taggedPeopleList)
        } else {
            null
        }

        request.tagVenue = if (taggedVenueList.isNotEmpty()) {
            TextUtils.join(",", taggedVenueList)
        } else {
            null
        }

        if (postType == CreateMediaType.post.name) {
//            request.postImage = imageUrlList
            request.type = 1
            addNewPostViewModel.createPost(request)
        } else if (postType == CreateMediaType.post_video.name) {
            request.uid = arrayListOf(UidRequest(videoId = videoId))
            request.type = 2
            addNewPostViewModel.createPost(request)
        }
    }

    private fun createPostData(): CreatePostRequest {
        val request = CreatePostRequest()
        request.caption = binding.etCaption.text.toString()
        request.latitude = latitude
        request.longitude = longitude
        request.postLocation = location
        request.placeId = placeId
        request.hashTags = hashtags
        request.descriptionTag = addNewPostViewModel.descriptionTagsInfo()
        val taggedPeopleList = taggedPeopleHashMap.keys
        val taggedVenueList = taggedVenueHashMap.keys
        request.tagPeople = if (taggedPeopleList.isNotEmpty()) {
            TextUtils.join(",", taggedPeopleList)
        } else {
            null
        }

        request.tagVenue = if (taggedVenueList.isNotEmpty()) {
            TextUtils.join(",", taggedVenueList)
        } else {
            null
        }
        return request
    }

    private fun createReelsData(): CreateReelRequest {
        val request = CreateReelRequest()
        request.caption = binding.etCaption.text.toString()
        request.latitude = latitude
        request.longitude = longitude
        request.reelLocation = location
        request.hashTags = binding.hashtagItemAppCompatTextView.text.toString()
        request.placeId = placeId
        request.descriptionTag = addNewPostViewModel.descriptionTagsInfo()

        if (this::musicResponse.isInitialized) {
            if (musicResponse != null){
                request.musicId = musicResponse.id
            }
        }

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
        return request
    }
    override fun onBackPressed() {
        finish()
    }
}