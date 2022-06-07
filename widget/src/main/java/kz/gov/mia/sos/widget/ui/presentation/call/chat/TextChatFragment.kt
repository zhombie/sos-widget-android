package kz.gov.mia.sos.widget.ui.presentation.call.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.garage.chat.model.Message
import kz.garage.chat.model.reply_markup.button.Button
import kz.garage.context.system.service.clipboardManager
import kz.garage.fragment.toast.toast
import kz.garage.kotlin.simpleNameOf
import kz.garage.multimedia.store.model.Audio
import kz.garage.multimedia.store.model.Document
import kz.garage.multimedia.store.model.Image
import kz.garage.multimedia.store.model.Video
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.core.image.preview.ImagePreviewDialogFragment
import kz.gov.mia.sos.widget.core.image.preview.VideoPreviewDialogFragment
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.core.multimedia.*
import kz.gov.mia.sos.widget.core.multimedia.source.sourceUri
import kz.gov.mia.sos.widget.ui.component.SOSWidgetProgressView
import kz.gov.mia.sos.widget.ui.component.chat.ChatFooterView
import kz.gov.mia.sos.widget.ui.platform.BaseFragment
import kz.gov.mia.sos.widget.ui.platform.alert.ThemedAlertDialog
import kz.gov.mia.sos.widget.ui.presentation.call.CallScreen
import kz.gov.mia.sos.widget.ui.presentation.call.vm.CallViewModel
import kz.gov.mia.sos.widget.ui.presentation.common.chat.adapter.ChatMessagesAdapter
import kz.gov.mia.sos.widget.ui.presentation.common.chat.adapter.ChatMessagesAdapterItemDecoration
import kz.gov.mia.sos.widget.utils.FileUtils
import kz.zhombie.radio.Radio
import kz.zhombie.radio.getDurationOrZeroIfUnset
import kz.zhombie.radio.getPositionByProgress
import kotlin.math.roundToInt

class TextChatFragment : BaseFragment(R.layout.sos_widget_fragment_text_chat),
    ChatMessagesAdapter.Callback,
    ChatFooterView.Callback, GetContentDelegate {

    companion object {
        private val TAG = simpleNameOf<TextChatFragment>()

        fun newInstance() = TextChatFragment()
    }

    private var recyclerView: RecyclerView? = null
    private var chatFooterView: ChatFooterView? = null
    private var progressView: SOSWidgetProgressView? = null

    private val viewModel: CallViewModel by activityViewModels()

    private var radio: Radio? = null

    private var concatAdapter: ConcatAdapter? = null
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    private var interactor: StorageAccessFrameworkInteractor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        interactor = StorageAccessFrameworkInteractor(this, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        chatFooterView = view.findViewById(R.id.chatFooterView)
        progressView = view.findViewById(R.id.progressView)

        setupRecyclerView()
        setupChatFooterView()
        setupProgressView()

        observeState()
        observeCallState()
        observeNewMessage()
        observeDownloadState()
    }

    override fun onDestroy() {
        chatMessagesAdapter?.unregisterAdapterDataObserver(chatAdapterDataObserver)

        interactor?.dispose()
        interactor = null

        super.onDestroy()
    }

    private fun setupRecyclerView() {
        chatMessagesAdapter = ChatMessagesAdapter(this)

        chatMessagesAdapter?.registerAdapterDataObserver(chatAdapterDataObserver)

        concatAdapter = ConcatAdapter(chatMessagesAdapter)
        recyclerView?.adapter = concatAdapter

//        recyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
//        layoutManager.stackFromEnd = true
        recyclerView?.layoutManager = layoutManager
//        recyclerView.isNestedScrollingEnabled = false

//        recyclerView.itemAnimator = DefaultItemAnimator()

        recyclerView?.addItemDecoration(ChatMessagesAdapterItemDecoration(requireContext()))
    }

    private fun setupChatFooterView() {
        chatFooterView?.disableSendMessageButton()

        chatFooterView?.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                chatFooterView?.disableSendMessageButton()
            } else {
                chatFooterView?.enableSendMessageButton()
            }
        }

        chatFooterView?.callback = this
    }

    private fun setupProgressView() {
        progressView?.setCancelable(true)
        progressView?.setOnCancelClickListener { viewModel.onCancelUploadMediaRequest() }
    }

    private fun observeState() {
        viewModel.getState().observe(viewLifecycleOwner, { state ->
            when (state) {
                is CallScreen.State.IDLE -> {
                    progressView?.hide()
                }
                is CallScreen.State.Loading.Determinate -> {
                    progressView?.setDeterminate()
                    progressView?.setProgress(state.progress)
                    progressView?.show()
                }
                is CallScreen.State.Loading.Indeterminate -> {
                    progressView?.setIndeterminate()
                    progressView?.show()
                }
            }
        })
    }

    private fun observeCallState() {
        viewModel.getCallState().observe(viewLifecycleOwner, { callState ->
            Logger.debug(TAG, "callState: $callState")

            if (callState.isActive) {
                chatFooterView?.enableMediaSelectionButton()
                chatFooterView?.setSendMessageButtonClickRestriction(false)
            } else {
                chatFooterView?.disableMediaSelectionButton()
                chatFooterView?.setSendMessageButtonClickRestriction(true)
            }
        })
    }

    private fun observeNewMessage() {
        viewModel.getNewMessage().observe(viewLifecycleOwner, { message ->
            chatMessagesAdapter?.addNewEntity(message, notify = true)
//            chatMessagesAdapter?.submitList(message)
        })
    }

    private fun observeDownloadState() {
        viewModel.getDownloadState().observe(viewLifecycleOwner, { state ->
            if (state.content is Document) {
                chatMessagesAdapter?.setDownloadState(state)
            }
        })
    }

    /**
     * [ChatMessagesAdapter.Callback] implementation
     */

    override fun onUrlInTextClicked(url: String) {
        if (url.startsWith("#")) {
            val text = url.removePrefix("#")
            viewModel.onUrlInTextClicked(text)
        } else {
            ThemedAlertDialog.OpenLinkConfirmationConfirmation(requireContext(), url)
                .build(
                    positive = {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
                .show()
        }
    }

    override fun onImageClicked(imageView: ImageView, image: Image) {
        val uri = image.sourceUri
        if (uri == null) {
            toast(R.string.sos_widget_error_file_cannot_be_read)
        } else {
            ImagePreviewDialogFragment.show(
                fragmentManager = parentFragmentManager,
                caption = image.label,
                uri = uri
            )
        }
    }

    override fun onVideoClicked(imageView: ImageView, video: Video) {
        val uri = video.sourceUri
        if (uri == null) {
            toast(R.string.sos_widget_error_file_cannot_be_read)
        } else {
            VideoPreviewDialogFragment.show(
                fragmentManager = parentFragmentManager,
                caption = video.label,
                uri = uri
            )
        }
    }

    override fun onAudioClicked(audio: Audio, itemPosition: Int): Boolean {
        Logger.debug(TAG, "onAudioClicked() -> audio: $audio, itemPosition: $itemPosition")

        val uri = audio.sourceUri

        if (uri == null) {
            toast(R.string.sos_widget_error_file_cannot_be_read)
            return false
        } else {
            if (radio?.isReleased() == false) {
                if (uri == radio?.currentSource) {
                    radio?.playOrPause()
                    return false
                }
            }

            radio?.release()
            radio = null
            radio = Radio.Builder(requireContext())
                .create(object : Radio.Listener {
                    private fun getAudioDuration(): Long = radio?.getDurationOrZeroIfUnset() ?: 0L

                    override fun onPlaybackStateChanged(state: Radio.PlaybackState) {
                        Logger.debug(TAG, "onPlaybackStateChanged() -> state: $state")
                        when (state) {
                            Radio.PlaybackState.READY, Radio.PlaybackState.ENDED -> {
                                chatMessagesAdapter?.resetAudioPlaybackState(
                                    itemPosition = itemPosition,
                                    duration = getAudioDuration()
                                )
                            }
                            else -> {
                            }
                        }
                    }

                    override fun onIsPlayingStateChanged(isPlaying: Boolean) {
                        Logger.debug(TAG, "onIsPlayingStateChanged() -> isPlaying: $isPlaying")

                        chatMessagesAdapter?.setAudioPlaybackState(itemPosition, isPlaying)
                    }

                    override fun onPlaybackPositionChanged(position: Long) {
                        Logger.debug(TAG, "onPlaybackPositionChanged() -> position: $position")

                        chatMessagesAdapter?.setAudioPlayProgress(
                            itemPosition = itemPosition,
                            progress = radio?.currentPercentage ?: 0F,
                            currentPosition = position,
                            duration = getAudioDuration()
                        )
                    }

                    override fun onPlayerError(cause: Throwable?) {
                        toast(R.string.sos_widget_error_audio_data_source)
                    }
                })

            radio?.start(uri, true)
            return true
        }
    }

    override fun onDocumentClicked(document: Document, itemPosition: Int) {
        val file = DownloadAssistant.getDownloadableFile(requireContext(), document)

        when (val openFile = FileUtils.openFile(requireContext(), file)) {
            is FileUtils.OpenFile.Success -> {
                if (!openFile.tryToLaunch(requireContext())) {
                    toast(R.string.sos_widget_error_file_cannot_be_read)
                }
            }
            is FileUtils.OpenFile.Error -> {
                when (openFile) {
                    is FileUtils.OpenFile.Error.Unknown -> {
                        toast(R.string.sos_widget_error_file_cannot_be_read)
                    }
                    is FileUtils.OpenFile.Error.FileDoesNotExist -> {
                        viewModel.onDownloadContent(
                            content = document,
                            outputFile = file,
                            itemPosition = itemPosition
                        )
                    }
                }
            }
        }
    }

    override fun onInlineButtonClicked(message: Message, button: Button, itemPosition: Int) {
        viewModel.onInlineButtonClicked(button)
    }

    override fun onSliderChange(audio: Audio, progress: Float): Boolean {
        if (radio == null) return false
        if (radio?.isReleased() == true) return false
        if (audio.sourceUri == radio?.currentSource) {
            val position = radio?.getPositionByProgress(progress.roundToInt())
            if (position != null) {
                radio?.seekTo(position)
                return true
            }
        }
        return false
    }

    override fun onMessageLongClicked(text: String) {
        with(context?.clipboardManager) {
            if (this == null) {
                toast(R.string.sos_widget_error_occurred)
            } else {
                addPrimaryClipChangedListener(object :
                    ClipboardManager.OnPrimaryClipChangedListener {
                    override fun onPrimaryClipChanged() {
                        removePrimaryClipChangedListener(this)
                        toast(R.string.sos_widget_copied)
                    }
                })

                setPrimaryClip(ClipData.newPlainText(getString(R.string.sos_widget_message), text))
            }
        }
    }

    /**
     * [ChatFooterView.Callback] implementation
     */

    override fun onMediaSelectionButtonClicked(): Boolean {
        if (chatFooterView?.isMediaSelectionButtonEnabled == false) {
            alertDialog?.dismiss()
            alertDialog = null
            alertDialog = ThemedAlertDialog.Builder(requireContext())
                .setTitle(R.string.sos_widget_attention)
                .setMessage(R.string.sos_widget_info_attach_media_available_on_active_conversation)
                .setPositiveButton(R.string.sos_widget_ok) { dialog, _ -> dialog.dismiss() }
                .show()
            return false
        }

        alertDialog?.dismiss()
        alertDialog = null
        alertDialog = ThemedAlertDialog.Builder(requireContext())
            .setTitle(R.string.sos_widget_media_selection)
            .setItems(
                arrayOf(
                    getString(R.string.sos_widget_image),
                    getString(R.string.sos_widget_video),
                    getString(R.string.sos_widget_audio),
                    getString(R.string.sos_widget_document)
                )
            ) { dialog, which ->
                dialog.dismiss()

                when (which) {
                    0 ->
                        interactor?.launchSelection(GetContentResultContract.Params(MimeType.IMAGE))
                    1 ->
                        interactor?.launchSelection(GetContentResultContract.Params(MimeType.VIDEO))
                    2 ->
                        interactor?.launchSelection(GetContentResultContract.Params(MimeType.AUDIO))
                    3 ->
                        interactor?.launchSelection(GetContentResultContract.Params(MimeType.DOCUMENT))
                }
            }
            .show()

        return true
    }

    override fun onSendMessageButtonClicked(messageText: String) {
        if (chatFooterView?.isSendMessageActionRestricted == true) {
            // Ignored
        } else {
            viewModel.onSendMessageButtonClicked(messageText)
            chatFooterView?.clearInputViewText()
        }
    }

    /**
     * [GetContentDelegate] implementation
     */

    override fun onContentResult(result: GetContentDelegate.Result) {
        when (result) {
            is GetContentDelegate.Result.Success -> {
                viewModel.onMediaSelected(result.content)
            }
            is GetContentDelegate.Result.Error.NullableUri -> {
                toast(R.string.sos_widget_error_occurred)
            }
            is GetContentDelegate.Result.Error.SizeLimitExceeds -> {
                toast(getString(R.string.sos_widget_error_file_size_exceeds_limit, result.maxSize))
            }
            else -> {
                toast(R.string.sos_widget_error_occurred)
            }
        }
    }

    private val chatAdapterDataObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                onNewChatMessagesInserted()
            }
        }
    }

    private fun onNewChatMessagesInserted() {
        recyclerView?.smoothScrollToPosition(0)
    }

}