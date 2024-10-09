package com.outgoer.ui.home.profile.newprofile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.engineer.linktextview.Linker
import com.engineer.linktextview.internal.OnLinkClickListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.profile.model.UpdateProfileRequest
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.venue.model.CheckInOutRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentNewMyProfileBinding
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.editprofile.EditProfileActivity
import com.outgoer.ui.followdetail.FollowDetailActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.create.CreateNewReelInfoActivity
import com.outgoer.ui.home.newReels.hashtag.PlayReelsByHashtagActivity
import com.outgoer.ui.home.profile.newprofile.setting.NewProfileSettingActivity
import com.outgoer.ui.home.profile.newprofile.view.NewMyProfileFragmentTabAdapter
import com.outgoer.ui.home.profile.view.MyReelsAdapter
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.newnotification.NewNotificationActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.post.ImagePickerBottomSheet
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.venue.update.VenueUpdateActivity
import com.outgoer.utils.FileUtils
import com.outgoer.utils.SnackBarUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates


class NewMyProfileFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = NewMyProfileFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private var loggedInUserId by Delegates.notNull<Int>()

    private var _binding: FragmentNewMyProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var myReelsAdapter: MyReelsAdapter
    private lateinit var newMyProfileFragmentTabAdapter: NewMyProfileFragmentTabAdapter
    private lateinit var handlePathOz: HandlePathOz
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var selectedImagePath: String = ""
    private  val FileTypeJPG = "image/jpg"
    private  val FileTypeJPEG = "image/jpeg"
    private  val FileTypePNG = "image/png"
    private  val FileTypeWebp = "image/webp"
    private val allImageMimeTypes = arrayOf(FileTypeJPG, FileTypeJPEG, FileTypePNG, FileTypeWebp)

    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888

    private var listOfReelsInfo: ArrayList<ReelInfo> = arrayListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        listenToViewEvents()
        listenToViewModel()
        loadProfileData(outgoerUser)
        profileViewModel.getCloudFlareConfig()

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "NewMyProfileFragmentTag") {
                lifecycleScope.launch {
                    delay(1000)
                    profileViewModel.myProfile()
                    profileViewModel.getMyReel(loggedInUserId)
                }
                binding.appBarLayout.setExpanded(true, true)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun listenToViewEvents() {

        profileViewModel.pullToRefresh(loggedInUserId)

        binding.usernameLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val switchAccountBottomSheet: SwitchAccountBottomSheet = SwitchAccountBottomSheet.newInstance()
            switchAccountBottomSheet.switchAccount.subscribeAndObserveOnMainThread {
                startActivityWithFadeInAnimation(HomeActivity.getIntent(requireContext()))
            }
            switchAccountBottomSheet.show(childFragmentManager, SwitchAccountBottomSheet.Companion::class.java.name)
        }.autoDispose()

        binding.llPostCount.throttleClicks().subscribeAndObserveOnMainThread {
            if (listOfReelsInfo.isNotEmpty()) {
                startActivity(PlayReelsByHashtagActivity.getIntent(requireContext(), listOfReelsInfo, listOfReelsInfo[0]))
            }
        }.autoDispose()

        handlePathOz = HandlePathOz(requireContext(), listener)

        newMyProfileFragmentTabAdapter = NewMyProfileFragmentTabAdapter(
            requireActivity(),
            outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type
        )
        binding.viewpager.isUserInputEnabled = false
        binding.viewpager.offscreenPageLimit = 3
        binding.viewpager.adapter = newMyProfileFragmentTabAdapter
        binding.viewpager.hackMatchParentCheckInViewPager()

        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.label_posts)
                }
                1 -> {
                    tab.text = getString(R.string.label_reels)
                }
                2 -> {
                    tab.text = getString(R.string.label_venues)
                }
            }
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        binding.ivSetting.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewProfileSettingActivity.getIntent(requireContext()))
        }

        binding.profileLinkAppCompatTextView.setOnClickListener {
            val uri = Uri.parse(outgoerUser.webLink) // missing 'http://' will cause crashed
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.profileBioAppCompatTextView.setOnMentionClickListener { _, text ->
            if (!outgoerUser.mentions.isNullOrEmpty()) {
                val tag = outgoerUser.mentions?.firstOrNull { cInfo ->
                    cInfo.username == text.toString()
                }

                if (tag != null) {
                    if (loggedInUserId != tag.mentionId) {
                        startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), tag.mentionId?: 0))
                    }
                }
            }
        }

        binding.btnEditProfile.throttleClicks().subscribeAndObserveOnMainThread {
            if (outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type) {

                val registerVenueRequest = RegisterVenueRequest(
                    outgoerUser.name,
                    outgoerUser.username,
                    outgoerUser.email,
                    outgoerUser.phone,
                    null,
                    outgoerUser.description,
                    outgoerUser.venueAddress,
                    outgoerUser.latitude,
                    outgoerUser.longitude,
                    outgoerUser.avatar,
                    outgoerUser.venueCategories,
                    outgoerUser.availibility ?: arrayListOf(),
                    outgoerUser.gallery ?: arrayListOf(),
                    phoneCode = outgoerUser.phoneCode
                )

                startActivityWithDefaultAnimation(VenueUpdateActivity.getIntent(requireContext(), registerVenueRequest))
            } else {
                startActivityWithDefaultAnimation(EditProfileActivity.getIntent(requireContext()))
            }
        }

        myReelsAdapter = MyReelsAdapter(requireContext()).apply {
            addReelsViewClick.subscribeAndObserveOnMainThread {
                checkPermissions()
            }.autoDispose()

            myReelsViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(ReelsDetailActivity.getIntent(requireContext(), it.id))
            }.autoDispose()
        }

        binding.llFollowersCount.setOnClickListener {
            startActivityWithDefaultAnimation(
                FollowDetailActivity.getIntent(
                    requireContext(),
                    loggedInUserId,
                    isFollower = true,
                    isFollowing = false,
                    isMutual = false
                )
            )
        }
        binding.llFollowingCount.setOnClickListener {
            startActivityWithDefaultAnimation(
                FollowDetailActivity.getIntent(
                    requireContext(),
                    loggedInUserId,
                    isFollower = false,
                    isFollowing = true,
                    isMutual = false
                )
            )
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            RxBus.publish(RxEvent.RefreshMyProfile)
        }.autoDispose()

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.swipeRefreshLayout.isEnabled = verticalOffset == 0
        })

        RxBus.listen(RxEvent.RefreshMyProfile::class.java).subscribeOnIoAndObserveOnMainThread({
//            profileViewModel.myProfile()
//            profileViewModel.pullToRefresh(loggedInUserId)
        }, {
            Timber.e(it)
        }).autoDispose()

        binding.ivNotification.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewNotificationActivity.getIntent(requireContext()))
        }

        binding.ivMyProfile.throttleClicks().subscribeAndObserveOnMainThread {
//            checkPermissionGranted(requireContext())

            val imagePickerBottomSheet = ImagePickerBottomSheet.getInstance().apply {
                postCameraItemClicks.subscribeAndObserveOnMainThread {
                    if (it) {
                        dismissBottomSheet()
                        checkPermissionGrantedForCamera()
                    } else {
                        dismissBottomSheet()
                        checkPermissionGranted(requireContext())
                    }
                }.autoDispose()
            }
            imagePickerBottomSheet.show(childFragmentManager, NewMyProfileFragment::class.java.name)
        }.autoDispose()
    }

    private fun checkPermissionGrantedForCamera() {
        if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), MY_CAMERA_PERMISSION_CODE
            )
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            } else {
                showToast(getString(R.string.msg_permission_denied))
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val path: String =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun listenToViewModel() {
        profileViewModel.profileViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is ProfileViewModel.ProfileViewState.GetMyReelInfo -> {
                    listOfReelsInfo = it.listOfReelsInfo as ArrayList<ReelInfo>

                    profileViewModel.loadMoreMyReel(loggedInUserId)
                }
                is ProfileViewModel.ProfileViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ProfileViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is ProfileViewModel.ProfileViewState.LoadingState -> {
                }
                is ProfileViewModel.ProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ProfileViewModel.ProfileViewState.GetMyReelInfo -> {
                    myReelsAdapter.listOfDataItems = it.listOfReelsInfo
                }
                is ProfileViewModel.ProfileViewState.MyProfileData -> {
                    loadProfileData(it.outgoerUser)
                }
                ProfileViewModel.ProfileViewState.LogoutSuccess -> {
                }
                is ProfileViewModel.ProfileViewState.DeactivateProfile -> {
                }
                is ProfileViewModel.ProfileViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ProfileViewModel.ProfileViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                }

                is ProfileViewModel.ProfileViewState.UploadMediaCloudFlareSuccess -> {
                    val request = UpdateProfileRequest(
                        image = it.mediaUrl,
                    )
                    profileViewModel.uploadProfile(request)
                }

                is ProfileViewModel.ProfileViewState.UploadMediaCloudFlareLoading -> {
                   binding.progressBar.isVisible = it.isLoading
                }
                ProfileViewModel.ProfileViewState.SuccessCheckOut -> {
                    profileViewModel.myProfile()
                }
                else -> {}
            }
        }.autoDispose()
    }


    private fun loadProfileData(outgoer: OutgoerUser) {
        this.outgoerUser = outgoer

        if (outgoer.venue != null) {
            binding.venueCheckOutLayout.visibility = View.VISIBLE
            Glide.with(this)
                .load(outgoer.venue.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.ivVenueProfile)
            binding.tvVenue.text = outgoer.venue.name.toString()
            binding.btnCheckOut.throttleClicks().subscribeAndObserveOnMainThread {
                outgoer.venue.id?.let { id ->
                    profileViewModel.checkInOutVenue(CheckInOutRequest(id, 0))
                }
            }.autoDispose()
        } else {
            binding.venueCheckOutLayout.visibility = View.GONE
        }

        binding.tvName.text = outgoerUser.username
        binding.fullNameAppCompatTextView.isVisible = !outgoerUser.name.isNullOrEmpty()
        binding.fullNameAppCompatTextView.text = outgoerUser.name
        binding.profileBioAppCompatTextView.visibility = if(outgoerUser.about.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.profileBioAppCompatTextView.text = outgoerUser.about
        binding.ivVerified.isVisible = outgoerUser.profileVerified == 1

        binding.marqueeText.isVisible = !outgoer.broadcastMessage.isNullOrEmpty()
        binding.marqueeText.text = outgoer.broadcastMessage

        if (outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type) {
            binding.profileBioAppCompatTextView.text = outgoerUser.description

            outgoerUser.gallery?.let {
                val galleryUrl = if((outgoerUser.gallery?.size ?: 0) > 0) outgoer.gallery?.get(0)?.media else null
                Glide.with(this)
                    .load(galleryUrl)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .error(R.drawable.ic_chat_user_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.ivMyProfile)
            }
        }
        else {
            binding.profileBioAppCompatTextView.text = outgoerUser.about
            val galleryUrl = outgoerUser.gallery?.size
            Glide.with(this)
                .load(outgoerUser.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.ivMyProfile)
        }

        if (outgoerUser.otherMutualFriend ?: 0 >= 1) {
            val builder = StringBuilder()
            binding.tvOtherMName.text = builder.append("and ")
                .append("${outgoerUser.otherMutualFriend?.prettyCount() ?: 0}").append(" ")
                .append("others")
        }
        outgoerUser.mutualFriend?.forEach {

            val builder = StringBuilder()
            try {
                if (outgoerUser.mutualFriend!!.size > 2) {

                    val textInfo = builder.append("Followed by ")
                        .append("${outgoerUser.mutualFriend!!.get(0).name} ")
                        .append(", ")
                        .append("${outgoerUser.mutualFriend!!.get(1).name}")

                    val android = textInfo

                    val androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "", outgoerUser.mutualFriend?.get(1)?.name ?: "")

                    Linker.Builder()
                        .content(android.toString())
                        .textView(binding.tvMName)
                        .links(androidRules)
                        .linkColor(ContextCompat.getColor(requireContext(), R.color.white))
                        .addOnLinkClickListener(onLinkClickListener)
                        .apply()

                    if (outgoerUser.mutualFriend!!.size < 2) {

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(0).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnail)
                            .view.setImageResource(R.drawable.grey_semi_transparent)

                    } else {

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(0).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnail)
                            .view.setImageResource(R.drawable.grey_semi_transparent)

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(1).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnailSec)
                            .view.setImageResource(R.drawable.grey_semi_transparent)
                    }


                } else if (outgoerUser.mutualFriend!!.isNotEmpty() && outgoerUser.mutualFriend!!.size <= 2) {


                    val androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "")

                    val android = builder.append("Followed by ")
                        .append("${outgoerUser.mutualFriend!!.get(0).name} ")
                        .append(", ")
                        .append("${outgoerUser.mutualFriend!!.get(1).name}")


                    Linker.Builder()
                        .content(android.toString())
                        .textView(binding.tvMName)
                        .links(androidRules)
                        .linkColor(ContextCompat.getColor(requireContext(),R.color.white))
                        .addOnLinkClickListener(onLinkClickListener)
                        .apply()

                    if (outgoerUser.mutualFriend!!.size < 2) {

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(0).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnail)
                            .view.setImageResource(R.drawable.grey_semi_transparent)
                    } else {

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(0).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnail)
                            .view.setImageResource(R.drawable.grey_semi_transparent)

                        Glide.with(requireContext())
                            .load(outgoerUser.mutualFriend!!.get(1).avatar)
                            .placeholder(R.drawable.ic_chat_user_placeholder)
                            .error(R.drawable.ic_chat_user_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(binding.ivThumbnailSec)
                            .view.setImageResource(R.drawable.grey_semi_transparent)
                    }


                }
            } catch (e: Exception) {
            }

        }


        binding.tvPostCount.text = "${outgoerUser.totalReels?.prettyCount() ?: 0}"
        binding.tvFollowersCount.text = "${outgoerUser.totalFollowers?.prettyCount() ?: 0}"
        binding.tvFollowingCount.text = "${outgoerUser.totalFollowing?.prettyCount() ?: 0}"
        binding.profileLinkAppCompatTextView.paintFlags = binding.profileLinkAppCompatTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        if (!outgoerUser.webTitle.isNullOrEmpty()) {
            binding.profileLinkAppCompatTextView.visibility = View.VISIBLE
            binding.profileLinkAppCompatTextView.text = outgoerUser.webTitle
        } else if(!outgoerUser.webLink.isNullOrEmpty()) {
            binding.profileLinkAppCompatTextView.visibility = View.VISIBLE
            binding.profileLinkAppCompatTextView.text = outgoerUser.webLink
        } else {
            binding.profileLinkAppCompatTextView.visibility = View.GONE
        }

        binding.marqueeText.isSelected = true

    }

    private val onLinkClickListener = object: OnLinkClickListener {
        override fun onClick(view: View, content: String) {

            if(content.endsWith("others", true)) {
                startActivity(
                    FollowDetailActivity.getIntent(
                        requireContext(),
                        outgoerUser.id,
                        isFollower = false,
                        isFollowing = false,
                        isMutual = true
                    )
                )
            } else{
                outgoerUser.mutualFriend?.find { it.name == content }?.let {
                    startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), it.id ?: 0))
                }
            }
        }
    }

    private fun checkPermissions() {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.READ_MEDIA_VIDEO
                )
            )
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        startActivity(DeeparEffectsActivity.getIntent(requireContext()))
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

    override fun onResume() {
        super.onResume()

        if(isResumed) {
            profileViewModel.myProfile()
            profileViewModel.getMyReel(loggedInUserId)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        }

        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        startActivityWithDefaultAnimation(
                            CreateNewReelInfoActivity.launchActivity(
                                requireContext(),
                                filePath
                            )
                        )
                    }
                }
            }
        }

        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        } else if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val photo = data!!.extras!!["data"] as Bitmap?
                val uri = photo?.let { getImageUri(requireContext(), it) }
                uri?.also {
                    handlePathOz.getRealPath(it)
                }
            }
        }
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        openImagePickerForFragment()
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }
    fun openImagePickerForFragment() {
        val intent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)
        }
        intent.apply {
            type = "image/*"

            putExtra(Intent.EXTRA_MIME_TYPES, allImageMimeTypes)
            action = Intent.ACTION_GET_CONTENT
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            putExtra("return-data", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResultWithDefaultAnimation(intent, FileUtils.PICK_IMAGE)
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    selectedImagePath = filePath
                    cloudFlareConfig?.let {
                        uploadImageToCloudFlare(it)
                    } ?: profileViewModel.getCloudFlareConfig()
                }
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            profileViewModel.uploadImageToCloudFlare(requireContext(), cloudFlareConfig, File(selectedImagePath))
        }
    }


    private fun ViewPager2.hackMatchParentCheckInViewPager() {
        (getChildAt(0) as RecyclerView).clearOnChildAttachStateChangeListeners()
    }
}