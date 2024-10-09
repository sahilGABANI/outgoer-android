package com.outgoer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.event.model.GoogleMentionRequest
import com.outgoer.api.story.StoryRepository
import com.outgoer.api.story.model.StoryRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.cloudFlareGetVideoUploadBaseUrl
import com.outgoer.base.extension.cloudFlareImageUploadBaseUrl
import com.outgoer.base.extension.cloudFlareVideoUploadBaseUrl
import com.outgoer.base.extension.getCommonPhotoFileName
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.create_story.model.SelectedMedia
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StoryUploadingService : Service() {
    companion object {
        const val LIST_OF_STORY_DATA = "LIST_OF_STORY_DATA"
        const val RETRY_OPTION = "RETRY_OPTION"
        const val FILE_PATH = "FILE_PATH"
        var isStop = false
    }

    private var listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
    private var VideoUid: String? = null
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var TAG = "StoryUploadingService"
    private var counter: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    lateinit var storyRepository: StoryRepository

    @Inject
    lateinit var cloudFlareRepository: CloudFlareRepository

    override fun onCreate() {
        super.onCreate()
        OutgoerApplication.component.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            onHandleIntent(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onHandleIntent(intent: Intent?) {
        intent?.let {
            getCloudFlareConfig()
            listOfSelectedFiles =
                it.getParcelableArrayListExtra<SelectedMedia>(LIST_OF_STORY_DATA) as ArrayList<SelectedMedia>
            Timber.tag(TAG).i("list :${Gson().toJson(listOfSelectedFiles)}")
            counter = 0
            Observable.timer(5000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                callBroadCastMangerByType()
            }
            isStop = false

            val sendIntent1 = Intent("IconDisable")
            sendIntent1.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
            LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent1)

            println("Retry option: " + intent.getBooleanExtra(RETRY_OPTION, false))
            if (listOfSelectedFiles.filter { it.isVideo() }.isEmpty() || intent?.getBooleanExtra(
                    RETRY_OPTION,
                    false
                ) == true
            ) {
                val sendIntent = Intent("ShowUploading")
                sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
            } else {
//                val sendIntent = Intent("ShowCompressing")
                val sendIntent = Intent("ShowUploading")
                sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
            }
        }
    }

    private fun callBroadCastMangerByType() {
        if (listOfSelectedFiles.isNotEmpty()) {
            if (counter < listOfSelectedFiles.size) {
                if (listOfSelectedFiles[counter].isVideo()) {
                    if (listOfSelectedFiles[counter].mergeAudioVideoPath.isNullOrEmpty() && listOfSelectedFiles[counter].mergeAudioImagePath.isNullOrEmpty()) {
                        compressVideoFile(listOfSelectedFiles[counter].filePath)
                    } else if (!listOfSelectedFiles[counter].mergeAudioImagePath.isNullOrEmpty()) {
                        listOfSelectedFiles[counter].mergeAudioImagePath?.let { compressVideoFile(it) }
                    } else {
                        listOfSelectedFiles[counter].mergeAudioVideoPath?.let {
                            compressVideoFile(it)
                        }
                    }
                } else {
                    counter++
                    callBroadCastMangerByType()
                }
            } else {
                Timber.tag(TAG).i("All Is Compressing Done ")
                Timber.tag(TAG).i("Now Uploading Start")
                Timber.tag(TAG).i("list Of Uploading ${Gson().toJson(listOfSelectedFiles)}")
                val sendIntent = Intent("ShowUploading")
                sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
                counter = 0
                upLoading()
            }

        }
    }

    private fun upLoading() {
        if (listOfSelectedFiles.isNotEmpty()) {
            if (counter < listOfSelectedFiles.size) {
                if (listOfSelectedFiles[counter].isVideo()) {
                    val sendIntent = Intent("ShowUploading")
                    sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
                    cloudFlareConfig?.let {
                        uploadVideoToCloudFlare(
                            applicationContext,
                            it,
                            File(listOfSelectedFiles[counter].compressFilePath.toString())
                        )
                    } ?: getCloudFlareConfig()

                } else {
                    cloudFlareConfig?.let {
                        uploadImageToCloudFlare(
                            applicationContext, it, File(listOfSelectedFiles[counter].filePath)
                        )
                    } ?: getCloudFlareConfig()
                }
            } else {
                val sendIntent = Intent("showProcessing")
                sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
                Timber.tag(TAG).i("All Is Uploading in cloudFlare")
                Timber.tag(TAG).i("list Of Uploading ${Gson().toJson(listOfSelectedFiles)}")
                counter = 0
                processing()
            }
        }

    }

    private fun processing() {
        if (listOfSelectedFiles.isNotEmpty()) {
            if (counter < listOfSelectedFiles.size) {
                if (listOfSelectedFiles[counter].isVideo()) {
                    val storyRequest = StoryRequest(
                        uid = listOfSelectedFiles[counter].uid,
                        image = null,
                        type = 2,
                        musicId = listOfSelectedFiles[counter].musicResponse?.id,
                        venue_mention = if (listOfSelectedFiles[counter].location != null) (listOfSelectedFiles[counter].location?.id
                            ?: 0).toString() else null,
                    )

                    if (listOfSelectedFiles[counter].googleLocation != null) {
                        val googlePlaces = listOfSelectedFiles[counter].googleLocation
                        val googleMentionRequest = GoogleMentionRequest(
                            rating = googlePlaces?.reviewAvg,
                            name = googlePlaces?.name,
                            venueAddress = googlePlaces?.venueAddress,
                            avatar = googlePlaces?.avatar
                        )
                        storyRequest.googleMention = arrayListOf(googleMentionRequest)
                    }

                    createStory(storyRequest)


                } else {
                    val storyRequest = StoryRequest(
                        uid = null,
                        image = listOfSelectedFiles[counter].imageFromCloudFlare,
                        type = 1,
                        musicId = listOfSelectedFiles[counter].musicResponse?.id,
                        venue_mention = if (listOfSelectedFiles[counter].location != null) (listOfSelectedFiles[counter].location?.id
                            ?: 0).toString() else null,
                    )

                    if (listOfSelectedFiles[counter].googleLocation != null) {
                        val googlePlaces = listOfSelectedFiles[counter].googleLocation
                        val googleMentionRequest = GoogleMentionRequest(
                            rating = googlePlaces?.reviewAvg,
                            name = googlePlaces?.name,
                            venueAddress = googlePlaces?.venueAddress,
                            avatar = googlePlaces?.avatar
                        )
                        storyRequest.googleMention = arrayListOf(googleMentionRequest)
                    }

                    createStory(storyRequest)
                }
            } else {
                Timber.tag(TAG).i("All Story Creating Done")
                val sendIntent = Intent("showFinish")
                sendIntent.putExtra(FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
            }
        }

    }

    private fun compressVideoFile(videoPath: String) {
        Timber.tag("VideoFail")
            .d("StoryUploadingService compressVideoFile -> videoPath: $videoPath")
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        Timber.tag("VideoFail")
            .d("StoryUploadingService compressVideoFile -> videoUris: $videoUris")
        var url: Uri? = null
        MediaScannerConnection.scanFile(
            applicationContext, arrayOf<String>(videoPath), null
        ) { path, uri ->
            Timber.tag("UploadingPostReelsService").i("Scanned $path:")
            Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
            Timber.tag("VideoFail")
                .d("StoryUploadingService compressVideoFile -> onSuccess scanFile Scanned $path & uri=$uri")
            url = uri
        }

        Timber.tag(TAG).i("VideoCompress SuccessFullyDone")
        Timber.tag(TAG).i("VideoCompress Path :$videoPath")
        listOfSelectedFiles[counter].compressFilePath = if (url != null) videoPath else videoPath
        listOfSelectedFiles[counter].isCompress = true
        counter++
        callBroadCastMangerByType()
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun uploadVideoToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        videoFile: File
    ) {

        Timber.tag(TAG).i("uploadVideoToCloudFlare")

//        val videoTempPathDir = context.getExternalFilesDir("OutgoerVideos")?.path
//        val fileName = getCommonVideoFileName(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
//        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
//        val finalImageFile = videoFile.copyTo(videoCopyFile)


        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.videoKey)

        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, videoFile)
            .subscribeOn(Schedulers.io()).flatMap {
            cloudFlareRepository.getUploadVideoDetails(it, authToken)
        }.observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribeOnIoAndObserveOnMainThread({
                listOfSelectedFiles[counter].uid = it.uid
                listOfSelectedFiles[counter].videoIsUploaded = true
                counter++
                upLoading()
            }, { throwable ->
                val sendIntent = Intent("cancelUploading")
                sendIntent.putExtra(LIST_OF_STORY_DATA, listOfSelectedFiles)
                sendIntent.putExtra(
                    UploadingPostReelsService.INTENT_EXTRA_POST_TYPE,
                    CreateMediaType.story.name
                )

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                Timber.e(throwable)
            })
    }

    fun uploadImageToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        imageFile: File
    ) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName =
            getCommonPhotoFileName(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart).doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            listOfSelectedFiles[counter].imageFromCloudFlare =
                                variants.firstOrNull()
                            listOfSelectedFiles[counter].imageIsUploaded = true
                            counter++
                            upLoading()
                        } else {
//                            handleCloudFlareMediaUploadError(response.errors)
                        }
                    } else {
//                        handleCloudFlareMediaUploadError(response.errors)
                    }
                } else {
//                    handleCloudFlareMediaUploadError(response.errors)
                }
            }, { throwable ->
                val sendIntent = Intent("cancelUploading")
                sendIntent.putExtra(LIST_OF_STORY_DATA, listOfSelectedFiles)
                sendIntent.putExtra(
                    UploadingPostReelsService.INTENT_EXTRA_POST_TYPE,
                    CreateMediaType.story.name
                )

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
            })
    }

    fun getCloudFlareConfig() {
        Timber.tag(TAG).i("getCloudFlareConfig")
        cloudFlareRepository.getCloudFlareConfig().doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({ response ->
                Timber.tag(TAG).i("getCloudFlareConfig success")
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        this.cloudFlareConfig = cloudFlareConfig
                    } else {

                    }
                }
            }, { throwable ->
                val sendIntent = Intent("cancelUploading")
                sendIntent.putExtra(LIST_OF_STORY_DATA, listOfSelectedFiles)
                sendIntent.putExtra(
                    UploadingPostReelsService.INTENT_EXTRA_POST_TYPE,
                    CreateMediaType.story.name
                )

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                Timber.e(throwable)
            })
    }

    fun stopUploading() {
        isStop = true
    }

    private fun getVideoStatusCheckAPI() {
        if (cloudFlareConfig != null) {
            val apiUrl =
                cloudFlareGetVideoUploadBaseUrl.format(cloudFlareConfig?.accountId, VideoUid)
            val authToken = "Bearer ".plus(cloudFlareConfig?.videoKey)
            cloudFlareRepository.getUploadVideoStatus(apiUrl, authToken)
                .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
                .subscribeOnIoAndObserveOnMainThread({
                    Timber.tag(TAG).i("Video Status ${it.result?.status?.state}")
                    if (it.result?.status?.state != "ready") {
                        Observable.timer(5000, TimeUnit.MILLISECONDS)
                            .subscribeAndObserveOnMainThread {
                                getVideoStatusCheckAPI()
                            }
                    } else {
                        Observable.timer(3000, TimeUnit.MILLISECONDS)
                            .subscribeAndObserveOnMainThread {
//                            val sendIntent = Intent("HideProcessingShowFinished")
//                            sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
//                            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
//                            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
//
//                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                            }
                    }
                }, {
                    Timber.tag(TAG).e(it)
                })
        }
    }

    fun createStory(storyRequest: StoryRequest) {
        storyRepository.createStory(storyRequest).doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({
                it.data?.let { story ->
                    counter++
                    processing()
                }
            }, { throwable ->
                val sendIntent = Intent("cancelUploading")
                sendIntent.putExtra(LIST_OF_STORY_DATA, listOfSelectedFiles)
                sendIntent.putExtra(
                    UploadingPostReelsService.INTENT_EXTRA_POST_TYPE,
                    CreateMediaType.story.name
                )

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                Timber.tag(TAG).e(throwable)
            })
    }
}