package com.outgoer.ui.story

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.PopupMenu
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatSendMessageRequest
import com.outgoer.api.chat.model.MessageType
import com.outgoer.api.story.model.StoriesResponse
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.story.model.ViewStoryRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.hideKeyboard
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentStoryListBinding
import com.outgoer.ui.create_story.viewmodel.StoryViewModel
import com.outgoer.ui.create_story.viewmodel.StoryViewState
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.story.storylibrary.StoriesProgressView
import com.outgoer.ui.story.view.EmojiAdapter
import com.outgoer.utils.Utility.prefetchedUrls
import com.outgoer.cache.VideoPrefetch
import com.petersamokhin.android.floatinghearts.HeartsView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates


class StoryListFragment : BaseFragment(), StoriesProgressView.StoriesListener {

    private var _binding: FragmentStoryListBinding? = null
    private val binding get() = _binding!!

    lateinit var resources: MutableList<StoriesResponse>
    private var counter = 0
    private var position = 0
    private var storyListResponse: StoryListResponse? = null
    private lateinit var emojiAdapter: EmojiAdapter
    var size by Delegates.notNull<Int>()
    private var visible = false

    private lateinit var stories: StoriesResponse
    private lateinit var videoPlayer: ExoPlayer

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoryViewModel>
    private lateinit var storyViewModel: StoryViewModel

    private lateinit var videoPrefetch: VideoPrefetch
    private lateinit var storyListContext: Context

    companion object {
        var onClick: ((Int) -> Unit?)? = null
        var onBackPress: ((Int) -> Unit?)? = null
        const val POSITION = "POSITION"
        const val STORY_LIST_RESPONSE = "STORY_LIST_RESPONSE"

        @JvmStatic
        fun newInstance(
            onClick: (Int) -> Unit,
            onBackPress: (Int) -> Unit,
            number: Int,
            storyListResponse: StoryListResponse
        ): StoryListFragment {
            this.onClick = onClick
            this.onBackPress = onBackPress
            val storyListFragment = StoryListFragment()
            val bundle = Bundle().apply {
                putInt(POSITION, number)
                putParcelable(STORY_LIST_RESPONSE, storyListResponse)
            }
            storyListFragment.arguments = bundle
            return storyListFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        storyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        storyListContext = view.context
        videoPrefetch = VideoPrefetch(
            view.context,
            viewLifecycleOwner.lifecycleScope
        )
        position = arguments?.getInt(POSITION) ?: 0
        storyListResponse = arguments?.getParcelable<StoryListResponse>(STORY_LIST_RESPONSE)
        Timber.tag("StoryListFragment").i("Fragment $position is Open")
        Timber.tag("StoryListFragment").i("isVisible :$isVisible")


        initUIInfo()
        listenToViewModel()
        initPlayer()
    }


    private fun listenToViewModel() {
        storyViewModel.storyViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is StoryViewState.ErrorMessage -> {
                    requireActivity().showLongToast(it.errorMessage)
                }

                is StoryViewState.SuccessMessage -> {
                    requireActivity().showLongToast(it.successMessage)
                    resources.removeAt(counter)
                    if (resources.isNotEmpty()) {
                        if (resources.size > counter) {
                            binding.stories.setStoriesCount(resources.size)
                            size = resources.size
                            binding.stories.setStoriesListener(this)

                            stories = if (resources[counter].type != 1) {
                                binding.stories.setStoryDuration(50000L)
                                resources[counter].videoUrl?.let { videoUrl ->
                                    startVideo(videoUrl)
                                }
                                resources[counter]
                            } else {
                                resources[counter].image?.let { image ->
                                    binding.stories.setStoryDuration(5000L)
                                    showImage(image)
                                }
                                resources[counter]
                            }

                            binding.stories.startStories(counter)
                        } else {
                            onClick?.let { onClick -> onClick(position) }
                        }
                    } else {
                        onClick?.let { onClick -> onClick(position) }
                    }
                }

                is StoryViewState.NewChatMessage -> {
                    requireActivity().showLongToast(it.chatMessageInfo.toString())

                }

                is StoryViewState.GetConversation -> {
                    storyViewModel.sendNewMessage(
                        ChatSendMessageRequest(
                            senderId = loggedInUserCache.getUserId() ?: 0,
                            receiverId = stories.userId,
                            conversationId = it.conversationId,
                            fileType = MessageType.Text,
                            message = binding.sendMessageAppCompatEditText.text.toString(),
                            fileUrl = "",
                            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatarUrl,
                            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.name,
                            chatType = "chat",
                            thumbnail = "",
                            groupName = "",
                            storyId = stories.id,
                            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username
                        )
                    )

                    binding.sendMessageAppCompatEditText.text?.clear()
                    requireActivity().hideKeyboard()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun prefetchInitialStoryVideos() {
        IntRange(0, resources.size).forEach { index ->
            if (index >= resources.size) return@forEach
            resources[index].let { item ->
                if (item.type != 1) {
                    val videoUrl = item.videoUrl
                    if (!videoUrl.isNullOrEmpty() && prefetchedUrls.add(videoUrl)) {
                        videoPrefetch.prefetchHlsVideo(Uri.parse(videoUrl))
                    }
                }
            }
        }
    }

/*    private val onTouchListener: View.OnTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    binding.stories.pause()
                    Handler(Looper.myLooper()!!).post {
                        Jzvd.goOnPlayOnPause()
                    }
                    return false
                }

                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    binding.stories.resume()
//                    Jzvd.goOnPlayOnResume()
                    Handler(Looper.myLooper()!!).post {
                        Jzvd.goOnPlayOnResume()
                    }
                    return limit < now - pressTime
                }
            }
            return false
        }
    }*/

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    private fun initUIInfo() {

        binding.sendMessageHorizontalScrollView.setOnScrollChangeListener { view, i, i2, i3, i4 ->
            println("i: $i")
            println("i2: $i2")
            println("i3: $i3")
            println("i4: $i4")

            if (i > 0) {
                if (videoPlayer != null)
                    videoPlayer.pause()

                binding.stories.pause()
            } else {
                binding.stories.resume()

                if (videoPlayer != null)
                    videoPlayer.play()
            }
        }


        val listOfEmoji = arrayListOf(0x1F602, 0x1F49C, 0x1F622, 0x1F621, 0x1F62E, 0x1F44A, 0x1F525)
        emojiAdapter = EmojiAdapter(requireContext()).apply {
            emojiActionState.subscribeAndObserveOnMainThread {
                val drawable: Drawable? = when (listOfEmoji.indexOf(it)) {
                    0 ->  ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_1, null)
                    1 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_2, null)
                    2 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_3, null)
                    3 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_4, null)
                    4 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_5, null)
                    5 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_6, null)
                    6 -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_7, null)
                    else -> ResourcesCompat.getDrawable(storyListContext.resources, R.drawable.emoji_1, null)
                }

                val bitmap = (drawable as BitmapDrawable).bitmap

                val model = HeartsView.Model(
                    it,                         // Unique ID of this image, used for Rajawali materials caching
                    bitmap                     // Bitmap image
                )

                binding.heartsView.emitHeart(model)
                binding.sendMessageAppCompatEditText.setText(getEmojiByUnicode(it))

                if(stories.conversationId == null || stories.conversationId == 0) {
                    storyViewModel.getConversation(stories.userId)
                } else {
                    storyViewModel.sendNewMessage(
                        ChatSendMessageRequest(
                            senderId = loggedInUserCache.getUserId() ?: 0,
                            receiverId = stories.userId,
                            conversationId = stories.conversationId,
                            fileType = MessageType.Text,
                            message = getEmojiByUnicode(it),
                            fileUrl = "",
                            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatarUrl,
                            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.name,
                            chatType = "chat",
                            thumbnail = "",
                            groupName = "",
                            storyId = stories.id,
                            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username
                        )
                    )
                    binding.sendMessageAppCompatEditText.text?.clear()
                    requireActivity().hideKeyboard()
                }

            }
        }

        binding.emojiViewRecyclerView.apply {
            adapter = emojiAdapter
        }

        emojiAdapter.listOfEmoji = listOfEmoji

        setEventListener(
            requireActivity(),
            viewLifecycleOwner,
            KeyboardVisibilityEventListener {
                if (it) {
                    binding.stories.pauseStory(counter)

                    if ((storyListResponse?.stories?.get(counter)?.type ?: 0) != 1)
                        videoPlayer.pause()
                } else {
                    binding.stories.resumeStory(counter)

                    if ((storyListResponse?.stories?.get(counter)?.type ?: 0) != 1)
                        videoPlayer.play()
                }
            })

        binding.addStory.isVisible = loggedInUserCache.getUserId()
            ?.equals(storyListResponse?.stories?.firstOrNull()?.userId) == true
        binding.ivMenu.isVisible = loggedInUserCache.getUserId()
            ?.equals(storyListResponse?.stories?.firstOrNull()?.userId) == true
        startStories(storyListResponse?.stories)

        binding.skip.setOnLongClickListener {
            if (videoPlayer != null)
                videoPlayer.pause()

            binding.stories.pause()
            true
        }

        binding.skip.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                if (videoPlayer != null)
                    videoPlayer.play()

                binding.stories.resume()
            }

            false
        }



        binding.reverse.setOnLongClickListener {
            if (videoPlayer != null)
                videoPlayer.pause()

            binding.stories.pause()
            true
        }

        binding.reverse.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                if (videoPlayer != null)
                    videoPlayer.play()

                binding.stories.resume()
            }

            false
        }

        binding.reverse.throttleClicks().subscribeAndObserveOnMainThread {
            if (counter == 0 && position != 0) {
                onBackPress?.let { onBackPress -> onBackPress(position) }
            } else {
                binding.stories.reverse()
            }
        }.autoDispose()
        binding.skip.throttleClicks().subscribeAndObserveOnMainThread {
            binding.stories.skip()
        }.autoDispose()

        binding.viewersLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
//            binding.stories.pause()
            binding.stories.pauseStory(counter)

            if ((storyListResponse?.stories?.get(counter)?.type ?: 0) != 1)
                videoPlayer.pause()

            val storyUserBottomSheet =
                StoryUserBottomSheet.newInstance(resources[counter].id).apply {
                    storyActionState.subscribeAndObserveOnMainThread {
                        binding.stories.resumeStory(counter)

                        if ((storyListResponse?.stories?.get(counter)?.type ?: 0) != 1)
                            videoPlayer.play()
                    }
                }
            storyUserBottomSheet.isCancelable = false
            storyUserBottomSheet.show(childFragmentManager, StoryUserBottomSheet.Companion::class.java.name)
        }.autoDispose()

        Glide.with(requireContext()).load(storyListResponse?.avatar)
            .placeholder(R.drawable.venue_placeholder).circleCrop().into(binding.ivProfile)

        binding.ivMenu.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoPlayer != null)
                videoPlayer.pause()

            binding.stories.pause()
            view?.let { showPopup(binding.ivMenu) }
        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().finish()
        }.autoDispose()

        binding.ivProfile.throttleClicks().subscribeAndObserveOnMainThread {
            if ((loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) == (storyListResponse?.stories?.get(counter)?.userId ?: 0)
            ) {
                startActivity(DeeparEffectsActivity.getIntent(requireContext(), true))
            } else {
                if (storyListResponse?.userType.equals(MapVenueUserType.VENUE_OWNER.type)) {
                    startActivity(
                        NewVenueDetailActivity.getIntent(
                            requireContext(),
                            0,
                            storyListResponse?.id ?: 0
                        )
                    )
                } else {
                    startActivity(
                        NewOtherUserProfileActivity.getIntent(
                            requireContext(),
                            storyListResponse?.stories?.get(counter)?.userId ?: 0
                        )
                    )
                }
            }
        }.autoDispose()

        binding.sendSelectionImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!binding.sendMessageAppCompatEditText.text.toString().isNullOrEmpty()) {
                if (stories.conversationId == null || stories.conversationId == 0) {
                    storyViewModel.getConversation(stories.userId)
                } else {
                    storyViewModel.sendNewMessage(
                        ChatSendMessageRequest(
                            senderId = loggedInUserCache.getUserId() ?: 0,
                            receiverId = stories.userId,
                            conversationId = stories.conversationId,
                            fileType = MessageType.Text,
                            message = binding.sendMessageAppCompatEditText.text.toString(),
                            fileUrl = "",
                            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatarUrl,
                            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.name,
                            chatType = "chat",
                            thumbnail = "",
                            groupName = "",
                            storyId = stories.id,
                            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username
                        )
                    )
                    binding.sendMessageAppCompatEditText.text?.clear()
                    requireActivity().hideKeyboard()
                }
            }
        }

        binding.locationLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {

            if (resources[counter].mentions?.get(0)?.user?.userType.equals(MapVenueUserType.VENUE_OWNER.type)) {
                startActivity(
                    NewVenueDetailActivity.getIntent(
                        requireContext(),
                        0,
                        resources[counter].mentions?.get(0)?.user?.id ?: 0
                    )
                )
            } else {
                val navigationIntentUri = Uri.parse(
                    "google.navigation:q=${resources[counter].mentions?.get(0)?.user?.name}, ${
                        resources[counter].mentions?.get(0)?.user?.venueAddress
                    }"
                )

                val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }

        }.autoDispose()

//        binding.progressLinearLayout.setOnTouchListener(object : View.OnTouchListener, GestureDetector.SimpleOnGestureListener() {
//            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
//                println("Touch here")
//                return true
//            }
//
//            override fun onDoubleTap(e: MotionEvent): Boolean {
//                println("Double click event")
//                return true
//            }
//        })
    }


    private fun showPopup(v: View) {
        val contextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenuOverlapAnchor)

        var popupMenu: PopupMenu? = PopupMenu(contextThemeWrapper, v).apply {
            setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item?.itemId) {
                    R.id.delete -> {
                        if (videoPlayer != null)
                            videoPlayer.play()

                        binding.stories.resume()


                        if (this@StoryListFragment::videoPlayer.isInitialized) {
                            videoPlayer.stop()
                        }
                        val infoId = resources[counter].id
                        storyViewModel.deleteStory(infoId)
                        true
                    }

                    else -> false
                }
            })
            setOnDismissListener {
                println("Dismiss")

                if (videoPlayer != null)
                    videoPlayer.play()

                binding.stories.resume()
            }
            inflate(R.menu.delete_menu)
            show()
        }
    }


    private fun startStories(storyDetailResponse: ArrayList<StoriesResponse>?) {
        if (!storyDetailResponse.isNullOrEmpty()) {
            counter = 0
            resources = mutableListOf()
            resources.addAll(storyDetailResponse)
            prefetchInitialStoryVideos()
            binding.stories.setStoriesCount(resources.size)
            size = resources.size
            binding.stories.setStoriesListener(this)
//            binding.stories.pauseStory(0)
            if ((loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) != storyListResponse?.id
            ) {
                storyViewModel.viewStory(ViewStoryRequest(resources[counter].id))
            }
            stories = if (resources[counter].type != 1) {
                binding.stories.setStoryDuration(60000L)
                resources[counter].videoUrl?.let {
                    startVideo(it)
                }
                resources[counter]
            } else {
                resources[counter].image?.let {
                    binding.stories.setStoryDuration(7000L)
                    showImage(it)
                }
                resources[counter]
            }

            binding.stories.startStories()

            binding.locationLinearLayout.isVisible = (((resources[counter].mentions?.size
                ?: 0) > 0))
            binding.locationAppCompatTextView.isSelected =
                (((resources[counter].mentions?.size ?: 0) > 0))

            if ((resources[counter].mentions?.size ?: 0) > 0)
                binding.locationAppCompatTextView.text =
                    if (resources[counter].mentions?.get(0)?.user?.venueAddress.isNullOrEmpty()) "${
                        resources[counter].mentions?.get(0)?.user?.name
                    }" else "${resources[counter].mentions?.get(0)?.user?.name}, ${
                        resources[counter].mentions?.get(
                            0
                        )?.user?.venueAddress
                    }"
            binding.llBottomSendLayout.isVisible =
                (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) != resources[counter].userId
            binding.viewersLinearLayout.isVisible =
                (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) == resources[counter].userId
            binding.emojiViewRecyclerView.isVisible =
                (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) != resources[counter].userId
            binding.totalViewsAppCompatTextView.text =
                "${resources[counter].totalViews ?: 0} Viewers"
        }

    }

    private fun showImage(imageOrVideoUrl: String) {
        Handler(Looper.myLooper()!!).post { binding.stories.pause() }
        binding.progressBar.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE
        binding.image.visibility = View.VISIBLE
        binding.tvPostDateTime.text = resources[counter].humanReadableTime
        binding.tvUsername.text = storyListResponse?.username

        if (resources[counter].music != null) {
            binding.tvMusicName.isVisible = true
            Glide.with(requireContext())
                .asGif()
                .load(R.raw.music_reels)
                .into(binding.ivMusicLyricsWav)
            binding.ivMusicLyricsWav.isVisible = true
            binding.tvMusicName.text = resources[counter].music?.songTitle
        } else {
            binding.tvMusicName.isVisible = false
            binding.ivMusicLyricsWav.isVisible = false
        }
        if (imageOrVideoUrl.isNotBlank() && imageOrVideoUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(imageOrVideoUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.stories.setStoryDuration(5000L)
                        binding.progressBar.visibility = View.GONE
                        Handler(Looper.myLooper()!!).post {
                            binding.stories.resume()
                        }
                        return false
                    }

                }).into(binding.image)
        }
    }

    private fun startVideo(url: String) {
        binding.stories.pauseStory(counter)
        binding.progressBar.visibility = View.VISIBLE
        binding.playerView.visibility = View.VISIBLE
        binding.image.visibility = View.GONE
        binding.tvPostDateTime.text = resources[counter].humanReadableTime
        binding.tvUsername.text = storyListResponse?.username
//        binding.time.text = createdAt
        if (resources[counter].music != null) {

            binding.tvMusicName.isVisible = true
            Glide.with(requireContext())
                .asGif()
                .load(R.raw.music_reels)
                .into(binding.ivMusicLyricsWav)
            binding.ivMusicLyricsWav.isVisible = true
            binding.tvMusicName.text = resources[counter].music?.songTitle
        } else {
            binding.tvMusicName.isVisible = false
            binding.ivMusicLyricsWav.isVisible = false
        }
        Timber.tag("StoryVideo").d("url: $url")
        buildMediaSource((url).toUri())
    }

    private fun initPlayer() {
        try {
            videoPlayer = ExoPlayer.Builder(requireContext()).build()
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            binding.playerView.player = videoPlayer
            val audioAttributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
            videoPlayer.setAudioAttributes(audioAttributes, true)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val mediaSource =
                HlsMediaSource.Factory(Outgoer.cacheDataSourceFactory).createMediaSource(MediaItem.fromUri(mUri))
            videoPlayer.setMediaSource(mediaSource)
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
            videoPlayer.addListener(object : Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == PlaybackStateCompat.STATE_PLAYING && visible) {
                        binding.stories.resumeStory(counter)
                        binding.stories.setDurationStory(videoPlayer.duration, counter)
                        binding.progressBar.isVisible = false
                        Timber.tag("StoryListFragment").i("video Duration :${videoPlayer.duration}")
                        Timber.tag("StoryListFragment").i("mUri :${mUri}")
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.tag("StoryListFragment").e(error)
                    videoPlayer.stop()
                }
            })
        } catch (e: java.lang.Exception) {
            Timber.tag("StoryListFragment").i("Error : $e")
        }
    }

/*    private fun videoSize(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this.context, uri)
        val mVideoWidth =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
        val mVideoHeight =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()

        Timber.tag("StoryListFragment").i("mVideoWidth :$mVideoWidth")
        Timber.tag("StoryListFragment").i("mVideoHeight :$mVideoHeight")
    }*/

    override fun onNext() {
        if (this::videoPlayer.isInitialized) {
            videoPlayer.stop()
        }
        if (this::resources.isInitialized) {
            counter++
            if ((loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                    ?: 0) != storyListResponse?.id
            ) {
                storyViewModel.viewStory(ViewStoryRequest(resources[counter].id))
            }

            if (counter < size) {
                stories = if (resources[counter].type == 2) {
                    resources[counter].videoUrl?.let {
                        startVideo(
                            it
                        )
                    }
                    resources[counter]
                } else {
                    resources[counter].image?.let {
                        binding.stories.setStoryDuration(5000L)
                        showImage(it)
                    }
                    resources[counter]
                }

                binding.locationLinearLayout.isVisible =
                    (((resources[counter].mentions?.size ?: 0) > 0))
                binding.locationAppCompatTextView.isSelected =
                    (((resources[counter].mentions?.size ?: 0) > 0))
                binding.totalViewsAppCompatTextView.text =
                    "${resources[counter].totalViews ?: 0} Viewers"

                if ((resources[counter].mentions?.size ?: 0) > 0)
                    binding.locationAppCompatTextView.text =
                        if (resources[counter].mentions?.get(0)?.user?.venueAddress.isNullOrEmpty()) "${
                            resources[counter].mentions?.get(0)?.user?.name
                        }" else "${resources[counter].mentions?.get(0)?.user?.name}, ${
                            resources[counter].mentions?.get(
                                0
                            )?.user?.venueAddress
                        }"
            }
        }
    }

    override fun onPrev() {
        if (this::videoPlayer.isInitialized) {
            videoPlayer.stop()
        }

        if (this::resources.isInitialized) {
            if (counter > 0) {
                counter--
                if (counter < size) {
                    if (counter < 0) return
                    stories = if (resources[counter].type == 1) {
                        resources[counter].image?.let { showImage(it) }
                        resources[counter]
                    } else {
                        resources[counter].videoUrl?.let { startVideo(it) }
                        resources[counter]
                    }


                    binding.totalViewsAppCompatTextView.text =
                        "${resources[counter].totalViews ?: 0} Viewers"

                    binding.locationLinearLayout.isVisible =
                        (((resources[counter].mentions?.size ?: 0) > 0))
                    binding.locationAppCompatTextView.isSelected =
                        (((resources[counter].mentions?.size ?: 0) > 0))

                    if ((resources[counter].mentions?.size ?: 0) > 0)
                        binding.locationAppCompatTextView.text =
                            if (resources[counter].mentions?.get(0)?.user?.venueAddress.isNullOrEmpty()) "${
                                resources[counter].mentions?.get(0)?.user?.name
                            }" else "${resources[counter].mentions?.get(0)?.user?.name}, ${
                                resources[counter].mentions?.get(
                                    0
                                )?.user?.venueAddress
                            }"
                }
            }
        }
    }

    override fun onComplete() {
        Timber.tag("StoryListFragment").i("Story Finish")
        onClick?.let { onClick -> onClick(position) }
    }

    override fun onDestroy() {
        Timber.tag("StoryListFragment").i("$position onDestroy")
        binding.stories.destroy()
        videoPlayer.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("StoryListFragment").i("$position OnResume")
        Timber.tag("StoryListFragment").i("isVisible :$isVisible")
        visible = isVisible
        if (isVisible && resources.isNotEmpty()) {
            startStories(resources as ArrayList<StoriesResponse>)
        }

    }

    override fun onPause() {
        super.onPause()
        visible = false
        binding.stories.pause()
        videoPlayer.pause()
        Timber.tag("StoryListFragment").i("$position onPause")
    }
}