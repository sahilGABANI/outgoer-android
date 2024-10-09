package com.outgoer.ui.venue.update

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueUpdateBinding
import com.outgoer.ui.addvenuemedia.viewmodel.AddVenueMediaViewModel
import com.outgoer.ui.home.create.AddHashtagBottomSheet
import com.outgoer.ui.post.ImagePickerBottomSheet
import com.outgoer.utils.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject


class VenueUpdateActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueUpdateBinding
    private var registerVenueRequest: RegisterVenueRequest? = null
    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath:String = ""
    private var avatar  = ""

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddVenueMediaViewModel>
    private lateinit var addVenueMediaViewModel: AddVenueMediaViewModel

    private var cloudFlareConfig : CloudFlareConfig?=null
    private var hashtags = ""

    private var addedHashtagArrayList = ArrayList<String>()
    private val REQUEST_IMAGE_CAPTURE = 10001
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888

    companion object {
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueUpdateActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityVenueUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addVenueMediaViewModel =  getViewModelFromFactory(viewModelFactory)


        initUI()
        addVenueMediaViewModel.getCloudFlareConfig()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        addVenueMediaViewModel.addVenueMediaState.subscribeAndObserveOnMainThread {
            when(it){
                is AddVenueMediaViewModel.AddVenueMediaViewState.GetCloudFlareConfig ->{
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.CloudFlareConfigErrorMessage ->{
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareSuccess ->{
                    avatar = it.mediaUrl
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.ErrorMessage ->{
                    showLongToast(it.errorMessage)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareLoading ->{
                    buttonVisibility(it.isLoading)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UpdateVenueSuccess -> {
                    finish()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.continueMaterialButton.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.continueMaterialButton.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }
    private fun initUI() {

        intent?.let {
            registerVenueRequest = it.getParcelableExtra(INTENT_REGISTER_VENUE)

            binding.nameAppCompatEditText.setText(registerVenueRequest?.name)
            binding.phoneNoAppCompatEditText.setText(registerVenueRequest?.phone)

            registerVenueRequest?.phoneCode?.removePrefix("+")?.toInt()?.let { it1 -> binding.ccp.setCountryForPhoneCode(it1) }
            binding.descriptionAppCompatEditText.setText(registerVenueRequest?.description)
            binding.emailAppCompatEditText.setText(registerVenueRequest?.email)
            binding.tvHashtag.setText(registerVenueRequest?.venueTags)
            avatar = registerVenueRequest?.avatar ?: ""

            val hashTagList = registerVenueRequest?.venueTags?.split(",") ?: listOf()
            addedHashtagArrayList.addAll(hashTagList)

            Glide.with(this@VenueUpdateActivity)
                .load(registerVenueRequest?.avatar)
                .error(R.drawable.venue_placeholder)
                .into(binding.ivMyProfile)
        }


        binding.rlHashTags.throttleClicks().subscribeAndObserveOnMainThread {

            openAddHashTagBottomSheet()
        }

        handlePathOz = HandlePathOz(this, listener)

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.flProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            val imagePickerBottomSheet = ImagePickerBottomSheet.getInstance().apply {
                postCameraItemClicks.subscribeAndObserveOnMainThread {
                    if (it) {
                        dismissBottomSheet()
                        checkPermissionGrantedForCamera()
                    } else {
                        dismissBottomSheet()
                        checkPermissionGranted(this@VenueUpdateActivity)
                    }
                }.autoDispose()
            }
            imagePickerBottomSheet.show(supportFragmentManager, VenueUpdateActivity::class.java.name)

        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(avatar.isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_error_profile))

            } else if (binding.nameAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_name))
            } else if (binding.phoneNoAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_phone_no))
            } else {
                val registerVenueRequest = RegisterVenueRequest(
                    name = binding.nameAppCompatEditText.text.toString(),
                    phone = binding.phoneNoAppCompatEditText.text.toString(),
                    avatar = avatar,
                    description = binding.descriptionAppCompatEditText.text.toString(),
                    venueTags = hashtags,
                    phoneCode = binding.ccp.selectedCountryCodeWithPlus
                )
                addVenueMediaViewModel.updateVenue(registerVenueRequest)
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

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@VenueUpdateActivity)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val path: String =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
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
                        addVenueMediaViewModel.uploadImageToCloudFlare(this@VenueUpdateActivity,
                            it,
                            File(selectedImagePath),
                             MEDIA_TYPE_IMAGE
                        )
                    }
                    Glide.with(this@VenueUpdateActivity)
                        .load(filePath)
                        .placeholder(R.drawable.venue_placeholder)
                        .error(R.drawable.venue_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding.ivMyProfile)
                }
            }
        }
    }

}