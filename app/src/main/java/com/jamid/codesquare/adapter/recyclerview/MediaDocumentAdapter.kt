package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import com.jamid.codesquare.R
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.databinding.VDocLayoutBinding
import com.jamid.codesquare.listeners.MediaClickListener
import java.text.SimpleDateFormat
import java.util.*


class MediaDocumentAdapter(
    private val isSelect: Boolean = false,
    private val mediaClickListener: MediaClickListener? = null
) : SuperMediaAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperMediaViewHolder {
        return MediaDocumentViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.v_doc_layout, parent, false),
            isSelect,
            mediaClickListener
        )
    }

    override fun onBindViewHolder(holder: SuperMediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class MediaDocumentViewHolder(
    view: View,
    isSelect: Boolean = false,
    listener: MediaClickListener? = null
) : SuperMediaViewHolder(view, isSelect, listener) {

    private val binding: VDocLayoutBinding by lazy { VDocLayoutBinding.bind(view) }

    override fun bind(item: MediaItemWrapper?) {
        super.bind(item)

        val mediaItemWrapper = item ?: return
        val mediaItem = mediaItemWrapper.mediaItem

        binding.fileName.text = mediaItem.name
        binding.fileSize.text = android.text.format.Formatter.formatShortFileSize(
            binding.root.context,
            mediaItem.sizeInBytes
        )

        val ext = if (mediaItem.ext.isNotBlank()) {
            mediaItem.ext.substringAfter('.')
        } else {
            mediaItem.mimeType.split('/')[1]
        }

        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            binding.fileExt,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        binding.fileExt.text = ext

        binding.fileModifiedDate.text = SimpleDateFormat(
            "hh:mm a dd/MM/yyyy",
            Locale.getDefault()
        ).format(mediaItem.dateModified * 1000)

        setSelectText(binding.root, binding.mediaSelectCount)

    }

    companion object {
        private const val TAG = "MediaDocumentViewHolder"
    }

}