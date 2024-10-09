package com.outgoer.ui.home.newmap.venueevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.base.BaseFragment
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentEventMediaBinding
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.temp.TempActivity

class EventMediaFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = EventMediaFragment()

        const val EVENT_INFO = "EVENT_INFO"

        @JvmStatic
        fun newInstanceWithData(eventData: EventData): EventMediaFragment {
            val eventMediaFragment = EventMediaFragment()
            val args = Bundle()
            args.putParcelable(EVENT_INFO, eventData)
            eventMediaFragment.arguments = args
            return eventMediaFragment
        }
    }

    private var _binding: FragmentEventMediaBinding? = null
    private val binding get() = _binding!!
    private var eventData: EventData? = null
    private var selectedType: String = "Photos"
    private lateinit var mediaPhotoAdepter: MediaPhotoAdepter
    private lateinit var mediaVideoAdepter: MediaVideoAdepter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun setSelectionStatus(type: String) {
        selectedType = type
        if (type == resources.getString(R.string.label_photos)) {
            binding.videoAppCompatTextView.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.new_map_category_unselected_background
                )
            binding.photosAppCompatImageView.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.new_map_category_selected_background
                )

            val listOfPopularProducts = arrayListOf<String>()
            eventData?.media?.forEach { menuItem ->
                menuItem.image?.let { product ->
                    listOfPopularProducts.add(product)
                }
            }

            mediaPhotoAdepter.listOfDataItems = listOfPopularProducts

            binding.videoRecyclerView.isVisible = false
            binding.photoRecyclerView.isVisible = true
        } else if (type == resources.getString(R.string.label_video)) {
            binding.photoRecyclerView.isVisible = false
            binding.videoRecyclerView.isVisible = true
            binding.videoAppCompatTextView.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.new_map_category_selected_background
                )
            binding.photosAppCompatImageView.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.new_map_category_unselected_background
                )
            val listOfPopularProducts = arrayListOf<EventData>()
            eventData?.let { listOfPopularProducts.add(it) }
            mediaVideoAdepter.listOfDataItems = listOfPopularProducts
        }
    }

    private fun initUI() {

        arguments?.let {
            eventData = it.getParcelable(EVENT_INFO)
        }
        binding.photosAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            setSelectionStatus(resources.getString(R.string.label_photos))
        }

        binding.videoAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            setSelectionStatus(resources.getString(R.string.label_video))
        }

        mediaPhotoAdepter = MediaPhotoAdepter(requireContext()).apply {
            mediaPhotoViewClick.subscribeAndObserveOnMainThread {
                requireActivity().startActivity(FullScreenActivity.getIntent(requireContext(), it))
            }
        }
        mediaVideoAdepter = MediaVideoAdepter(requireContext()).apply {
            mediaVideoViewClick.subscribeAndObserveOnMainThread {
//                requireActivity().startActivity(
//                    FullScreenImageActivity.getIntent(
//                        requireContext(),
//                        it
//                    )
//                )
                startActivityWithDefaultAnimation(TempActivity.getIntent(requireContext(), it.postVideoUrl,it.postVideoThumbnailUrl))
            }
        }

        val listOfPopularProducts = arrayListOf<String>()
        eventData?.media?.forEach { menuItem ->
            menuItem.image?.let { product ->
                listOfPopularProducts.add(product)
            }
        }
        mediaPhotoAdepter.listOfDataItems = listOfPopularProducts
        binding.photoRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
            adapter = mediaPhotoAdepter
        }
        binding.videoRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
            adapter = mediaVideoAdepter
        }
    }
}