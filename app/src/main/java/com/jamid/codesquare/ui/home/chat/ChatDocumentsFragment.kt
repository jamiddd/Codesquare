package com.jamid.codesquare.ui.home.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.MediaDocumentAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatDocumentsBinding
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.ui.MessageListener3

class ChatDocumentsFragment: BaseFragment<FragmentChatDocumentsBinding>(), MessageListener3, MediaClickListener {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatDocumentsBinding {
        return FragmentChatDocumentsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return
        val documentAdapter = MediaDocumentAdapter(false, this)

        binding.documentsRecycler.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        }

        runOnBackgroundThread {
            val documentMessages = viewModel.getDocumentMessages(chatChannelId)

            runOnMainThread {
                if (documentMessages.isNotEmpty()) {
                    binding.noDocumentsText.hide()
                    runOnBackgroundThread {
                        documentAdapter.submitList(getMediaItemsFromMessages(documentMessages))
                    }
                } else {
                    binding.noDocumentsText.show()
                }
            }
        }

    }

    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"
        private const val TAG = "ChatDocumentsFragment"

        fun newInstance(chatChannelId: String) =
            ChatDocumentsFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }


 /*   override fun onMessageDocumentClick(message: Message) {
        val destination = File(documentsDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file)
    }

    private fun openFile(file: File) {
        // Get URI and MIME type of file
        try {
            Log.d(TAG, file.path)
            val uri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)
            val mime = requireActivity().contentResolver.getType(uri)

            // Open file with user selected app
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, e.localizedMessage.orEmpty())
        }

    }*/

  /*  override fun onMessageRead(message: Message) {}

    override fun onMessageUpdated(message: Message) {}

    override fun onMessageSenderClick(message: Message) {}*/


    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }


     override fun onMediaMessageItemClick(message: Message) {
         if (message.isDownloaded) {
             if (message.type == document) {
                 showMessageDocument(message)
             }
         } else {
             onMessageNotDownloaded(message) {
                 onMessageMediaItemClick(it)
             }
         }
     }

    /*private fun showMessageMedia(message: Message) {

        if (message.isDownloaded) {
            val fullPath = "${message.type.toPlural()}/${message.chatChannelId}"
            val name = message.content + message.metadata!!.ext

            getNestedDir(activity.filesDir, fullPath)?.let { dest ->
                getFile(dest, name)?.let { it1 ->
                    val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                    val mimeType = getMimeType(uri)

                    mimeType?.let {
                        val mediaItem = MediaItem(uri.toString(), name, message.type,
                            it, message.metadata!!.size, message.metadata!!.ext, "", null, System.currentTimeMillis(), System.currentTimeMillis())

                        activity.showMediaFragment(listOf(mediaItem))
                    }
                }
            }
        } else {
            onMessageNotDownloaded(message) {
                showMessageMedia(message)
            }
        }
    }
*/
    private fun showMessageDocument(message: Message) {

        if (message.isDownloaded) {

            val fullPath = "documents/${message.chatChannelId}"
            val name = message.content + message.metadata!!.ext

            getNestedDir(activity.filesDir, fullPath)?.let {
                getFile(it, name)?.let { it1 ->
                    val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                    openFile(uri)
                }
            }

        } else {
            onMessageNotDownloaded(message) {
                showMessageDocument(message)
            }
        }

        // query external downloads directory and check for file name [name]
        // if it is present than open the file through intent
        // else save the internal file to external downloads directory and then
        // open it through intent
    }

    private fun openFile(uri: Uri) {
        try {
            val mime = requireActivity().contentResolver.getType(uri)

            // Open file with user selected app
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, e.localizedMessage.orEmpty())
        }
    }

    override fun onMessageNotDownloaded(message: Message, f: (Message) -> Unit) {
        super.onMessageNotDownloaded(message, f)

        val name = message.content + message.metadata!!.ext
        val dirType = message.type.toPlural()
        val fullPath = dirType + "/" + message.chatChannelId

        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                viewModel.saveMediaToFile(message, it1) { m ->
                    // the download may finish after the user has moved to different fragment
                    if (this.isVisible) {
                        runOnMainThread {
                            f(m)
                        }
                    }
                }
            }
        }
    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        val name = mediaItemWrapper.mediaItem.name
        viewModel.getMessageByMediaItemName(name) { message ->
            runOnMainThread {
                if (message != null) {
                    Log.d(TAG, "onMediaClick: ${message.messageId}")
                    onMediaMessageItemClick(message)
                } else {
                    Log.d(TAG, "onMediaClick: No message found by the name $name")
                }
            }
        }
    }

}