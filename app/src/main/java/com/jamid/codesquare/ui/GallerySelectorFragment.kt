package com.jamid.codesquare.ui

/*
class GallerySelectorFragment: BaseFragment2<FragmentGallerySelectorBinding, MainViewModel>(), LoaderManager.LoaderCallbacks<Cursor>, MediaClickListener {

    override val viewModel: MainViewModel by activityViewModels()

    private var lm: LoaderManager? = null
    private val savedList = mutableListOf<MediaItemWrapper>()

    override fun getViewBinding(): FragmentGallerySelectorBinding {
        return FragmentGallerySelectorBinding.inflate(layoutInflater)
    }

    private val posArray = IntArray(10){-1}
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            lm?.initLoader(GALLERY_LOADER_ID, null, this)
        }
    }

    private val selectedMediaItems = mutableListOf<MediaItem>()
    private lateinit var gridMediaAdapter: GridMediaAdapter
    private lateinit var gridMediaAdapter2: GridMediaAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gridMediaAdapter = GridMediaAdapter(true, this)
        gridMediaAdapter2 = GridMediaAdapter()

        val chatChannelId = arguments?.getString("chatChannelId") ?: return

        binding.galleryMediaRecycler.apply {
            adapter = gridMediaAdapter
            itemAnimator = null
            layoutManager = GridLayoutManager(binding.root.context, 3)
        }

        binding.selectedMediaRecycler.apply {
            adapter = gridMediaAdapter2
            itemAnimator = null
            layoutManager = GridLayoutManager(binding.root.context, 3)
        }

        lm = LoaderManager.getInstance(this)

        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                lm?.initLoader(GALLERY_LOADER_ID, null, this)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                toast("Grant permission")
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        viewModel.selectMediaItems.observe(viewLifecycleOwner) { selectMediaItems ->
            if (!selectMediaItems.isNullOrEmpty()) {
                binding.noMedia.hide()
                savedList.clear()
                savedList.addAll(selectMediaItems.map { it1 -> MediaItemWrapper(it1, false, -1) })
                gridMediaAdapter.submitList(savedList)
            } else {
                binding.noMedia.show()
            }
        }

        binding.mediaSendBtn.setOnClickListener {
            if (selectedMediaItems.isNotEmpty()) {
                binding.uploadProgress.show()
                binding.mediaSelectCountText.text = "Uploading files ..."

                binding.mediaSendBtn.disable()

                val messages = selectedMediaItems.map {
                    val meta = Metadata(it.sizeInBytes, it.name, it.url, it.ext, 0, 0)
                    Message(randomId(), chatChannelId, it.type, randomId(), UserManager.currentUserId, UserManager.currentUser.minify(), meta, listOf(), listOf(), System.currentTimeMillis(), System.currentTimeMillis(), null, null, isDownloaded = false, isSavedToFiles = false, isCurrentUserMessage = false)
                }

                saveThumbnailsBeforeSendingMessages(messages)

                viewModel.sendMessages(messages) { taskResult ->
                    activity.runOnUiThread {
                        binding.uploadProgress.hide()
                        binding.mediaSelectCountText.text = "Uploaded successfully"
                        binding.mediaSendBtn.enable()

                        when (taskResult) {
                            is Result.Error -> {
                                toast("Something went wrong while uploading documents")
                            }
                            is Result.Success -> {
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }
        }

        binding.browseFiles.setOnClickListener {
            selectedMediaItems.clear()
            onChange()
            activity.selectChatMedia2()
        }

        binding.clearSelect.setOnClickListener {
            selectedMediaItems.clear()
            onChange()
            viewModel.setChatUploadMedia(emptyList())
        }

        viewModel.chatUploadMediaItems.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                gridMediaAdapter2.submitList(it.map {it1 -> MediaItemWrapper(it1, false, 0) })
                binding.selectedMediaRecycler.show()
                binding.selectScrim.show()

                selectedMediaItems.clear()
                selectedMediaItems.addAll(it)

                binding.clearSelect.show()
                onChange()
            } else {
                binding.selectedMediaRecycler.hide()
                binding.selectScrim.hide()
                binding.clearSelect.hide()
            }
        }

    }

    @Suppress("DEPRECATION")
    private fun saveThumbnailsBeforeSendingMessages(messages: List<Message>) {
        for (item in messages) {
            val name = "thumb_${item.content}.jpg"
            val uri = item.metadata!!.url.toUri()

            val bitmap = getObjectThumbnail(uri)

            if (bitmap != null) {
                val fullPath = "images/thumbnails/${item.chatChannelId}"

                getNestedDir(activity.filesDir, fullPath)?.let { dest ->
                    getFile(dest, name)?.let {
                        val destUri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it)

                        when (val res = createImageFile(bitmap, destUri)) {
                            is Result.Error -> {
                                Log.e(TAG, "saveThumbnailsBeforeSendingMessages: ${res.exception}")
                            }
                            is Result.Success -> {
                                item.metadata!!.thumbnail = res.data
                            }
                        }
                    }
                }
            }
        }
    }


    private fun createImageFile(image: Bitmap, destUri: Uri): Result<String> {
        val contentResolver = activity.contentResolver
        val pfd: ParcelFileDescriptor?
        return try {
            pfd = contentResolver.openFileDescriptor(destUri, "w")
            val out = FileOutputStream(pfd?.fileDescriptor)

            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            pfd?.close()

            Result.Success(destUri.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e)
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

        val selection = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"     //Selection criteria
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

    @Suppress("DEPRECATION")
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

                val thumbnail = getObjectThumbnail(fileUri)

                val ext = "." + name.split('.')[1]

                val mediaItem = MediaItem(fileUri.toString(),
                    name, type, mimeType, size, ext, "", thumbnail, createdAt, dateModified)

                mediaItems.add(mediaItem)
            } while (cursor.moveToNext() && count < 50)
        }

        viewModel.setSelectMediaItems(mediaItems)

    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }

    companion object {
        private const val TAG = "GallerySelectorFrag"
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

    override fun onMediaDocumentClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

    }

    override fun onMediaDocumentLongClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

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
            savedList[pos].isSelected = true

            if (lastPosition == -1) {
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
            val s = "${selectedMediaItems.size} files selected"
            binding.mediaSelectCountText.text = s
        } else {
            binding.mediaSelectCountText.text = ""
        }
    }


}*/
