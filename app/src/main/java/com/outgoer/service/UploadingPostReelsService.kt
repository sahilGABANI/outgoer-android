package com.outgoer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.google.gson.Gson
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.event.model.GoogleMentionRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.api.post.model.ImageList
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.api.reels.model.UidRequest
import com.outgoer.api.story.model.StoryRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.*
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

class UploadingPostReelsService : Service() {

    companion object {
        const val INTENT_EXTRA_POST_TYPE = "INTENT_EXTRA_POST_TYPE"
        const val POST_TYPE_VIDEO = "POST_TYPE_VIDEO"
        const val POST_TYPE_REELS = "POST_TYPE_REELS"
        const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        const val INTENT_EXTRA_THUMBNAIL_PATH = "INTENT_EXTRA_THUMBNAIL_PATH"
        const val INTENT_EXTRA_LIST_OF_POST = "INTENT_EXTRA_LIST_OF_POST"
        const val INTENT_VIDEO_COMPRESSING_DONE = "INTENT_VIDEO_COMPRESSING_DONE"
        var isStop = false
        var compressFilePath = ""

        fun stopUploading() {
            isStop = true
        }
    }

    private var postType: String? = null
    private var VideoUid: String? = null
    private var videoPath: String? = null
    private var thumbnailPath: String? = null
    private var createPostData: CreatePostRequest? = null
    private var createReelsData: CreateReelRequest? = null
    private var cloudFlareConfig: CloudFlareConfig? = null

    private var TAG = "UploadingPostReelsService"
    private var counter: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    lateinit var postRepository: PostRepository

    @Inject
    lateinit var cloudFlareRepository: CloudFlareRepository

    @Inject
    lateinit var reelsRepository: ReelsRepository
    private var listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()


    override fun onCreate() {
        super.onCreate()
        OutgoerApplication.component.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            onHandleIntent(intent)
        }
        getCloudFlareConfig()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onHandleIntent(intent: Intent?) {
        intent?.let {
            isStop = false

            postType = it.getStringExtra(INTENT_EXTRA_POST_TYPE)
            videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
            thumbnailPath = it.getStringExtra(INTENT_EXTRA_THUMBNAIL_PATH)
            listOfSelectedFiles = it.getParcelableArrayListExtra<SelectedMedia>(INTENT_EXTRA_LIST_OF_POST) ?: arrayListOf()

            val sendIntent = Intent("ShowReelsActivity")
            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)

            Timber.tag(TAG).i("list : ${Gson().toJson(listOfSelectedFiles)}")
            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                createReelsData = it.getParcelableExtra<CreateReelRequest>("createReelRequest")
                callBroadCastMangerByType()
            } else {
                createPostData = it.getParcelableExtra<CreatePostRequest>("createPostData")
                val sendIntent1 = Intent("ShowProgressDialog")
                sendIntent1.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                sendIntent1.putExtra(INTENT_EXTRA_VIDEO_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent1)
                }
                if (listOfSelectedFiles.isNotEmpty()) {
                    counter = 0
                    callBroadCastMangerByType()
                }
            }
        }
    }


    private fun callBroadCastMangerByType() {
        if (listOfSelectedFiles.isNotEmpty()) {
            if (counter < listOfSelectedFiles.size) {
                if (listOfSelectedFiles[counter].isVideo()) {
                    if (listOfSelectedFiles[counter].mergeAudioVideoPath.isNullOrEmpty() && listOfSelectedFiles[counter].mergeAudioImagePath.isNullOrEmpty()) {
                        compressVideoFile(listOfSelectedFiles[counter].filePath,postType)
                    } else if (!listOfSelectedFiles[counter].mergeAudioImagePath.isNullOrEmpty()) {
                        listOfSelectedFiles[counter].mergeAudioImagePath?.let { compressVideoFile(it,postType) }
                    } else {
                        listOfSelectedFiles[counter].mergeAudioVideoPath?.let {
                            compressVideoFile(it,postType)
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
                val sendIntent = Intent("HideCompressingShowUploading")
                sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                    counter = 0
                    upLoading()
                }
            }

        } else {
            if (videoPath != null) {
                val sendIntent = Intent("ShowProgressDialog")
                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
                Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
                    compressVideoFile(videoPath!!, postType)
                }
            }
        }
    }

    private fun upLoading() {
        if (listOfSelectedFiles.isNotEmpty()) {
            if (counter < listOfSelectedFiles.size) {
                if (listOfSelectedFiles[counter].isVideo()) {
                    cloudFlareConfig?.let {
                        uploadVideoToCloudFlare(
                            applicationContext, it, File(listOfSelectedFiles[counter].compressFilePath.toString())
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
                sendIntent.putExtra(StoryUploadingService.FILE_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent)
                Timber.tag(TAG).i("All Is Uploading in cloudFlare")
                Timber.tag(TAG).i("list Of Uploading ${Gson().toJson(listOfSelectedFiles)}")
                counter = 0
                processing()
            }
        }

    }


    private fun processing() {
        val sendIntent = Intent("HideUploadingShowProcessing")
        sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
        sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
        sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
        if (listOfSelectedFiles.isNotEmpty()) {
            val videoItemSize = listOfSelectedFiles.filter { it.isCheckVideo() }
            val imageItemSize = listOfSelectedFiles.filter { !it.isCheckVideo() }
            val imageVideoSize = listOfSelectedFiles.filter { it.isImageVideoCheck() }
            if (videoItemSize.isNotEmpty() && imageItemSize.isEmpty()) {
                createPostData?.type = 2
            } else if (imageItemSize.isNotEmpty() && videoItemSize.isEmpty() && imageVideoSize.isEmpty()) {
                createPostData?.type = 1
            } else {
                if(listOfSelectedFiles.size == 1 && !imageItemSize.isEmpty())
                    createPostData?.type = 2
                else
                    createPostData?.type = 3
            }
        }
        val uidList = ArrayList<UidRequest>()
        val imageList = ArrayList<ImageList>()
        listOfSelectedFiles.forEachIndexed { index, it ->
            if (it.isVideo()) {
                uidList.add(UidRequest(musicId = it.musicResponse?.id, videoId = it.uid,index))
            } else {
                it.imageFromCloudFlare?.let { it1 -> imageList.add(ImageList(it1, index)) }
            }
        }
        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            createPostData?.uid = uidList
            createPostData?.postImage = imageList
        }
        Observable.timer(5000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            createPostData?.let { it1 -> createPost(it1) }
        }

    }


    private fun compressVideoFile(videoPath: String, postType: String?) {
        Timber.tag("VideoFail").d(  "UploadingPostReelsService compressVideoFile -> videoPath: $videoPath")
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        Timber.tag("VideoFail").d(  "UploadingPostReelsService compressVideoFile -> videoUris: $videoUris")
        if (!isStop) {
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                var url: Uri? = null
                MediaScannerConnection.scanFile(
                    applicationContext, arrayOf(videoPath), null
                ) { path, uri ->
                    Timber.tag("VideoFail").d( "UploadingPostReelsService compressVideoFile -> onSuccess Scanned $path")
                    Timber.tag("VideoFail").d( "UploadingPostReelsService compressVideoFile -> onSuccess uri=$uri")
                    Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                    Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
                    url = uri

                    if (url != null) {
                        Timber.tag("VideoFail").e( "UploadingPostReelsService compressVideoFile -> onSuccess url is not null")
                        cloudFlareConfig?.let {
                            uploadVideoToCloudFlare(
                                applicationContext, it, File(path)
                            )
                        } ?: getCloudFlareConfig()
                    } else {
                        Timber.tag("VideoFail").e( "UploadingPostReelsService compressVideoFile -> onSuccess url is null")
                        cloudFlareConfig?.let {
                            uploadVideoToCloudFlare(
                                applicationContext, it, File(videoPath)
                            )
                        } ?: getCloudFlareConfig()
                    }
                }

                val sendIntent = Intent("HideCompressingShowUploading")
                sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
            } else {
                var url: Uri? = null
                MediaScannerConnection.scanFile(
                    applicationContext, arrayOf(videoPath), null
                ) { path, uri ->
                    Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                    Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
                    url = uri
                }
                listOfSelectedFiles[counter].compressFilePath = if (url != null) videoPath else videoPath
                listOfSelectedFiles[counter].isCompress = true
                counter++
                callBroadCastMangerByType()
            }
        } else {
            Timber.tag("UploadingPostReelsService").i("VideoCompress SuccessFullyDone But User Cancel Creating Post Or reels.")
            val sendIntent = Intent("cancelUploading")


            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
            sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                sendIntent.putExtra("createReelRequest", createReelsData)
            } else {
                sendIntent.putExtra("createPostData", createPostData)
            }

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
        }
        /*val t: Thread = object : Thread() {
            override fun run() {
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
                            Timber.tag("UploadingPostReelsService").i("VideoCompress percent $percent")
                            if (isStop) {
                                VideoCompressor.cancel()
                                val sendIntent = Intent("cancelUploading")

                                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                                sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                                sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                                    sendIntent.putExtra("createReelRequest", createReelsData)
                                } else {
                                    sendIntent.putExtra("createPostData", createPostData)
                                }

                                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                            }
                        }

                        override fun onStart(index: Int) {

                        }

                        override fun onSuccess(index: Int, size: Long, path: String?) {
                            Timber.tag("UploadingPostReelsService").i("VideoCompress SuccessFullyDone")
                            Timber.tag("VideoFail").d( "UploadingPostReelsService compressVideoFile -> onSuccess path: $path")
                            if (!isStop) {
                                if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                                    var url: Uri? = null
                                    MediaScannerConnection.scanFile(
                                        applicationContext, arrayOf<String>(path.toString()), null
                                    ) { path, uri ->
                                        Timber.tag("VideoFail").d( "UploadingPostReelsService compressVideoFile -> onSuccess Scanned $path")
                                        Timber.tag("VideoFail").d( "UploadingPostReelsService compressVideoFile -> onSuccess uri=$uri")
                                        Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                                        Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
                                        url = uri

                                        if (url != null) {
                                            Timber.tag("VideoFail").e( "UploadingPostReelsService compressVideoFile -> onSuccess url is not null")
                                            cloudFlareConfig?.let {
                                                uploadVideoToCloudFlare(
                                                    applicationContext, it, File(path)
                                                )
                                            } ?: getCloudFlareConfig()
                                        } else {
                                            Timber.tag("VideoFail").e( "UploadingPostReelsService compressVideoFile -> onSuccess url is null")
                                            cloudFlareConfig?.let {
                                                uploadVideoToCloudFlare(
                                                    applicationContext, it, File(videoPath)
                                                )
                                            } ?: getCloudFlareConfig()
                                        }
                                    }

                                    val sendIntent = Intent("HideCompressingShowUploading")
                                    sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                                    sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
                                    sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                                } else {
                                    var url: Uri? = null
                                    MediaScannerConnection.scanFile(
                                        applicationContext, arrayOf<String>(path.toString()), null
                                    ) { path, uri ->
                                        Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                                        Timber.tag("UploadingPostReelsService").i("-> uri=$uri")
                                        url = uri
                                    }

                                    Timber.tag(TAG).i("VideoCompress SuccessFullyDone")
                                    Timber.tag(TAG).i("VideoCompress Path :$path")
                                    listOfSelectedFiles[counter].compressFilePath = if (url != null) path else videoPath
                                    listOfSelectedFiles[counter].isCompress = true
                                    counter++
                                    callBroadCastMangerByType()
                                }
                            } else {
                                Timber.tag("UploadingPostReelsService").i("VideoCompress SuccessFullyDone But User Cancel Creating Post Or reels.")
                                val sendIntent = Intent("cancelUploading")


                                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                                sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                                sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                                    sendIntent.putExtra("createReelRequest", createReelsData)
                                } else {
                                    sendIntent.putExtra("createPostData", createPostData)
                                }

                                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                            }

                        }

                        override fun onFailure(index: Int, failureMessage: String) {
                            Timber.wtf(failureMessage)
                            Timber.tag("UploadingPostReelsService").i("VideoCompress SuccessFullyDone But User Cancel Creating Post Or reels.")

                            val sendIntent = Intent("cancelUploading")

                            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                            sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                            sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                                sendIntent.putExtra("createReelRequest", createReelsData)
                            } else {
                                sendIntent.putExtra("createPostData", createPostData)
                            }


                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                        }

                        override fun onCancelled(index: Int) {
                            Timber.tag("UploadingPostReelsService").e("compression has been cancelled")
                            Timber.tag("UploadingPostReelsService").i("VideoCompress SuccessFullyDone But User Cancel Creating Post Or reels.")
                            val sendIntent = Intent("cancelUploading")

                            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                            sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                            sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                                sendIntent.putExtra("createReelRequest", createReelsData)
                            } else {
                                sendIntent.putExtra("createPostData", createPostData)
                            }

                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                            // make UI changes, cleanup, etc
                        }
                    },
                )
            }
        }
        t.start()
        t.interrupt()*/
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun uploadVideoToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, videoFile: File) {

        Timber.tag("UploadingPostReelsService").i("uploadVideoToCloudFlare")

//        val videoTempPathDir = context.getExternalFilesDir("OutgoerVideos")?.path
//        val fileName = getCommonVideoFileName(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
//        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
//        val finalImageFile = videoFile.copyTo(videoCopyFile)


        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.videoKey)

        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, videoFile).subscribeOn(Schedulers.io()).flatMap {
            cloudFlareRepository.getUploadVideoDetails(it, authToken)
        }.observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribeOnIoAndObserveOnMainThread({

                Timber.tag("UploadingPostReelsService").i("uploadVideoToCloudFlare success")
                Timber.tag("UploadingPostReelsService").i("uid :${it.uid}")
                Timber.tag("UploadingPostReelsService").i("thumbnail :${it.thumbnail}")
                VideoUid = it.uid
//                if (!compressFilePath.isNullOrEmpty()) {
//                    File(compressFilePath).delete()
//                    MediaScannerConnection.scanFile(
//                        this, arrayOf<String>(compressFilePath), null
//                    ) { path, uri ->
//                        Timber.tag("UploadingPostReelsService").i("Scanned $path:")
//                        Timber.tag("UploadingPostReelsService").i( "-> uri=$uri")
//                        videoPath  = it.thumbnail
//                    }
//                }
                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                    val sendIntent = Intent("HideUploadingShowProcessing")
                    sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                    sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
                    sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                    createReelsData?.uid = it.uid
                    createReelsData?.let { it1 -> createReel(it1) }
                } else {
                    listOfSelectedFiles[counter].uid = it.uid
                    listOfSelectedFiles[counter].videoIsUploaded = true
                    counter++
                    upLoading()
                }
            }, { throwable ->
                val sendIntent = Intent("cancelUploading")

                sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
                sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)

                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                    sendIntent.putExtra("createReelRequest", createReelsData)
                } else {
                    sendIntent.putExtra("createPostData", createPostData)
                }

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                Timber.e(throwable)
            })
    }

    fun getCloudFlareConfig() {
        Timber.tag("UploadingPostReelsService").i("getCloudFlareConfig")
        cloudFlareRepository.getCloudFlareConfig().doOnSubscribe {}.subscribeOnIoAndObserveOnMainThread({ response ->
            Timber.tag("UploadingPostReelsService").i("getCloudFlareConfig success")
            if (response.success) {
                val cloudFlareConfig = response.data
                if (cloudFlareConfig != null) {
                    this.cloudFlareConfig = cloudFlareConfig
                }
            }
        }, { throwable ->
            val sendIntent = Intent("cancelUploading")

            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
            sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                sendIntent.putExtra("createReelRequest", createReelsData)
            } else {
                sendIntent.putExtra("createPostData", createPostData)
            }

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
            Timber.e(throwable)
        })
    }

    fun stopUploading() {
        isStop = true
    }


    fun createReel(request: CreateReelRequest) {
        Timber.tag("UploadingPostReelsService").i("Call createReel API")
        reelsRepository.createReel(request).doOnSubscribe {}.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.success) {
                Timber.tag("UploadingPostReelsService").i("Response Successful createReel")
                getVideoStatusCheckAPI()

            }
        }, { throwable ->
            Timber.e(throwable)
        })
    }

    private fun getVideoStatusCheckAPI() {
        if (cloudFlareConfig != null) {
            val apiUrl = cloudFlareGetVideoUploadBaseUrl.format(cloudFlareConfig?.accountId, VideoUid)
            val authToken = "Bearer ".plus(cloudFlareConfig?.videoKey)
            cloudFlareRepository.getUploadVideoStatus(apiUrl, authToken).observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
                .subscribeOnIoAndObserveOnMainThread({
                    Timber.tag("UploadingPostReelsService").i("Video Status ${it.result?.status?.state}")
                    if (it.result?.status?.state != "ready") {
                        Observable.timer(5000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            getVideoStatusCheckAPI()
                        }
                    } else {
                        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            val sendIntent = Intent("HideProcessingShowFinished")
                            sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)

                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                        }
                    }
                }, {
                    Timber.tag("UploadingPostReelsService").e(it)
                })
        }
    }

    fun createPost(request: CreatePostRequest) {
        postRepository.createPost(request)
            .doOnSubscribe {
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    if (listOfSelectedFiles.filter { it.isVideo() == true }.isNotEmpty()) {
                        getVideoStatusCheckAPI()
                    } else {
                        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            val sendIntent = Intent("HideProcessingShowFinished")
                            sendIntent.putExtra(INTENT_VIDEO_COMPRESSING_DONE, true)
                            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
                            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, thumbnailPath)
                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
                        }
                    }
                }
            }, { throwable ->
                Timber.e(throwable)
            })
    }


    fun uploadImageToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName = getCommonPhotoFileName(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart).doOnSubscribe {}.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.success) {
                val result = response.result
                if (result != null) {
                    val variants = result.variants
                    if (!variants.isNullOrEmpty()) {
                        listOfSelectedFiles[counter].imageFromCloudFlare = variants.firstOrNull()
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
            sendIntent.putExtra(INTENT_EXTRA_POST_TYPE, postType)
            sendIntent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            sendIntent.putExtra(INTENT_EXTRA_THUMBNAIL_PATH, thumbnailPath)
            sendIntent.putExtra(INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)

            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                sendIntent.putExtra("createReelRequest", createReelsData)
            } else {
                sendIntent.putExtra("createPostData", createPostData)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(sendIntent)
        })
    }

}