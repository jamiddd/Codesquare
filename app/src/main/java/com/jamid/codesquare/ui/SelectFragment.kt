package com.jamid.codesquare.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SuperMediaAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentSelectBinding
import com.jamid.codesquare.listeners.ItemSelectResultListener
import com.jamid.codesquare.listeners.MediaClickListener

abstract class SelectFragment(
    private val isMultipleSelection: Boolean = true,
    protected val type: ItemSelectType = ItemSelectType.GALLERY
) : BaseBottomFragment<FragmentSelectBinding>(),
    MediaClickListener {

    companion object {
        private const val TAG = "SelectFragment"
        private const val LOAD_THRESHOLD = 20
        private const val PAGE_SIZE = 30
    }

    private var itemSelectResultListener: ItemSelectResultListener<MediaItem>? = null
    private val selectedMediaItems = mutableListOf<MediaItem>()

    private val filesViewModel: FilesViewModel by viewModels()

    private lateinit var mediaAdapter: SuperMediaAdapter

    var title: String = "Select"
    var primaryActionLabel: String = "Done"
    var hasReachedEnd = false

    protected fun onChange() {
        val size = selectedMediaItems.size
        if (size > 0) {
            if (isMultipleSelection) {
                val s = "${selectedMediaItems.size} items selected"
                binding.sheetTopComp.bottomSheetToolbar.subtitle = s
            }
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = true
        } else {
            if (isMultipleSelection) {
                binding.sheetTopComp.bottomSheetToolbar.subtitle = null
            }
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = false
        }
    }

    private val posArray: IntArray = if (isMultipleSelection) {
        IntArray(10) { -1 }
    } else {
        IntArray(1) { -1 }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setSelectMediaItems(emptyList())
        viewModel.setPreUploadMediaItems(emptyList())
    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        val existing = selectedMediaItems.find {
            it.url == mediaItemWrapper.mediaItem.url
        }

        if (existing == null) {
            var lastPosition = -1

            for (i in posArray.indices) {
                if (posArray[i] == -1) {
                    lastPosition = i
                    break
                }
            }

            mediaItemWrapper.isSelected = true
            filesViewModel.updateSelectStatePos(pos, true)

            if (lastPosition == -1) {
                if (isMultipleSelection) {
                    toast("Cannot add any more items")
                } else {
                    toast("You can only select one item")
                }
                // all elements are filled and this can not be added
                return
            } else {
                posArray[lastPosition] = pos
                mediaItemWrapper.selectedCount = lastPosition + 1
                filesViewModel.updateSelectCountAtPos(pos, lastPosition + 1)
                selectedMediaItems.add(mediaItemWrapper.mediaItem)
            }
        } else {

            mediaItemWrapper.isSelected = false

            var posToBeRemoved = -1

            // items can be deselected without order, order must be maintained in the pos array
            for (i in posArray.indices) {
                if (posArray[i] == pos) {
                    posToBeRemoved = i
                    break
                }
            }

            posArray[posToBeRemoved] = -1
            mediaItemWrapper.selectedCount = -1

            for (i in selectedMediaItems.indices) {
                if (mediaItemWrapper.mediaItem.url == selectedMediaItems[i].url) {
                    selectedMediaItems.removeAt(i)
                    break
                }
            }

            fixPosArray()

            filesViewModel.updateSelectStatePos(pos, false)
            filesViewModel.updateSelectCountAtPos(pos, -1)

            for (i in posArray.indices) {
                val itemP = posArray[i]
                if (itemP != -1) {
                    filesViewModel.updateSelectStatePos(itemP, true)
                    filesViewModel.updateSelectCountAtPos(itemP, i + 1)
                } else {
                    break
                }
            }

        }

        mediaAdapter.submitList(filesViewModel.selectMediaItems.value)

        onChange()

        for (i in posArray.indices) {
            val itemPos = posArray[i]
            if (itemPos != -1) {
                mediaAdapter.notifyItemChanged(itemPos)
            }
        }

        mediaAdapter.notifyItemChanged(pos)
    }

    private fun findHole(): Int {
        var hole = -1
        for (i in posArray.indices) {
            if (posArray[i] == -1 && i < posArray.size - 1 && posArray[i + 1] != -1) {
                hole = i
                break
            }
        }
        return hole
    }

    private fun fixPosArray() {
        val hole = findHole()

        while (findHole() != -1) {
            for (i in hole until posArray.size - 1) {
                posArray[i] = posArray[i + 1]
            }
        }
    }

    open fun setItemSelectListener(mResultListener: ItemSelectResultListener<MediaItem>?) {
        itemSelectResultListener = mResultListener
    }

    abstract fun getAdapter(): SuperMediaAdapter

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {}
    override fun onMediaMessageItemClick(message: Message) {}

    private var lastItemTime: Long = System.currentTimeMillis()

    private fun loadData() {
        val files = filesViewModel.loadItemsFromExternal(requireActivity().contentResolver, type, lastItemSortAnchor = lastItemTime)
        if (files.isNotEmpty()) {
            hasReachedEnd = files.size < PAGE_SIZE
            filesViewModel.addMediaItemsToList(files.map { MediaItemWrapper(it, false, -1) })
            lastItemTime = files.last().dateModified
        } else {
            hasReachedEnd = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaAdapter = getAdapter()

        val request = Manifest.permission.READ_EXTERNAL_STORAGE

        setPrimaryBtn()
        setNavigation()
        setRecycler()
        setBrowseButton()

        checkPermission(request) { isGranted ->
            if (isGranted) {
                loadData()
            } else {
                activity.apply {
                    currentRequest = request
                    permissionLauncher.launch(request)
                }
            }
        }

        viewModel.readPermission.observe(viewLifecycleOwner) {
            if (it) {
                loadData()
            }
        }

        viewModel.preUploadMediaItems.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                selectedMediaItems.clear()
                selectedMediaItems.addAll(it)
                itemSelectResultListener?.onItemsSelected(selectedMediaItems, true)
                dismiss()
            }
        }

        filesViewModel.selectMediaItems.observe(viewLifecycleOwner) { selectMediaItems ->

            binding.itemsProgress.hide()

            if (!selectMediaItems.isNullOrEmpty()) {
                binding.noItemsText.hide()
                mediaAdapter.submitList(selectMediaItems)
                Log.d(TAG, "onViewCreated: Submitted items to adapter")
            } else {
                binding.noItemsText.show()
            }
        }

    }

    private fun setPrimaryBtn() {
        binding.sheetTopComp.bottomSheetDoneBtn.text = primaryActionLabel
        binding.sheetTopComp.bottomSheetDoneBtn.setOnClickListener {
            itemSelectResultListener?.onItemsSelected(selectedMediaItems, false)
            dismiss()
        }
    }

    private fun setNavigation() {

        binding.sheetTopComp.apply {
            bottomSheetToolbar.setNavigationOnClickListener {
                dismiss()
            }

            bottomSheetToolbar.title = title
        }

    }

    private fun setRecycler() {
        binding.selectItemsRecycler.apply {
            adapter = mediaAdapter
            itemAnimator = null

            when (type) {
                ItemSelectType.DOCUMENT -> {
                    layoutManager = LinearLayoutManager(requireContext())
                }
                else -> {
                    layoutManager = GridLayoutManager(requireContext(), 3)
                    val smallestPadding = (resources.getDimension(R.dimen.smallest_padding) / 2).toInt()
                    setPadding(smallestPadding, smallestPadding, 0, 0)
                }
            }
        }

        binding.selectItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = when (type) {
                        ItemSelectType.DOCUMENT ->recyclerView.layoutManager as LinearLayoutManager
                        else -> recyclerView.layoutManager as  GridLayoutManager
                    }

                    val itemPos = lm.findLastCompletelyVisibleItemPosition()
                    if (mediaAdapter.itemCount - itemPos < LOAD_THRESHOLD && !hasReachedEnd) {
                        // load next batch
                        loadData()
                    }
                }
            }
        })

    }

    private fun setBrowseButton() {
        binding.browseBtn.setOnClickListener {
            val (itemType, mimeTypes) = when (type) {
                ItemSelectType.DOCUMENT -> {
                    "*/*" to null
                }
                ItemSelectType.GALLERY -> {
                    "*/*" to arrayOf("image/bmp", "image/jpeg", "image/png", "video/mp4")
                }
                ItemSelectType.GALLERY_ONLY_IMG -> {
                    "image/*" to arrayOf("image/bmp", "image/jpeg", "image/png")
                }
                ItemSelectType.GALLERY_ONLY_VID -> {
                    "video/*" to arrayOf("video/mp4")
                }
            }
            (requireActivity() as MainActivity).selectMediaItems(
                isMultipleSelection,
                itemType,
                mimeTypes
            )
        }
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentSelectBinding {
        return FragmentSelectBinding.inflate(inflater)
    }

}

enum class ItemSelectType {
    GALLERY_ONLY_IMG, GALLERY_ONLY_VID, GALLERY, DOCUMENT
}

