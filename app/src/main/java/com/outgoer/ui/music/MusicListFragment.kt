package com.outgoer.ui.music

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentMusicListBinding
import com.outgoer.ui.music.view.AddMusicAdapter
import com.outgoer.ui.music.viewmodel.MusicViewModel
import javax.inject.Inject


class MusicListFragment : BaseFragment() {

    private var postType: String = CreateMediaType.post_video.name
    private var _binding: FragmentMusicListBinding? = null
    private val binding get() = _binding!!
    private var musicCategoryResponse: MusicCategoryResponse? = null

    private var durationOfMedia: Int = 0

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MusicViewModel>
    private lateinit var musicViewModel: MusicViewModel
    private val exoPlayer by lazy { ExoPlayer.Builder(requireContext()).build() }

    private lateinit var addMusicAdapter: AddMusicAdapter
    private var progressBar: CircularProgressIndicator? = null
    private var mHandler: Handler? = null
    private var runnable: Runnable? = null
    private var videoPath = ""
    private var mPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        musicViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMusicListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mHandler = Handler()
        arguments?.let {
            videoPath = it.getString(ARG_EXTRA_VIDEO_PATH, "")
            postType = it.getString(INTENT_EXTRA_MEDIA_TYPE) ?: CreateMediaType.post_video.name
            musicCategoryResponse = it.getParcelable<MusicCategoryResponse>(MUSIC_CATEGORY_INFO)
            val searchString = (requireActivity() as AddMusicActivity).getSearchInfo()
            musicCategoryResponse?.id?.let {
                musicViewModel.getMusicList(categoryId = musicCategoryResponse?.id ?: 0, searchString)
            }
        }
        listenToViewEvents()
        listenToViewModel()
        runnable = Runnable { updateSeekBar() }
    }

    private fun updateSeekBar() {
        val progress: Double = exoPlayer.currentPosition.toDouble().div(durationOfMedia)
        progressBar?.progress = (progress * 100).toInt()
        runnable?.let { mHandler?.postDelayed(it, 100) }
    }

    private fun listenToViewEvents() {
        addMusicAdapter = AddMusicAdapter(requireContext()).apply {
            musicClick.subscribeAndObserveOnMainThread {
                if (it.isPlaying) {
                    if(mPosition != -1) {
                        val load: ProgressBar = binding.musicRecyclerView.getChildAt(
                            mPosition
                        ).findViewById(
                            R.id.loadMusic
                        )
                        load.visibility = View.GONE
                    }

                    mPosition = addMusicAdapter.listOfMusicInfo?.indexOf(it) ?: 0
                    progressBar = binding.musicRecyclerView.getChildAt(
                        mPosition
                    ).findViewById(
                        R.id.progressProgressBar
                    )

                    val loadMusic: ProgressBar = binding.musicRecyclerView.getChildAt(
                        mPosition
                    ).findViewById(
                        R.id.loadMusic
                    )

//                    progressBar?.max = 1000

                    val listOfMusic = addMusicAdapter.listOfMusicInfo
                    listOfMusic?.forEach { music ->
                        if (music.isPlaying && it.id != music.id) music.isPlaying = false
                    }
                    addMusicAdapter.listOfMusicInfo = listOfMusic
                    startPlayAudio(it.songFile ?: "", loadMusic)

                } else {
                    pausePlayAudio()
                }
            }

            selectMusicClick.subscribeAndObserveOnMainThread {
                if (videoPath.isNullOrEmpty()) {
                    val intent = Intent()
                    intent.putExtra("INTENT_ADD_MUSIC_INFO", it)

                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                } else {
                    if (postType == CreateMediaType.story.name) {
                        val intent = Intent()
                        intent.putExtra("INTENT_ADD_MUSIC_INFO", it)
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().finish()
                    }else {
                        startActivity(TrimMusicActivity.launchActivity(requireContext(), arguments?.getString(INTENT_EXTRA_MEDIA_TYPE, "") ?: "", videoPath, it))
                    }
                }
            }
        }

        binding.musicRecyclerView.apply {
            adapter = addMusicAdapter
        }
    }

    private fun listenToViewModel() {
        musicViewModel.musicState.subscribeAndObserveOnMainThread {
            when (it) {
                is MusicViewModel.MusicInfoViewState.LoadingState -> {}
                is MusicViewModel.MusicInfoViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is MusicViewModel.MusicInfoViewState.GetMusicList -> {
                    if (it.listOfMusic.size > 0) {
                        binding.noMusicLinearLayout.visibility = View.GONE
                        addMusicAdapter.listOfMusicInfo = it.listOfMusic
                    } else {
                        binding.noMusicLinearLayout.visibility = View.VISIBLE
                    }
                }

                else -> {}
            }
        }
    }


    private fun startPlayAudio(audioUrl: String, loadMusic: ProgressBar) {
        val mediaItem = MediaItem.fromUri(audioUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
//        updateSeekBar()
        loadMusic.visibility = View.VISIBLE

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                    //do something
                    durationOfMedia = exoPlayer.duration.toInt()
                    updateSeekBar()
                    loadMusic.visibility = View.GONE
                }
            }
        })

//        mediaPlayer?.apply {
//            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
//            setDataSource(audioUrl)
//            prepareAsync()
//
//            setOnPreparedListener { mp ->
//                mp.start()
//                durationOfMedia = mp.duration
//                updateSeekBar()
//                loadMusic.visibility = View.GONE
//
//                //current value in the text view
//            }
//            setOnCompletionListener { m ->
//                if (!m.isPlaying) {
//                    m.stop()
//                }
//            }
//        }
    }

    private fun pausePlayAudio() {
        if (exoPlayer != null) {
            exoPlayer.pause()
            exoPlayer.stop()
        }
    }

    private fun killMediaPlayer() {
        if (exoPlayer != null) {
            exoPlayer.pause()
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    override fun onStop() {
        super.onStop()
        killMediaPlayer()
    }

    override fun onResume() {
        super.onResume()
        val searchString = (requireActivity() as AddMusicActivity).getSearchInfo()
        musicCategoryResponse?.id?.let {
            musicViewModel.getMusicList(categoryId = musicCategoryResponse?.id ?: 0, searchString)
        }

        RxBus.listen(RxEvent.SearchMusic::class.java).subscribeAndObserveOnMainThread {
            if(it.musicCategoryId == musicCategoryResponse?.id) {
                musicCategoryResponse?.id?.let { _ ->
                    musicViewModel.getMusicList(categoryId = musicCategoryResponse?.id ?: 0, it.searchString)
                }
            }
        }
    }

    companion object {
        private var MUSIC_CATEGORY_INFO = "MUSIC_CATEGORY_INFO"
        private const val ARG_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"

        @JvmStatic
        fun newInstance(position: Int, mediaType: String, listOfMusic: ArrayList<MusicCategoryResponse>, videoPath: String ?= null): MusicListFragment {
            val musicListFragment = MusicListFragment()
            val bundle = Bundle()

            val musicCategory = listOfMusic[position]
            bundle.putString(INTENT_EXTRA_MEDIA_TYPE, mediaType)
            bundle.putParcelable(MUSIC_CATEGORY_INFO, musicCategory)
            bundle.putString(ARG_EXTRA_VIDEO_PATH, videoPath)
            musicListFragment.arguments = bundle
            return musicListFragment
        }
    }
}