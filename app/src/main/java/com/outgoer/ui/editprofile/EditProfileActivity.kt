package com.outgoer.ui.editprofile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.profile.model.UpdateProfileRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityEditProfileBinding
import com.outgoer.ui.editprofile.view.CommentTagAdapter
import com.outgoer.ui.editprofile.viewmodel.EditProfileViewModel
import com.outgoer.ui.post.ImagePickerBottomSheet
import com.outgoer.ui.venue.RegisterVenueActivity
import com.outgoer.utils.FileUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EditProfileActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, EditProfileActivity::class.java)
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<EditProfileViewModel>
    private lateinit var editProfileViewModel: EditProfileViewModel
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var isValidUsername = true
    private lateinit var commentTagPeopleAdapter: CommentTagAdapter
    private var initialListOfFollower: List<FollowUser> = listOf()
    private var mentions: ArrayList<FollowUser> = arrayListOf()
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        editProfileViewModel = getViewModelFromFactory(viewModelFactory)

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
        listenToViewModel()

        loadProfileData(outgoerUser)
        editProfileViewModel.getCloudFlareConfig()
        editProfileViewModel.getInitialFollowersList(outgoerUser.id)

    }

    private fun listenToViewEvents() {
        handlePathOz = HandlePathOz(this, listener)

        commentTagPeopleAdapter = CommentTagAdapter(this).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.etAbout.selectionStart
                val descriptionString = binding.etAbout.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()

                mentions.add(followUser)

                editProfileViewModel.searchTagUserClicked(
                    binding.etAbout.text.toString(),
                    subString,
                    followUser
                )
            }.autoDispose()
        }

        binding.rlFollowerList.apply {
            layoutManager = LinearLayoutManager(this@EditProfileActivity, LinearLayoutManager.VERTICAL, false)
            adapter = commentTagPeopleAdapter
        }

        binding.etAbout.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                Timber.tag("etAbout").i("$it")
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
                            editProfileViewModel.getFollowersList(
                                loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0,
                                lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()


        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.flProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            val imagePickerBottomSheet = ImagePickerBottomSheet.getInstance().apply {
                postCameraItemClicks.subscribeAndObserveOnMainThread {
                    if (it) {
                        dismissBottomSheet()
                        checkPermissionGrantedForCamera()
                    } else {
                        dismissBottomSheet()
                        checkPermissionGranted(this@EditProfileActivity)
                    }
                }.autoDispose()
            }
            imagePickerBottomSheet.show(supportFragmentManager, EditProfileActivity::class.java.name)

        }.autoDispose()

        binding.addLinkAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(AddExternalLinkActivity.getIntent(this@EditProfileActivity))
            finish()
        }

        binding.linkRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(AddExternalLinkActivity.getIntent(this@EditProfileActivity))
            finish()
        }

        binding.btnSave.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                cloudFlareConfig?.let {
                    uploadImageToCloudFlare(it)
                } ?: editProfileViewModel.getCloudFlareConfig()
            }
        }.autoDispose()

        binding.etUsername.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.toString() == (outgoerUser.username ?: "")) {
                    binding.progressBarUsername.visibility = View.GONE
                    binding.ivUsername.visibility = View.VISIBLE

                    isValidUsername = true
                    binding.ivUsername.setImageResource(R.drawable.ic_username_not_exist)
                } else {
                    if (it.length > 3) {
                        editProfileViewModel.checkUsername(it.toString())
                    } else {
                        binding.progressBarUsername.visibility = View.GONE
                        binding.ivUsername.visibility = View.GONE
                    }
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun checkPermissionGrantedForCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        editProfileViewModel.editProfileViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is EditProfileViewModel.EditProfileViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is EditProfileViewModel.EditProfileViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is EditProfileViewModel.EditProfileViewState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is EditProfileViewModel.EditProfileViewState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.etAbout.setText(it.descriptionString)
                    binding.etAbout.setSelection(binding.etAbout.text.toString().length)
                }
                is EditProfileViewModel.EditProfileViewState.FollowerList -> {
                    hideKeyboard()
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is EditProfileViewModel.EditProfileViewState.UploadImageCloudFlareSuccess -> {
                    val request = UpdateProfileRequest(
                        image = it.imageUrl,
                        name = binding.etName.text.toString(),
                        username = binding.etUsername.text.toString(),
                        about = binding.etAbout.text.toString()
                    )
                    editProfileViewModel.uploadProfile(request)
                }
                is EditProfileViewModel.EditProfileViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is EditProfileViewModel.EditProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    onBackPressed()
                }
                is EditProfileViewModel.EditProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is EditProfileViewModel.EditProfileViewState.CheckUsernameLoading -> {
                    if (it.isLoading) {
                        binding.progressBarUsername.visibility = View.VISIBLE
                    } else {
                        binding.progressBarUsername.visibility = View.GONE
                    }
                }
                is EditProfileViewModel.EditProfileViewState.CheckUsernameExist -> {
                    binding.ivUsername.visibility = View.VISIBLE
                    if (it.isUsernameExist == 1) {
                        isValidUsername = false
                        binding.ivUsername.setImageResource(R.drawable.ic_username_exist)
                    } else {
                        isValidUsername = true
                        binding.ivUsername.setImageResource(R.drawable.ic_username_not_exist)
                    }
                }
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

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSave.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnSave.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun loadProfileData(outgoerUser: OutgoerUser) {
        binding.etName.setText(outgoerUser.name ?: "")
        binding.etUsername.setText(outgoerUser.username ?: "")
        binding.etAbout.setText(outgoerUser.about ?: "")


        if(outgoerUser.webTitle == null && outgoerUser.webLink == null) {
            binding.addLinkAppCompatTextView.visibility = View.VISIBLE
            binding.linkRelativeLayout.visibility = View.GONE
        } else {
            binding.addLinkAppCompatTextView.visibility = View.GONE
            binding.linkRelativeLayout.visibility = View.VISIBLE
            binding.webTitleAppCompatTextView.text = outgoerUser?.webTitle
            binding.webLinkAppCompatTextView.text = outgoerUser?.webLink
        }


        Glide.with(this)
            .load(outgoerUser.avatar)
            .circleCrop()
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivMyProfile)
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etName.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.empty_name))
            isValidate = false
        }  else if (binding.etUsername.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.empty_username))
            isValidate = false
        } else if (binding.etUsername.text.toString().length < 5) {
            showToast(resources.getString(R.string.msg_username_not_length))
            isValidate = false
        } else if (!isValidUsername) {
            showToast(resources.getString(R.string.msg_username_already_taken_please_for_try_another))
            isValidate = false
        }
        return isValidate
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            editProfileViewModel.uploadImageToCloudFlare(this, cloudFlareConfig, File(selectedImagePath))
        } else {
            mentions.forEach { mention ->
                if(mention.username?.let { binding.etName.text.toString().contains(it) } == true) {
                    mentions.remove(mention)
                }
            }

            val mentionId: ArrayList<Int> = arrayListOf()
            mentions.forEach {
                mentionId.add(it.id)
            }
            val request = UpdateProfileRequest(
                name = binding.etName.text.toString(),
                username = binding.etUsername.text.toString(),
                about = binding.etAbout.text.toString(),
                mentionIds = mentionId.joinToString(separator = ",")
            )
            editProfileViewModel.uploadProfile(request)
        }
    }

    private fun checkPermissionGranted(context: Context) {
        FileUtils.openImagePicker(this@EditProfileActivity)
//        XXPermissions.with(context)
//            .permission(Manifest.permission.READ_MEDIA_IMAGES)
//            .request(object : OnPermissionCallback {
//
//                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
//                    if (all) {
//                        FileUtils.openImagePicker(this@EditProfileActivity)
//                    } else {
//                        showToast(getString(R.string.msg_permission_denied))
//                    }
//                }
//
//                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
//                    super.onDenied(permissions, never)
//                    showToast(getString(R.string.msg_permission_denied))
//                }
//            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        }

        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        } else if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val photo = data!!.extras!!["data"] as Bitmap?
                val uri = photo?.let { getImageUri(applicationContext, it) }
                uri?.also {
                    handlePathOz.getRealPath(it)
                }
            }
        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    selectedImagePath = filePath
                    Glide.with(this@EditProfileActivity)
                        .load(filePath)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding.ivMyProfile)
                }
            }
        }
    }
}