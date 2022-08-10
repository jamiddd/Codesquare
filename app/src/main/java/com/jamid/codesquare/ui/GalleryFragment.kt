package com.jamid.codesquare.ui

import android.os.Bundle
import com.jamid.codesquare.adapter.recyclerview.GridMediaAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperMediaAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.listeners.ItemSelectResultListener

/*
@ExperimentalPagingApi
class GalleryFragment(
    private val isMultipleAllowed: Boolean = true,
    private val isVideosAllowed: Boolean = true,
    private val onFinish: (items: List<MediaItem>) -> Unit
): FullscreenBottomSheetFragment(), LoaderManager.LoaderCallbacks<Cursor>, MediaClickListener {

    private lateinit var binding: FragmentGalleryBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var lm: LoaderManager? = null
    private val savedList = mutableListOf<MediaItemWrapper>()
    private lateinit var gridMediaAdapter: GridMediaAdapter
    private val posArray: IntArray = if (isMultipleAllowed) {
        IntArray(10){-1}
    } else {
        IntArray(1){-1}
    }
    private val selectedMediaItems = mutableListOf<MediaItem>()

    var title: String = "Select items"
    var primaryLabel: String = "Done"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFullHeight()

        binding.galleryProgress.show()

        lm = LoaderManager.getInstance(this)

        gridMediaAdapter = GridMediaAdapter(this, true)
        binding.sheetTopComp.bottomSheetToolbar.title = title
        binding.sheetTopComp.bottomSheetDoneBtn.text = primaryLabel
        binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = false

        binding.sheetTopComp.bottomSheetToolbar.setNavigationOnClickListener {
            dismiss()
        }

        val request = Manifest.permission.READ_EXTERNAL_STORAGE
        checkPermission(request) { isGranted ->
            if (isGranted) {
                lm?.initLoader(GALLERY_LOADER_ID, null, this)
            } else {
                (requireActivity() as MainActivity).apply {
                    currentRequest = request
                    permissionLauncher.launch(request)
                }
            }
        }

        viewModel.readPermission.observe(viewLifecycleOwner) {
            if (it) {
                lm?.initLoader(GALLERY_LOADER_ID, null, this)
            }
        }

        viewModel.selectMediaItems.observe(viewLifecycleOwner) { selectMediaItems ->

            binding.galleryProgress.hide()

            if (!selectMediaItems.isNullOrEmpty()) {
                binding.noItemsText.hide()
                savedList.clear()
                savedList.addAll(selectMediaItems.map { it1 -> MediaItemWrapper(it1, false, -1) })
                gridMediaAdapter.submitList(savedList)
            } else {
                binding.noItemsText.show()
            }
        }

        binding.galleryItemsRecycler.apply {
            adapter = gridMediaAdapter
            itemAnimator = null
            layoutManager = GridLayoutManager(binding.root.context, 3)
        }

        binding.browseBtn.setOnClickListener {
            (requireActivity() as MainActivity).selectMediaItems(isMultipleAllowed)
        }

        viewModel.preUploadMediaItems.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                selectedMediaItems.clear()
                selectedMediaItems.addAll(it)
                onFinish(selectedMediaItems)
                dismiss()
            }
        }

        binding.sheetTopComp.bottomSheetDoneBtn.setOnClickListener {
            onFinish(selectedMediaItems)
            dismiss()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Files.getContentUri("external")
            }

        val selection = if (isVideosAllowed) {
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"     //Selection criteria
        } else {
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        }

        val selectionArgs = arrayOf<String>()  //Selection criteria
        val sortOrder: String = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.SIZE
        )

        return CursorLoader(
            requireContext(),
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (cursor == null)
            return

        val mediaItems = mutableListOf<MediaItem>()

        val idCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mimeCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val dateAddedCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val modifiedCol: Int =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val nameCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

        var count = 0

        if (cursor.moveToFirst()) {
            do {
                count++
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol)

                val (type, fileUri) = if (mimeType.contains(video)) {
                    video to ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    image to ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                val name = cursor.getString(nameCol)
                val dateModified = cursor.getLong(modifiedCol)
                val size = cursor.getLong(sizeCol)
                val createdAt = cursor.getLong(dateAddedCol)

                val ext = "." + name.split('.')[1]

                val mediaItem = MediaItem(fileUri.toString(),
                    name, type, mimeType, size, ext, "", null, createdAt, dateModified)

                mediaItems.add(mediaItem)
            } while (cursor.moveToNext() && count < 100)
        }

        viewModel.setSelectMediaItems(mediaItems)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {}

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {}

    override fun onMediaMessageItemClick(message: Message) {}

    override fun onMediaDocumentClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {}

    override fun onMediaDocumentLongClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {}

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
            savedList[pos].isSelected = true

            if (lastPosition == -1) {
                if (isMultipleAllowed) {
                    toast("Cannot add any more items")
                } else {
                    toast("You can only select one item")
                }
                // all elements are filled and this can not be added
                return
            } else {
                posArray[lastPosition] = pos
                mediaItemWrapper.selectedCount = lastPosition + 1
                savedList[pos].selectedCount = lastPosition + 1
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

            savedList[pos].isSelected = false
            savedList[pos].selectedCount = -1

            for (i in posArray.indices) {
                val itemP = posArray[i]
                if (itemP != -1) {
                    savedList[itemP].isSelected = true
                    savedList[itemP].selectedCount = i + 1
                } else {
                    break
                }
            }

        }

        gridMediaAdapter.submitList(savedList)

        onChange()

        for (i in posArray.indices) {
            val itemPos = posArray[i]
            if (itemPos != -1) {
                gridMediaAdapter.notifyItemChanged(itemPos)
            }
        }

        gridMediaAdapter.notifyItemChanged(pos)
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

    private fun onChange() {
        val size = selectedMediaItems.size
        if (size > 0) {
            if (isMultipleAllowed) {
                val s = "${selectedMediaItems.size} items selected"
                binding.sheetTopComp.bottomSheetToolbar.subtitle = s
            }
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = true
        } else {
            if (isMultipleAllowed) {
                binding.sheetTopComp.bottomSheetToolbar.subtitle = null
            }
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setPreUploadMediaItems(emptyList())
    }

}*/
// something simple

class GalleryFragment(
    isMultiple: Boolean = true,
    type: ItemSelectType = ItemSelectType.GALLERY,
    private val itemSelectResultListener: ItemSelectResultListener<MediaItem>? = null
): SelectFragment(isMultiple, type) {

    override fun getAdapter(): SuperMediaAdapter {
        return GridMediaAdapter(true, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setItemSelectListener(itemSelectResultListener)
    }

    companion object {
        const val TAG = "GalleryFragment"
    }

}