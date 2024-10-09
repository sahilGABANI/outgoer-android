package com.outgoer.ui.suggested

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.profile.model.SuggestedUserActionState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivitySuggestedUsersBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.suggested.view.SuggestedUserAdapter
import com.outgoer.ui.suggested.viewmodel.SuggestedUsersViewModel
import com.outgoer.ui.suggested.viewmodel.SuggestedUsersViewState
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SuggestedUsersActivity : BaseActivity() {

    companion object {
        val INTENT_USER_TYPE = "INTENT_USER_TYPE"
        fun getIntent(context: Context): Intent {
            val intent = Intent(context, SuggestedUsersActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }

        fun getIntentWithData(context: Context, venueOwner: String): Intent {
            val intent = Intent(context, SuggestedUsersActivity::class.java)
            intent.putExtra(INTENT_USER_TYPE, venueOwner)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    private lateinit var userType: String

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SuggestedUsersViewModel>
    private lateinit var suggestedUsersViewModel: SuggestedUsersViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivitySuggestedUsersBinding
    private lateinit var suggestedUserAdapter: SuggestedUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuggestedUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        suggestedUsersViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
        listenToViewEvents()
        userType = intent?.getStringExtra(INTENT_USER_TYPE) ?: ""
    }

    private fun initUI() {

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this@SuggestedUsersActivity)
            }.autoDispose()

        binding.etSearch.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    suggestedUsersViewModel.resetSearchInfo(it.toString())
                } else {
                    suggestedUsersViewModel.resetSearchInfo("")
                }
//                UiUtils.hideKeyboard(this@AddLocationActivity)
            }, {
                Timber.e(it)
            }).autoDispose()


        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this@SuggestedUsersActivity)
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (this::userType.isInitialized) {
                if (userType.isNotEmpty()) {
                    if (userType == MapVenueUserType.VENUE_OWNER.type) {
                        startActivityWithDefaultAnimation(HomeActivity.getIntentWithData(this,userType))
                        finish()
                    } else {
                        startActivityWithDefaultAnimation(HomeActivity.getIntent(this))
                        finish()
                    }
                } else {
                    startActivityWithDefaultAnimation(HomeActivity.getIntent(this))
                    finish()
                }
            } else {
                startActivityWithDefaultAnimation(HomeActivity.getIntent(this))
                finish()
            }
        }.autoDispose()

        suggestedUserAdapter = SuggestedUserAdapter(this)
        suggestedUserAdapter.apply {
            suggestedUserActionState.subscribeAndObserveOnMainThread {
                when (it) {
                    is SuggestedUserActionState.FollowButtonClick -> {
                        suggestedUsersViewModel.followUnfollowUser(it.outgoerUser)
                    }
                    is SuggestedUserActionState.UserProfileClick -> {
                        if (it.outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == it.outgoerUser.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        this@SuggestedUsersActivity, 0, it.outgoerUser.id ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@SuggestedUsersActivity, it.outgoerUser.id
                                )
                            )
                        }
                    }
                }
            }.autoDispose()
        }
        binding.rvSuggestedUser.apply {
            layoutManager = LinearLayoutManager(this@SuggestedUsersActivity, LinearLayoutManager.VERTICAL, false)
            adapter = suggestedUserAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                suggestedUsersViewModel.loadMore(if(binding.etSearch.text.toString().isNullOrEmpty()) "" else binding.etSearch.text.toString())
                            }
                        }
                    }
                }
            })
        }
        suggestedUsersViewModel.resetSearchInfo("")
    }

    private fun listenToViewModel() {
        suggestedUsersViewModel.suggestedUserViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is SuggestedUsersViewState.LoadingState -> {

                }
                is SuggestedUsersViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is SuggestedUsersViewState.LoadSuggestedUserList -> {
                    suggestedUserAdapter.listOfSuggestedUsers = it.listOfSuggestedUser
                }
                is SuggestedUsersViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
            }
        }
    }
}