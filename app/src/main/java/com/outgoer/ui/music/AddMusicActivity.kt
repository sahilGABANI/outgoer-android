package com.outgoer.ui.music

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAddMusicBinding
import com.outgoer.ui.music.view.MusicListTabAdapter
import com.outgoer.ui.music.viewmodel.MusicViewModel
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddMusicActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"

        fun getIntent(context: Context, mediaType: String, videoPath: String ?= null): Intent {
            return Intent(context, AddMusicActivity::class.java)
                .putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
                .putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
        }
    }

    private lateinit var binding : ActivityAddMusicBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MusicViewModel>
    private lateinit var musicViewModel: MusicViewModel

    private var searchString: String? = null
    private var listOfMusicCategory: ArrayList<MusicCategoryResponse> = arrayListOf()
    private var videoPath: String = ""
    private var postType: String = CreateMediaType.post_video.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityAddMusicBinding.inflate(layoutInflater)
        musicViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        videoPath = intent.getStringExtra(INTENT_EXTRA_VIDEO_PATH) ?: ""
        postType = intent.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: CreateMediaType.post_video.name

        initUI()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        musicViewModel.musicState.subscribeAndObserveOnMainThread {
            when(it) {
                is MusicViewModel.MusicInfoViewState.GetMusicCategoryList -> {
                    listOfMusicCategory = it.listOfMusicCategory as ArrayList<MusicCategoryResponse>

                    val searchViewPagerAdapter = MusicListTabAdapter(
                        this@AddMusicActivity,
                        it.listOfMusicCategory.size,
                        intent?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: "",
                        listOfMusic = it.listOfMusicCategory,
                        videoPath,
                        postType
                    )
                    binding.viewPager.isUserInputEnabled = false
                    binding.viewPager.offscreenPageLimit = it.listOfMusicCategory.size
                    binding.viewPager.adapter = searchViewPagerAdapter

                    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                        tab.text = it.listOfMusicCategory[position].categoryName
                    }.attach()
                }
                is MusicViewModel.MusicInfoViewState.LoadingState -> {}
                is MusicViewModel.MusicInfoViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("MusicInfoViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    fun getSearchInfo(): String {
        return binding.searchAppCompatEditText.text.toString()
    }

    private fun initUI() {

        binding.searchAppCompatEditText.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.clearAppCompatImageView.visibility = View.INVISIBLE
                } else {
                    binding.clearAppCompatImageView.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    searchString = it.toString()
                    val selectedMusicId = listOfMusicCategory[binding.tabLayout.selectedTabPosition].id
                    RxBus.publish(RxEvent.SearchMusic(searchString, selectedMusicId))
                } else {
                    searchString = null
                    val selectedMusicId = listOfMusicCategory[binding.tabLayout.selectedTabPosition].id
                    RxBus.publish(RxEvent.SearchMusic(searchString, selectedMusicId))
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        musicViewModel.getMusicCategory()
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }

        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
            }.autoDispose()

        binding.searchAppCompatEditText.textChanges()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.clearAppCompatImageView.visibility = View.INVISIBLE
                } else {
                    binding.clearAppCompatImageView.visibility = View.VISIBLE
                }
            }
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                //
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.clearAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.searchAppCompatEditText.setText("")
        }.autoDispose()
    }
}