package com.jamid.codesquare.ui
// something simple
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentCreatePostBinding
import com.jamid.codesquare.listeners.AddTagsListener
import com.jamid.codesquare.listeners.ChipClickListener
import com.jamid.codesquare.listeners.ItemSelectResultListener
import com.jamid.codesquare.listeners.MediaClickListener
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

// TODO("Add create post reactive form")
class CreatePostFragment : BaseFragment<FragmentCreatePostBinding>(),
    AddTagsListener,
    ItemSelectResultListener<MediaItem>, ChipClickListener, MediaClickListener {

    private var isUpdateMode = false
    private var hasInitiated = false
    var currentCropPos = -1
    private var startTime = System.currentTimeMillis()
    private var thumbnailUrl: String? = null

    private var currentDialogFrag: BottomSheetDialogFragment? =  null

    private fun onUpdatePost(post: Post) {
        activity.binding.mainToolbar.title =
            getString(R.string.update_post)
        isUpdateMode = true
        thumbnailUrl = post.thumbnail
        viewModel.setCurrentPost(post)
        val mediaItems = convertMediaListToMediaItemList(post.mediaList, post.mediaString)
        viewModel.setCreatePostMediaList(mediaItems)
    }

    private fun validatePostContent(): Boolean {
        if (binding.postTitleText.editText?.text?.trim().isNullOrBlank()) {
            binding.postTitleText.showError("Title cannot be empty.")
            return false
        }

        if (binding.postTitleText.editText?.text?.trim().toString().length !in 6..100) {
            binding.postTitleText.showError( "Title must be longer than 5 characters and shorter than 100 characters.")
            return false
        }

        if (binding.postContentText.editText?.text?.trim().isNullOrBlank()) {
            binding.postContentText.showError("Content must not be empty.")
            return false
        }


        if (thumbnailUrl == null) {
            Snackbar.make(binding.root, "Couldn't generate thumbnail for this post. Try re-selecting media items.", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.createPostActions)
                .show()
            return false
        }

        val currentMediaList = viewModel.createPostMediaList.value
        return if (currentMediaList.isNullOrEmpty()) {
            Snackbar.make(binding.root, "No media items selected.", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.createPostActions)
                .show()
            return false
        } else {
            true
        }
    }

    override fun onMediaItemsAdded() {
        super.onMediaItemsAdded()
        binding.uploadError.hide()
    }

    override fun onMediaLayoutItemRemoved(pos: Int) {
        super.onMediaLayoutItemRemoved(pos)
        viewModel.deleteMediaItemAtPosition(pos)
        binding.uploadError.hide()
    }

    override fun onMediaLayoutCleared() {
        super.onMediaLayoutCleared()
        viewModel.clearCreatePostMediaItems()
        binding.uploadError.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser
        val prevPost = arguments?.getParcelable<Post>(PREVIOUS_POST)
        if (prevPost != null) {
            onUpdatePost(prevPost)
        } else {
            val newPost = Post.newInstance(currentUser)
            viewModel.setCurrentPost(newPost)
        }

        binding.userName.text = currentUser.name
        binding.userImg.setImageURI(currentUser.photo)

        setBottomActions()
        setNavigation()
        setMediaRecycler()

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment.tag) {
                AddTagsFragment.TAG, InputSheetFragment.TAG, LocationFragment.TAG -> {
                    showKeyboardDelayed()
                }
                GalleryFragment.TAG -> {
                    hideKeyboard()
                }
            }
        }

    }

    private fun showKeyboardDelayed() {
        runDelayed(1000) {
            if (keyboardState.value != true) {
                showKeyboard()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.currentPost.value?.let {
            prefill(it)
        }
    }

    private fun setBottomActions() {
        binding.addTagBtn.setOnClickListener {
            val tags = getTags()
            currentDialogFrag = AddTagsFragment.builder()
                .setTitle("Add tag to post")
                .setPrefill(tags)
                .setListener(this)
                .build()

            currentDialogFrag?.show(activity.supportFragmentManager, AddTagsFragment.TAG)
        }
        binding.addLocationBtn.setOnClickListener {
            currentDialogFrag = LocationFragment()
            currentDialogFrag?.show(activity.supportFragmentManager, LocationFragment.TAG)
        }
        binding.addLinkBtn.setOnClickListener {
            currentDialogFrag =
                InputSheetFragment.builder("Add links to your existing post sources or files. Ex. Github, Google drive, etc.")
                    .setTitle("Add link to post")
                    .setHint("Ex: https://wwww.google.com")
                    .setMessage("Make sure to give a proper url.")
                    .setPositiveButton("Add link") { _, _, s ->

                        // this cannot be an url
                        if (!s.contains('.')) {
                            Snackbar.make(binding.root, "Not a proper url", Snackbar.LENGTH_LONG)
                                .show()
                            return@setPositiveButton
                        }

                        if (s.startsWith('.') || s.endsWith('.')) {
                            Snackbar.make(binding.root, "Not a proper url", Snackbar.LENGTH_LONG)
                                .show()
                            return@setPositiveButton
                        }

                        addLink(s)

                    }.setNegativeButton("Cancel") { a, _ ->
                        a.dismiss()
                    }.build()

            currentDialogFrag?.show(activity.supportFragmentManager, InputSheetFragment.TAG)
        }

        binding.addMediaBtn.setOnClickListener {
            currentDialogFrag = GalleryFragment(itemSelectResultListener = this).apply {
                title = "Select items"
                primaryActionLabel = "Select"
            }
            currentDialogFrag?.show(activity.supportFragmentManager, GalleryFragment.TAG)
        }
    }

    private fun onBackPressed() {
        viewModel.setCurrentImage(null)
        viewModel.clearCreatePostMediaItems()
        findNavController().navigateUp()
    }

    private fun setNavigation() {
        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })

        activity.binding.mainToolbar.setNavigationOnClickListener {
            viewModel.clearCreatePostMediaItems()
            onBackPressed()
        }
    }

    private fun setMediaRecycler() {
        val helper: SnapHelper = LinearSnapHelper()

        val lm = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        var totalCount = 0

        fun setCounterText(currentPos: Int) {
            if (totalCount != 0) {
                binding.mediaHelperLayout.mediaItemCounter.show()
                val t = "$currentPos/$totalCount"
                binding.mediaHelperLayout.mediaItemCounter.text = t
            } else {
                binding.mediaHelperLayout.mediaItemCounter.hide()
            }
        }

        val mediaAdapter = MediaAdapter("", false, this)


        binding.mediaHelperLayout.imageCropBtn.show()
        binding.mediaHelperLayout.imageCropBtn.setOnClickListener {
            val pos = lm.findFirstVisibleItemPosition()
            currentCropPos = pos
            val item = viewModel.createPostMediaList.value!![pos]

            if (item.type == image) {
                if (item.url.toUri().scheme == "https" || item.url.toUri().scheme == "http") {
                    Snackbar.make(binding.root, "This image cannot be cropped because this is an already uploaded image.", Snackbar.LENGTH_INDEFINITE)
                        .setAnchorView(binding.createPostActions)
                        .show()
                } else {
                    val cropFragment = CropFragment2().apply {
                        image = item.url
                    }
                    cropFragment.show(activity.supportFragmentManager, "CropFragment")
                }
            } else {
                Snackbar.make(binding.root, "Video cannot be cropped.", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.createPostActions)
                    .show()
            }
        }

        viewModel.currentImage.observe(viewLifecycleOwner) {
            if (it != null) {
                val metadata = getMetadataForFile(it)
                val mediaItem = if (metadata != null) {
                    val mime = getMimeType(metadata.url.toUri())
                    if (mime != null) {
                        val type = if (mime.contains(video)) {
                            video
                        } else {
                            image
                        }

                        if (type == image) {
                            val img = compressImage(activity, it)
                            if (img != null) {
                                MediaItem(img.toString(), metadata.name, image, mime, sizeInBytes = metadata.size, ext = metadata.ext)
                            } else {
                                Log.e(TAG, "Something went wrong while compressing image.")
                                null
                            }
                        } else {
                            if (metadata.size < VIDEO_SIZE_LIMIT) {
                                MediaItem(it.toString(), metadata.name, video, mime, sizeInBytes = metadata.size, ext = metadata.ext)
                            } else {
                                Log.e(TAG, "Video selection omitted because video size is too large.")
                                null
                            }
                        }
                    } else {
                        Log.e(TAG, "Mime couldn't be generated for the given file: $it")
                        null
                    }
                } else {
                    Log.e(TAG, "ActivityResult: Metadata of the $it is null")
                    null
                }

                if (mediaItem != null) {
                    if (currentCropPos != -1) {
                        viewModel.updateCreatePostList(mediaItem, currentCropPos)
                    }
                } else {
                    Log.d(TAG, "setMediaRecycler: Media Item nulll")
                }

            }
        }

        // setting up the recycler
        binding.postMediaRecycler.apply {
            mLifecycleOwner = viewLifecycleOwner
            adapter = mediaAdapter

            setMediaCounterText(binding.mediaHelperLayout.mediaItemCounter)

            onFlingListener = null
            helper.attachToRecyclerView(this)
            layoutManager = lm
            OverScrollDecoratorHelper.setUpOverScroll(binding.postMediaRecycler, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
        }

        binding.mediaHelperLayout.removeCurrentImgBtn.setOnClickListener {
            val pos = lm.findFirstCompletelyVisibleItemPosition()
            onMediaLayoutItemRemoved(pos)
        }

        binding.mediaHelperLayout.clearAllImagesBtn.setOnClickListener {
            onMediaLayoutCleared()
        }

        viewModel.createPostMediaList.observe(viewLifecycleOwner) { mediaItems ->
            if (!mediaItems.isNullOrEmpty()) {
                totalCount = mediaItems.size
                setCounterText(1)
                mediaAdapter.submitList(mediaItems)
                binding.postMediaContainer.show()

                binding.postMediaRecycler.scrollToPosition(currentCropPos)
            } else {
                binding.postMediaContainer.hide()
            }
        }
    }


    private fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        for (child in binding.postTagsContainer.children) {
            val chip = child as Chip
            val tag = chip.text.toString()
            tags.add(tag)
        }
        return tags
    }

    private fun setTags(tags: List<String>) {
        binding.tagsHeader.isVisible = tags.isNotEmpty()
        
        binding.postTagsContainer.addTagChips(
            tags,
            true,
            isDefaultTheme = false,
            insertAtStart = true,
            chipClickListener = this,
            tag = CHIPS_TAGS
        )
    }

    private fun setLinks(links: List<String>) {
        binding.linksHeader.isVisible = links.isNotEmpty()

        binding.postLinksContainer.addTagChips(
            links,
            true,
            isDefaultTheme = false,
            insertAtStart = true,
            chipClickListener = this,
            tag = CHIPS_LINKS
        )
    }

    override fun onCloseIconClick(chip: Chip) {
        super.onCloseIconClick(chip)
        when (chip.tag) {
            CHIPS_LINKS -> {
                binding.postLinksContainer.removeView(chip)
            }
            CHIPS_TAGS -> {
                binding.postTagsContainer.removeView(chip)
            }
        }
    }

    private fun addTag(tag: String) {
        tag.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.text = tag
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            binding.postTagsContainer.removeView(chip)
        }
        binding.postTagsContainer.addView(chip, 0)
    }

    private fun addLink(link: String) {
        link.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.isCloseIconVisible = true
        chip.text = link
        chip.isCheckable = false
        chip.setOnCloseIconClickListener {
            binding.postLinksContainer.removeView(chip)
        }

        binding.postLinksContainer.addView(chip, 0)
    }

    override fun onStop() {
        super.onStop()
        binding.postTitleText.editText?.text?.toString()
            ?.let { viewModel.setCurrentPostTitle(it) }

        binding.postContentText.editText?.text?.toString()?.let {
            viewModel.setCurrentPostContent(it)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.postTitleText.editText?.text?.toString()
            ?.let { viewModel.setCurrentPostTitle(it) }

        binding.postContentText.editText?.text?.toString()?.let {
            viewModel.setCurrentPostContent(it)
        }
    }

    override fun onTagsSelected(tags: List<String>) {

        val existingTags = getTags()
        val allTags = mutableListOf<String>()

        allTags.addAll(existingTags)
        allTags.addAll(tags)

        setTags(allTags.distinct())
    }

    private fun prefill(currentPost: Post) {
        if (!hasInitiated) {
            hasInitiated = true

            /* This is must because the title and content are not refreshed
              all the time by the observer, since it may lead to infinite loop */

            binding.postTitleText.editText?.setText(currentPost.name)
            binding.postContentText.editText?.setText(currentPost.content)

            Log.d(TAG, "setNonReactiveUiOnStartAndResume: ${currentPost.tags.size}")

            setTags(currentPost.tags)
            setLinks(currentPost.sources)

            setPostObserver()
        }
    }


    private fun setLocationTextUi(location: Location) {
        if (location.address.isNotBlank()) {

            binding.postLocationText.show()

            binding.postLocationText.text = location.address

            binding.postLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_round_location_on_small,
                0,
                0,
                0
            )

            binding.postLocationText.setOnClickListener {
                val frag =
                    MessageDialogFragment.builder("Are you sure you want to remove location attached to this post?")
                        .setTitle("Removing location ...")
                        .setPositiveButton("Remove") { _, _ ->
                            viewModel.setCurrentPostLocation(Location())
                        }
                        .setNegativeButton("Cancel") { d, _ ->
                            d.dismiss()
                        }
                        .build()

                frag.show(activity.supportFragmentManager, MessageDialogFragment.TAG)
            }
        } else {

            binding.postLocationText.hide()

            binding.postLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_round_add_location_small,
                0,
                0,
                0
            )

            binding.postLocationText.text = getString(R.string.add_location)

            binding.postLocationText.setOnClickListener {
                val frag = LocationFragment()
                frag.show(activity.supportFragmentManager, "LocationFragment")
            }
        }
    }

    private fun setReactiveUi(currentPost: Post) {
        setLocationTextUi(currentPost.location)
    }


    /* There are two places when we need the saved post from viewModel
    *
    * 1. when the fragments opens
    * 2. when the fragment resumes
    *
    * Location, tags and links are changed outside the current fragment
    * hence there should be an observer for current post only regarding
    * location, tags and links.
    *
    * To have updated data at all times, the changes in the form must be
    * reflected in the viewModel data immediately
    *
    * */


    private fun setPostObserver() {
        viewModel.currentPost.observe(viewLifecycleOwner) { currentPost ->
            if (currentPost != null) {
                setReactiveUi(currentPost)
                prefill(currentPost)
            }
        }

        setTitleObserver()
        setContentObserver()
        setTagsObserver()
        setLinksObserver()

    }

    private fun setTitleObserver() {

        /* To make sure the title always stays exactly the same as entered immediately */
        binding.postTitleText.editText?.doAfterTextChanged {

            binding.postTitleText.error = null
            binding.postTitleText.isErrorEnabled = false

            if (it.isNullOrBlank()) {
                viewModel.setCurrentPostTitle("")
            } else {
                viewModel.setCurrentPostTitle(it.trim().toString())
            }
        }
    }

    private fun setContentObserver() {
        /* To make sure content is always reflective */
        binding.postContentText.editText?.doAfterTextChanged {

            binding.postContentText.error = null
            binding.postContentText.isErrorEnabled = false

            if (it.isNullOrBlank()) {
                viewModel.setCurrentPostContent("")
            } else {
                viewModel.setCurrentPostContent(it.trim().toString())
            }
        }
    }

    private fun setTagsObserver() {
        binding.postTagsContainer.onChildrenChanged {

            binding.tagsHeader.isVisible = binding.postTagsContainer.childCount != 0

            val tags = mutableListOf<String>()
            for (v in it) {
                val chip = v as Chip
                val tag = chip.text.toString()
                tags.add(tag)
            }
            viewModel.setCurrentPostTags(tags)
        }
    }

    private fun setLinksObserver() {
        binding.postLinksContainer.onChildrenChanged {

            binding.linksHeader.isVisible = binding.postLinksContainer.childCount != 0

            val links = mutableListOf<String>()
            for (v in it) {
                val chip = v as Chip
                val link = chip.text.toString()
                links.add(link)
            }
            viewModel.setCurrentPostLinks(links)
        }
    }

    fun ViewGroup.onChildrenChanged(onChange: (Sequence<View>) -> Unit) {
        this.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(p0: View?, p1: View?) {
                onChange(this@onChildrenChanged.children)
            }

            override fun onChildViewRemoved(p0: View?, p1: View?) {
                onChange(this@onChildrenChanged.children)
            }
        })
    }

    override fun onItemsSelected(items: List<MediaItem>, externalSelect: Boolean) {
        if (items.isNotEmpty()) {
            val mediaList = viewModel.createPostMediaList.value ?: emptyList()

            // to maintain the order, in a way that the first image should always be the thumbnail
            if (mediaList.isEmpty()) {
                val postThumb = getObjectThumbnail(items.first().url.toUri())
                if (postThumb != null) {
                    val temp = convertBitmapToFile(postThumb)
                    if (temp != null) {
                        val uri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, temp)
                        if (uri != null) {
                            /*viewModel.uploadPostThumbnail(uri)*/
                            thumbnailUrl = uri.toString()
                        }
                    }
                }
            }

            val newList = mutableListOf<MediaItem>()
            newList.addAll(mediaList)
            newList.addAll(items)

            viewModel.setCreatePostMediaList(newList)
        } else {
            toast("NULL")
        }
    }

    companion object {
        private const val TAG = "CreatePostFragment"

        private const val CHIPS_TAGS = "chip_tags"
        private const val CHIPS_LINKS = "chip_links"
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentCreatePostBinding {
        setMenu(R.menu.create_post_menu, onItemSelected = { item ->
            when (item.itemId) {
                R.id.create_post -> {
                    if (!validatePostContent())
                        return@setMenu true

                    if (isUpdateMode) {
                        val dialogFragment =
                            MessageDialogFragment.builder(getString(R.string.update_post_loading))
                                .setProgress()
                                .build()

                        dialogFragment.show(activity.supportFragmentManager, MessageDialogFragment.TAG)

                        viewModel.updatePost(thumbnailUrl!!) { _, task ->
                            runOnMainThread {
                                dialogFragment.dismiss()
                                if (task.isSuccessful) {
                                    findNavController().navigateUp()
                                } else {
                                    viewModel.setCurrentError(task.exception)
                                }
                            }
                        }
                    } else {
                        val dialogFragment = MessageDialogFragment
                            .builder(getString(R.string.create_post_loading))
                            .setProgress()
                            .build()

                        dialogFragment.show(activity.supportFragmentManager, MessageDialogFragment.TAG)

                        viewModel.createPost(thumbnailUrl!!) {
                            runOnMainThread {
                                dialogFragment.dismiss()
                                if (it.isSuccessful) {
                                    findNavController().navigateUp()
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }
                        }
                    }
                }
            }
            true
        })
        return FragmentCreatePostBinding.inflate(inflater)
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

    }

}