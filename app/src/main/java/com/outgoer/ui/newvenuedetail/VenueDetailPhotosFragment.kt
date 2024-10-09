package com.outgoer.ui.newvenuedetail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.AddVenueGalleryRequest
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.api.venue.model.VenueGalleryItem
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentVenueDetailPhotosBinding
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.newvenuedetail.view.VenueDetailPhotosAdapter
import com.outgoer.ui.venue.viewmodel.CreateVenueViewModel
import javax.inject.Inject

class VenueDetailPhotosFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = VenueDetailPhotosFragment()

        private const val REQUEST_CODE_EVENT = 1890
        private const val VENUE_DETAILS = "venueDetail"

        @JvmStatic
        fun newInstanceWithData(venueDetail: VenueDetail): VenueDetailPhotosFragment {
            val venueDetailPhotosFragment = VenueDetailPhotosFragment()
            val bundle = Bundle()
            bundle.putParcelable(VENUE_DETAILS, venueDetail)
            venueDetailPhotosFragment.arguments = bundle
            return venueDetailPhotosFragment
        }
    }

    private var _binding: FragmentVenueDetailPhotosBinding? = null
    private val binding get() = _binding!!
    private lateinit var venueDetailPhotosAdapter: VenueDetailPhotosAdapter
    private var venueDetail: VenueDetail? = null
    private var venueGallery: VenueGalleryItem? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateVenueViewModel>
    private lateinit var createVenueViewModel: CreateVenueViewModel
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        createVenueViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVenueDetailPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        listenToViewModel()
    }

    override fun onResume() {
        super.onResume()
        arguments?.let {
            venueDetail = it.getParcelable(VENUE_DETAILS)
            createVenueViewModel.getPhotosOfVenue(GetVenueDetailRequest(venueDetail?.id ?: 0))
            venueDetailPhotosAdapter.isMyVenue =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.id == venueDetail?.id
        }
    }

    private fun initUI() {
        arguments?.let {
            venueDetail = it.getParcelable(VENUE_DETAILS)
            binding.addPhotosMaterialButton.visibility = if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id?.equals(venueDetail?.id) == true) View.VISIBLE else View.GONE
        }

        binding.addPhotosMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermission("POST_TYPE_IMAGE")
        }

        venueDetailPhotosAdapter = VenueDetailPhotosAdapter(requireContext()).apply {
            venueDetailGalleryViewClick.subscribeAndObserveOnMainThread {
                startActivity(FullScreenActivity.getIntent(requireContext(), it.media))
            }
            deleteViewClick.subscribeAndObserveOnMainThread {
                venueGallery = it
                it.id?.let { it1 -> createVenueViewModel.removeVenuePhotoFromGallery(it1) }
            }
        }

        binding.rvPhotos.apply {
            adapter = venueDetailPhotosAdapter
            layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
        }

//        venueDetail?.let {
//            if ((it.gallery?.size ?: 0) > 0) {
//                venueDetailPhotosAdapter.listOfDataItems = it.gallery
//            }
//        }
    }

    private fun checkPermission(type: String) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO
                )
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        @Suppress("DEPRECATION")
                        startActivityForResult(
                            AddMediaEventActivity.getIntentWithData(requireContext(), type, -1),
                            REQUEST_CODE_EVENT
                        )
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)

                    XXPermissions.startPermissionActivity(requireContext(), permissions);
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_EVENT) {
                val mediaUrl = data?.getStringArrayListExtra("MEDIA_URL") ?: arrayListOf()
                val listOfPhotos = venueDetailPhotosAdapter.listOfDataItems

                if(data?.hasExtra("UID") == true) {
                    val mediaVideoUrl = data?.getStringExtra("UID") ?: ""
                    listOfPhotos?.add(VenueGalleryItem(media = mediaVideoUrl))
                    createVenueViewModel.addPhotosToVenue(AddVenueGalleryRequest(uid = arrayListOf(mediaVideoUrl)))
                } else {
                    mediaUrl.forEach {
                        listOfPhotos?.add(VenueGalleryItem(media = it))
                    }
                    createVenueViewModel.addPhotosToVenue(AddVenueGalleryRequest(mediaUrl))
                }
                venueDetailPhotosAdapter.listOfDataItems = listOfPhotos
            }
        }
    }

    private fun listenToViewModel() {
        createVenueViewModel.groupState.subscribeAndObserveOnMainThread {
            when (it) {
                is CreateVenueViewModel.GroupViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CreateVenueViewModel.GroupViewState.LoadingState -> {}
                is CreateVenueViewModel.GroupViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    createVenueViewModel.getPhotosOfVenue(GetVenueDetailRequest(venueDetail?.id ?: 0))
                    val list = venueDetailPhotosAdapter.listOfDataItems ?: arrayListOf()
                    list.add(VenueGalleryItem(media = it.mediaUrl, id = -1))
                    list.distinct()
                    venueDetailPhotosAdapter.listOfDataItems = list
                    hideShowNoData(list)
                }
                is CreateVenueViewModel.GroupViewState.SuccessDMessage -> {
                    showToast(it.successMessage)
                    val list = venueDetailPhotosAdapter.listOfDataItems ?: arrayListOf()
                    list.remove(venueGallery)
                    list.distinct()
                    venueDetailPhotosAdapter.listOfDataItems = list
                    hideShowNoData(list)
                }
                is CreateVenueViewModel.GroupViewState.ListOfVenueGallery -> {
                    venueDetailPhotosAdapter.listOfDataItems = it.listofgallery
                    hideShowNoData(it.listofgallery)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(postInfoList: ArrayList<VenueGalleryItem>) {
        if (postInfoList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
            binding.addPhotosRelativeLayout.visibility = View.VISIBLE
        } else {
            binding.llNoData.visibility = View.VISIBLE
            binding.photosAppCompatTextView.visibility = View.VISIBLE
        }
    }
}