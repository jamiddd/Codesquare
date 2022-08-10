package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.codesquare.adapter.recyclerview.MediaDocumentAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperMediaAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.listeners.ItemSelectResultListener

class FilesFragment(
    isMultiple: Boolean = true,
    type: ItemSelectType = ItemSelectType.DOCUMENT,
    private val itemSelectResultListener: ItemSelectResultListener<MediaItem>? = null
) : SelectFragment(isMultiple, type) {

    override fun getAdapter(): SuperMediaAdapter {
        return MediaDocumentAdapter(true, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setItemSelectListener(itemSelectResultListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

    }

}