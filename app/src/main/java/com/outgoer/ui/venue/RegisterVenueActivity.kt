package com.outgoer.ui.venue

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.util.Linkify
import android.util.Patterns
import android.view.View
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
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityRegisterVenueBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.ui.addvenuemedia.viewmodel.AddVenueMediaViewModel
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.home.create.AddHashtagBottomSheet
import com.outgoer.ui.post.AddNewPostActivity
import com.outgoer.ui.post.ImagePickerBottomSheet
import com.outgoer.ui.venue.update.VenueUpdateActivity
import com.outgoer.utils.FileUtils
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

class RegisterVenueActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterVenueBinding
    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var avatar = ""
    private var GET_GALLERY = 1996

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddVenueMediaViewModel>
    private lateinit var addVenueMediaViewModel: AddVenueMediaViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var hashtags = ""
    private var taggedPeopleHashMap = HashMap<Int, String?>()

    private var addedHashtagArrayList = ArrayList<String>()
    private var taggedVenueHashMap = HashMap<Int, String?>()

    private var cloudFlareConfig: CloudFlareConfig? = null

    private var registerVenueRequest: RegisterVenueRequest? = null
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"

        fun getIntent(context: Context): Intent {
            return Intent(context, RegisterVenueActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityRegisterVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addVenueMediaViewModel = getViewModelFromFactory(viewModelFactory)


        registerVenueRequest = loggedInUserCache.getVenueRequest()

        initUI()
        addVenueMediaViewModel.getCloudFlareConfig()
        listenToViewModel()

        setUpDetails()
    }

    private fun setUpDetails() {
        registerVenueRequest?.let {
            binding.nameAppCompatEditText.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.name)
            binding.phoneNoAppCompatEditText.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.phone)
            binding.emailAppCompatEditText.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.email)
            binding.passwordAppCompatEditText.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.password)
            binding.cPasswordAppCompatEditText.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.password)
            binding.tvHashtag.text = Editable.Factory.getInstance().newEditable(registerVenueRequest?.venueTags)

            Glide.with(this@RegisterVenueActivity).load(registerVenueRequest?.avatar).placeholder(R.drawable.venue_placeholder)
                .into(binding.ivMyProfile)
        }
    }

    private fun openAddHashTagBottomSheet() {
        val bottomSheet = AddHashtagBottomSheet(addedHashtagArrayList)
        bottomSheet.addedHashTagsClicks.subscribeAndObserveOnMainThread {
            bottomSheet.dismissBottomSheet()
            addedHashtagArrayList = it
            binding.tvHashtag.text = addedHashtagArrayList.joinToString()
            hashtags = binding.tvHashtag.text.toString()
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, AddHashtagBottomSheet::class.java.name)
    }

    private fun listenToViewModel() {
        addVenueMediaViewModel.addVenueMediaState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddVenueMediaViewModel.AddVenueMediaViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareSuccess -> {
                    avatar = it.mediaUrl
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareLoading -> {

                    buttonVisibility(it.isLoading)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.CheckVenue -> {
                    if (it.createVenueResponse) {
                        val registerVenueRequest = loggedInUserCache.getVenueRequest()
                        if (registerVenueRequest != null) {
                            startActivity(
                                VenueCategoryActivity.getIntent(
                                    this@RegisterVenueActivity, registerVenueRequest
                                )
                            )
                        }
                    } else{
                        showLongToast(it.successMessage)
                    }
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
        handlePathOz = HandlePathOz(this, listener)

        val termsConditionsText = getString(R.string.text_terms_conditions)
        val privacyPolicyText = getString(R.string.text_privacy_policy)
        val userAgreementText = getString(R.string.text_user_agreement)
        val legalText = getString(R.string.account_create_info, userAgreementText, privacyPolicyText, termsConditionsText)
        binding.agreeTermsCheckBox.setText(legalText)

        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(userAgreementText),
            null,
            null,
            { match, url -> BaseConstants.USER_AGREEMENT }
        )

        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(termsConditionsText),
            null,
            null,
            { match, url -> BaseConstants.TERMS_N_CONDITIONS }
        )
        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(privacyPolicyText),
            null,
            null,
            { match, url -> BaseConstants.PRIVACY_POLICY }
        )

        binding.rlHashTags.throttleClicks().subscribeAndObserveOnMainThread {
            openAddHashTagBottomSheet()
        }

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
                        checkPermissionGranted(this@RegisterVenueActivity)
                    }
                }.autoDispose()
            }
            imagePickerBottomSheet.show(supportFragmentManager, RegisterVenueActivity::class.java.name)

        }

        binding.cPasswordAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if(!binding.cPasswordAppCompatEditText.text.isNullOrEmpty() && binding.cPasswordAppCompatEditText.text.toString().equals(binding.passwordAppCompatEditText.text.toString())) {
                    binding.samePwdAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.samePwdAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }
            }.autoDispose()

        binding.passwordAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if(binding.passwordAppCompatEditText.text.toString().length >= 8) {
                    binding.minCharsAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.minCharsAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                if(binding.passwordAppCompatEditText.text.toString().contains("[A-Z]".toRegex())) {
                    binding.upperCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.upperCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                if(binding.passwordAppCompatEditText.text.toString().contains("[0-9]".toRegex())) {
                    binding.numberAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.numberAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                val special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")

                if(special.matcher(binding.passwordAppCompatEditText.text.toString()).find()) {
                    binding.specialCharCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.specialCharCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }
            }.autoDispose()


        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {

            val expression = "^[+]?[0-9]{4,15}$"
            val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(binding.phoneNoAppCompatEditText.text.toString())

            if (binding.nameAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_name))
            } else if (binding.usernameAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_username))
            } else if (binding.phoneNoAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_phone_no))
            } else if (!matcher.matches()) {
                showToast(resources.getString(R.string.label_error_v_phone_number))
            } else if (binding.emailAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_email_address))
            } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailAppCompatEditText.text.toString()).matches()) {
                showToast(resources.getString(R.string.invalid_email))
            } else if (binding.passwordAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_password))
            } else if (binding.cPasswordAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.error_v_confirm_password))
            } else if (binding.passwordAppCompatEditText.text.toString().length < 8) {
                showToast(resources.getString(R.string.password_minimum_length))
            } else if (binding.cPasswordAppCompatEditText.text.toString().length < 8) {
                showToast(resources.getString(R.string.password_minimum_length))
            } else if (!(binding.cPasswordAppCompatEditText.text.toString().equals(binding.passwordAppCompatEditText.text.toString()))) {
                showToast(resources.getString(R.string.error_v_password_n_confirm_password))
            } else if (!binding.agreeTermsCheckBox.isChecked) {
                showToast(resources.getString(R.string.msg_agree_terms))
            } else {
                var registerVenueRequest = loggedInUserCache.getVenueRequest()

                if (registerVenueRequest != null) {
                    registerVenueRequest.name = binding.nameAppCompatEditText.text.toString()
                    registerVenueRequest.username = binding.usernameAppCompatEditText.text.toString()
                    registerVenueRequest.phone = binding.phoneNoAppCompatEditText.text.toString()
                    registerVenueRequest.email = binding.emailAppCompatEditText.text.toString()
                    registerVenueRequest.password = binding.passwordAppCompatEditText.text.toString()
                    registerVenueRequest.avatar = avatar
                    registerVenueRequest.venueTags = hashtags
                    registerVenueRequest.phoneCode = binding.ccp.selectedCountryCodeAsInt.toString()
                } else {
                    registerVenueRequest = RegisterVenueRequest(
                        name = binding.nameAppCompatEditText.text.toString(),
                        username = binding.usernameAppCompatEditText.text.toString(),
                        phone = binding.phoneNoAppCompatEditText.text.toString(),
                        email = binding.emailAppCompatEditText.text.toString(),
                        password = binding.passwordAppCompatEditText.text.toString(),
                        avatar = avatar,
                        venueTags = hashtags,
                        phoneCode = binding.ccp.selectedCountryCodeAsInt.toString()
                    )
                }


                loggedInUserCache.setVenueRequest(registerVenueRequest)
                addVenueMediaViewModel.checkVenue(registerVenueRequest)

//               registerVenueRequest.venueCategory = "3,4"

            }
        }
    }

    private fun checkPermissionGrantedForCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), MY_CAMERA_PERMISSION_CODE
            )
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context).permission(Permission.READ_MEDIA_IMAGES).permission(Permission.CAMERA).request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@RegisterVenueActivity)

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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
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
        val path: String = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
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
                        addVenueMediaViewModel.uploadImageToCloudFlare(
                            this@RegisterVenueActivity, it, File(selectedImagePath), MEDIA_TYPE_IMAGE
                        )
                    }
                    Glide.with(this@RegisterVenueActivity).load(filePath).placeholder(R.drawable.venue_placeholder)
                        .error(R.drawable.venue_placeholder).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.ivMyProfile)
                }
            }
        }
    }
}