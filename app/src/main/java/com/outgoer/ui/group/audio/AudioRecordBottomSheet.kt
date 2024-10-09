package com.outgoer.ui.group.audio

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.chat.model.ChatSendMessageRequest
import com.outgoer.api.chat.model.MessageType
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.base.view.CustomProgressDialog
import com.outgoer.databinding.AudioRecordBottomsheetBinding
import com.outgoer.ui.chat.viewmodel.ChatMessageViewModel
import com.outgoer.ui.chat.viewmodel.ChatMessageViewState
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AudioRecordBottomSheet : BaseBottomSheetDialogFragment() {

    // creating a variable for media recorder object class.
    private var mRecorder: MediaRecorder? = null

    // creating a variable for mediaplayer class
    private var mPlayer: MediaPlayer? = null

    // constant for storing audio permission
    val REQUEST_AUDIO_PERMISSION_CODE = 1
    private var recorder: MediaRecorder? = null
    private var output: String? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatMessageViewModel>
    private lateinit var chatMessageViewModel: ChatMessageViewModel
    private var chatConversationInfo: ChatConversationInfo? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    lateinit var cloudFlareRepository: CloudFlareRepository

    var handler: Handler? = null
    var seconds = 0
    var finalSeconds = 0
    companion object {
        val TAG: String = "AudioRecordBottomSheet"
        val CHAT_USER_INFO = "CHAT_USER_INFO"

        @JvmStatic
        fun newInstance(): AudioRecordBottomSheet {
            return AudioRecordBottomSheet()
        }

        @JvmStatic
        fun newInstanceWithData(chatConversationInfo: ChatConversationInfo): AudioRecordBottomSheet {
            var audioRecordBottomSheet = AudioRecordBottomSheet()

            var bundle = Bundle()
            bundle.putParcelable(CHAT_USER_INFO, chatConversationInfo)

            audioRecordBottomSheet.arguments = bundle

            return audioRecordBottomSheet
        }
    }

    private var _binding: AudioRecordBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var isStart: Int = 0

    private var cloudFlareConfig: CloudFlareConfig? = null
    private var imageUploadDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        chatMessageViewModel = getViewModelFromFactory(viewModelFactory)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AudioRecordBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        chatMessageViewModel.getCloudFlareConfig()

        handler  = Handler(Looper.getMainLooper())
        arguments?.let {
            chatConversationInfo = it.getParcelable<ChatConversationInfo>(CHAT_USER_INFO)
        }

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        mRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            MediaRecorder()
        }
        output =
            requireActivity().cacheDir.absolutePath + "/android_recording_${System.currentTimeMillis()}_" + loggedInUserCache.getLoggedInUser()?.loggedInUser?.id + ".mp3"

        var outputfile = File(output)
        outputfile.createNewFile()
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder?.setOutputFile(outputfile.absolutePath)

        listenToViewEvents()
        listenToViewModel()
    }

    private fun runTimer() {
        handler!!.post(object : Runnable {
            override fun run() {
                val hours: Int = seconds / 3600
                val minutes: Int = seconds % 3600 / 60
                val secs: Int = seconds % 60

                // Format the seconds into hours, minutes,
                // and seconds.
                val time: String = java.lang.String
                    .format(
                        Locale.getDefault(),
                        "%d:%02d:%02d", hours,
                        minutes, secs
                    )
                binding.timerAppCompatTextView.text = time

                seconds++

                handler?.postDelayed(this, 1000)

                if(!(mPlayer?.isPlaying() == true) && mRecorder == null) {
                    handler = null
                    seconds = 0
                    return
                }
            }
        })

    }
    private fun listenToViewEvents() {

        binding.deleteCardView.throttleClicks().subscribeAndObserveOnMainThread {

            openDeletePopup()
            dismissBottomSheet()
        }

        binding.cvChatAction.throttleClicks().subscribeAndObserveOnMainThread {
            output = ""
            mRecorder = null
        }

        binding.messageSendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (cloudFlareConfig != null && output != null) {
                context?.let { it1 ->
                    pausePlaying()

                    val chatRequest = ChatSendMessageRequest(
                        senderId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0,
                        receiverId = if (resources.getString(
                                R.string.label_group
                            ).equals(
                                chatConversationInfo?.chatType ?: 0
                            )
                        ) 0 else chatConversationInfo?.receiverId ?: 0,
                        conversationId = if (resources.getString(
                                R.string.label_group
                            ).equals(
                                chatConversationInfo?.conversationId ?: 0
                            )
                        ) 0 else chatConversationInfo?.conversationId ?: 0,
                        message = "",
                        fileSize = "",
                        file = "",
                        fileType = MessageType.Audio,
                        profileUrl = if(chatConversationInfo?.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo?.filePath else  loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar,
                        name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.name ?: "",
                        chatType = chatConversationInfo?.chatType,
                        groupName = if(chatConversationInfo?.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo?.groupName else "",
                        duration = finalSeconds.toString(),
                        username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username ?: ""
                    )

                    chatMessageViewModel.uploadVideo(output, chatRequest)

                }
            }
        }

        binding.recordAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (isStart == 0) {
                binding.cvChatAction.visibility = View.GONE
                binding.deleteCardView.visibility = View.GONE
                checkPermissionGranted(requireContext())
                binding.actionAppCompatTextView.text =
                    resources.getString(R.string.label_release_for_end_record)
                binding.recordAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_stop,
                        null
                    )
                )
                binding.recordStatusAppCompatTextView.text =
                    resources.getString(R.string.label_stop_Record)
                binding.recordStatusAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_FF2121,
                        null
                    )
                )
                runTimer()
                isStart = 1
            } else if (isStart == 1) {
                binding.cvChatAction.visibility = View.VISIBLE
                binding.deleteCardView.visibility = View.VISIBLE
                pauseRecording()
                binding.actionAppCompatTextView.text =
                    resources.getString(R.string.label_you_Can_listen_Record)
                binding.recordAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_send_record,
                        null
                    )
                )

                finalSeconds = seconds
                seconds = 0
                handler = null

                binding.recordStatusAppCompatTextView.text =
                    resources.getString(R.string.label_send_Record)
                binding.recordStatusAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_03CA16,
                        null
                    )
                )
                isStart = 2
            } else {
                pausePlaying()
                io.reactivex.Observable.timer(1000,TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                    playAudio()
                }.autoDispose()
            }

        }
    }

    private fun openDeletePopup() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_audio))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            pausePlaying()
            output = ""
            mRecorder = null
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    private fun listenToViewModel() {
        chatMessageViewModel.messageViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatMessageViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ChatMessageViewState.CloudFlareConfigErrorMessage -> {

                    showLongToast(it.errorMessage)
                    dismiss()
                }
                is ChatMessageViewState.UploadImageCloudFlareLoading -> {
                    if (it.isLoading) {
                        imageUploadDialog = CustomProgressDialog.showAVLProgressDialog(requireContext())
                    } else {
                        CustomProgressDialog.hideAVLProgressDialog(imageUploadDialog)
                    }
                }
                is ChatMessageViewState.ErrorMessage -> {
                    showLongToast(it.toString())
                }
                is ChatMessageViewState.NewChatMessage -> {
                    dismiss()
                }
                is ChatMessageViewState.LoadingState -> {

                }
                is ChatMessageViewState.SuccessMessage -> {

                }

                else -> {}
            }
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    fun dismissBottomSheet() {
        dismiss()
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        startRecording()
                    } else {
                        showToast(getString(com.outgoer.R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(com.outgoer.R.string.msg_permission_denied))
                }
            })
    }

    private fun startRecording() {
        try {
            mRecorder?.prepare()
            mRecorder?.start()
            Toast.makeText(requireContext(), "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    fun playAudio() {
        handler = Handler(Looper.getMainLooper())
        runTimer()
        // for playing our recorded audio
        // we are using media player class.
        mPlayer = MediaPlayer()
        try {
            // below method is used to set the
            // data source which will be our file name
            mPlayer?.setDataSource(output)

            // below method will prepare our media player
            mPlayer?.prepare()

            // below method will start our media player.
            mPlayer?.start()
        } catch (e: IOException) {
            Log.e("TAG", "prepare() failed")
        }
    }

    fun pauseRecording() {

        // below method will stop
        // the audio recording.
        mRecorder?.stop()

        // below method will release
        // the media recorder class.
        mRecorder?.release()
        mRecorder = null
    }

    fun pausePlaying() {
        // this method will release the media player
        // class and pause the playing of our recorded audio.
        handler = null
        seconds = 0
        mPlayer?.release()
        mPlayer = null
    }
}