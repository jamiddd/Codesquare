package com.jamid.codesquare.ui

import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.data.ImageSelectType
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.FragmentCreatePostBinding
import com.jamid.codesquare.listeners.AddTagsListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class CreatePostFragment: BaseFragment<FragmentCreatePostBinding, MainViewModel>(), AddTagsListener {

    override val viewModel: MainViewModel by activityViewModels()
    
    private lateinit var imageAdapter: ImageAdapter
    private var imagesCount = 0

    private var isUpdateMode = false

    private fun onUpdatePost(post: Post) {
        activity.findViewById<MaterialToolbar>(R.id.main_toolbar).title = getString(R.string.update_post)
        isUpdateMode = true
        viewModel.setCurrentPost(post)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.create_post_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.create_post -> {

                // instead of toasts we will use helper text of text input layout
                if (!validatePostContent())
                    return true

                viewModel.setCurrentPostTitle(getTitle())
                viewModel.setCurrentPostTags(getTags())
                viewModel.setCurrentPostContent(getContent())
                viewModel.setCurrentPostLinks(getLinks())

                if (isUpdateMode) {
                    val dialogFragment = MessageDialogFragment.builder(getString(R.string.update_post_loading))
                        .setIsHideable(false)
                        .setIsDraggable(false)
                        .shouldShowProgress(true)
                        .build()

                    dialogFragment.show(childFragmentManager, MessageDialogFragment.TAG)

                    viewModel.updatePost { newPost, task ->
                        activity.runOnUiThread {
                            dialogFragment.dismiss()
                            if (task.isSuccessful) {
                                val mainRoot = activity.findViewById<CoordinatorLayout>(R.id.main_container_root)
                                Snackbar.make(mainRoot, "Post updated successfully", Snackbar.LENGTH_LONG).show()

                                viewModel.deletePostById(newPost.id)

                                // updating local post
                                viewModel.insertPosts(newPost)
                                viewModel.setCurrentPost(null)
                                findNavController().navigateUp()
                            } else {
                                viewModel.setCurrentError(task.exception)
                            }
                        }
                    }
                } else {
                    val dialogFragment = MessageDialogFragment
                        .builder(getString(R.string.create_post_loading))
                        .setIsDraggable(false)
                        .setIsHideable(false)
                        .shouldShowProgress(true)
                        .build()

                    dialogFragment.show(childFragmentManager, MessageDialogFragment.TAG)

                    viewModel.createPost {
                        activity.runOnUiThread {
                            dialogFragment.dismiss()
                            if (it.isSuccessful) {
                                viewModel.setCreatedNewPost(true)

                                findNavController().navigateUp()
                                viewModel.setCurrentPost(null)

                                val mainRoot = activity.findViewById<CoordinatorLayout>(R.id.main_container_root)
                                Snackbar.make(mainRoot, "Post uploaded successfully.", Snackbar.LENGTH_LONG)
                                    .setBehavior(NoSwipeBehavior())
                                    .show()
                            } else {
                                viewModel.setCurrentError(it.exception)
                            }
                        }
                    }
                }

                true
            }
            else -> true
        }
    }

    private fun getLinks(): List<String> {
        val links = mutableListOf<String>()
        for (child in binding.postLinksContainer.children) {
            val chip = child as Chip
            val link = chip.text.toString()
            if (link != getString(R.string.add_link)) {
                links.add(link)
            }
        }
        return links
    }

    private fun validatePostContent(): Boolean {
        if (binding.postTitleText.editText?.text.isNullOrBlank()) {
            binding.postTitleText.isErrorEnabled = true
            binding.postTitleText.error = "Title cannot be empty."
            return false
        }

        if (binding.postTitleText.editText?.text.toString().length !in 6..100) {
            binding.postTitleText.isErrorEnabled = true
            binding.postTitleText.error = "Title must be longer than 5 characters and shorter than 100 characters."
            return false
        }

        if (binding.postContentText.editText?.text.isNullOrBlank()) {
            binding.postContentText.isErrorEnabled = true
            binding.postContentText.error = "Content must not be empty."
            return false
        }

        val currentPost = viewModel.currentPost.value
        return if (currentPost != null) {
            if (currentPost.images.isEmpty()) {
                Snackbar.make(binding.root, "Must include at least one image for the post.", Snackbar.LENGTH_LONG).show()
                false
            } else {
                true
            }
        } else {
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.hide()

        val currentUser = UserManager.currentUser
        val prevPost = arguments?.getParcelable<Post>(PREVIOUS_POST)
        if (prevPost != null) {
            onUpdatePost(prevPost)
        } else {
            Log.d(TAG, "onViewCreated: Creating new post")
            val newPost = Post.newInstance(currentUser)
            viewModel.setCurrentPost(newPost)
        }

        binding.userName.text = currentUser.name
        binding.userImg.setImageURI(currentUser.photo)

        imageAdapter = ImageAdapter()

        val helper: SnapHelper = LinearSnapHelper()
        val imagesManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        binding.postImagesRecycler.apply {
            adapter = imageAdapter
            onFlingListener = null
            helper.attachToRecyclerView(this)
            layoutManager = imagesManager
        }

        binding.postContentText.editText?.doAfterTextChanged {
            binding.postContentText.isErrorEnabled = false
            binding.postContentText.error = null
        }

        binding.postTitleText.editText?.doAfterTextChanged {
            binding.postTitleText.isErrorEnabled = false
            binding.postTitleText.error = null
        }

        viewModel.currentPost.observe(viewLifecycleOwner) { currentPost ->
            Log.d(TAG, "onViewCreated: Current post has changed")
            if (currentPost != null) {
                val images = currentPost.images
                if (images.isNotEmpty()) {

                    Log.d(TAG, "onViewCreated: Images not empty")

                    val newImages = checkForSizeIssues(images)

                    if (newImages.size > images.size) {
                        viewModel.setCurrentPostImages(newImages)
                        return@observe
                    }

                    imageAdapter.submitList(images)

                    imagesCount = images.size

                    updateLayoutOnImagesLoaded()

                    val counterText = "1/$imagesCount"
                    binding.imageCounter.text = counterText

                    binding.removeCurrentImgBtn.show()

                } else {

                    Log.d(TAG, "onViewCreated: Images empty")

                    imageAdapter.submitList(emptyList())
                    updateBtnOnImageCleared()

                    binding.removeCurrentImgBtn.hide()
                }


                binding.postTitleText.editText?.setText(currentPost.name)

                binding.postContentText.editText?.setText(currentPost.content)

                if (currentPost.location.address.isNotBlank()) {
                    binding.postLocationText.text = currentPost.location.address

                    binding.postLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_location_on_small, 0, 0, 0)

                    binding.postLocationText.setOnClickListener {

                        val frag = MessageDialogFragment.builder("Are you sure you want to remove location attached to this post?")
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
                    binding.postLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_add_location_small, 0, 0, 0)
                    binding.postLocationText.text = getString(R.string.add_location)

                    binding.postLocationText .setOnClickListener {
                        viewModel.setCurrentPostTitle(getTitle())
                        viewModel.setCurrentPostContent(getContent())

                        val tags = getTags()
                        viewModel.setCurrentPostTags(tags)

                        val frag = LocationFragment()
                        frag.show(activity.supportFragmentManager, "LocationFragment")
                    }
                }

                addTags(currentPost.tags)
                addLinks(currentPost.sources)

            } else {
                updateBtnOnImageCleared()
            }
        }

        binding.removeCurrentImgBtn.setOnClickListener {
            val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deletePostImageAtPosition(pos)
        }

        binding.addImagesBtn.setOnClickListener {
            activity.selectImage(ImageSelectType.IMAGE_POST)
        }

        binding.clearAllImagesBtn.setOnClickListener {
            viewModel.setCurrentPostImages(emptyList())
        }

        binding.addMoreImagesBtn.setOnClickListener {
            activity.selectImage(ImageSelectType.IMAGE_POST)
        }

        binding.addTagBtn.setOnClickListener {
            val frag = AddTagsFragment.builder()
                .setTitle("Add tag to post")
                .setIsDraggable(false)
                .setListener(this)
                .build()

            frag.show(requireActivity().supportFragmentManager, AddTagsFragment.TAG)
        }

        binding.addLinkBtn.setOnClickListener {

            val frag = InputSheetFragment.builder("Add links to your existing post sources or files. Ex. Github, Google drive, etc.")
                .setTitle("Add link to post")
                .setHint("Ex: https://wwww.google.com")
                .setMessage("Make sure to give a proper url.")
                .setPositiveButton("Add link") { _, _, s ->

                    // this cannot be an url
                    if (!s.contains('.')) {
                        Snackbar.make(binding.root, "Not a proper url", Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (s.startsWith('.') || s.endsWith('.')) {
                        Snackbar.make(binding.root, "Not a proper url", Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    addLink(s)

                }.setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.build()

            frag.show(activity.supportFragmentManager, "InputSheetFrag")
        }

        binding.postImagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                    val counterText = "${pos + 1}/$imagesCount"
                    binding.imageCounter.text = counterText
                }
            }
        })

       /* val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isCreatingPostFirstTime = sharedPref.getBoolean("isCreatingPostFirstTime", true)
        if (isCreatingPostFirstTime) {
            viewLifecycleOwner.lifecycleScope.launch {

                delay(1000)

                if (this@CreatePostFragment.isVisible) {
                    val frag = MessageDialogFragment.builder(getString(R.string.create_post_info))
                        .setTitle("Creating your post .. ")
                        .build()

                    frag.show(activity.supportFragmentManager, MessageDialogFragment.TAG)
                    val editor = sharedPref.edit()
                    editor.putBoolean("isCreatingPostFirstTime", false)
                    editor.apply()
                }
            }
        }*/

    }

    private fun checkForSizeIssues(images: List<String>): List<String> {
        val imagesUris = images.map {
            it.toUri()
        }

        val newImages = images.toMutableList()

        imagesUris.forEachIndexed { _, uri ->
            val cursor = activity.contentResolver.query(uri, null, null, null, null)

            try {
                cursor?.moveToFirst()
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                cursor?.close()

                if (size/1024 > 1024) {
                    newImages.remove(uri.toString())
                }

            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            }
        }

        return newImages
    }

    private fun getTitle() = binding.postTitleText.editText?.text?.trim().toString()

    private fun getContent() = binding.postContentText.editText?.text?.trim().toString()

    private fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        for (child in binding.postTagsContainer.children) {
            val chip = child as Chip
            val tag = chip.text.toString()
            if (tag != getString(R.string.add_tag)) {
                tags.add(tag)
            }
        }
        return tags
    }

    private fun addTags(tags: List<String>) {
        if (binding.postTagsContainer.childCount != 1) {
            binding.postTagsContainer.removeViews(0, binding.postTagsContainer.childCount - 1)
        }
        for (tag in tags) {
            addTag(tag)
        }
    }

    private fun addLinks(links: List<String>) {
        if (binding.postLinksContainer.childCount != 1) {
            binding.postLinksContainer.removeViews(0, binding.postLinksContainer.childCount - 1)
        }
        for (link in links) {
            addLink(link)
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

            onChange()
        }
        binding.postTagsContainer.addView(chip, 0)
    }

    private fun onChange() {
        val title = getTitle()
        viewModel.setCurrentPostTitle(title)

        val content = getContent()
        viewModel.setCurrentPostContent(content)

        val tags = getTags()
        viewModel.setCurrentPostTags(tags)

        val links = getLinks()
        viewModel.setCurrentPostLinks(links)
    }

    private fun addLink(link: String) {
        link.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip

        chip.isCloseIconVisible = true

        /*val splitLink = link.split(".")
        if (splitLink.size > 2) {
            if (splitLink[2].length > 3) {
                val linkText = link.substring(0, 12) + "..."
                chip.text = linkText
                // trim
            } else {
                chip.text = link
            }
        } else {
        }*/
        chip.text = link


        chip.isCheckable = false
        chip.setOnCloseIconClickListener {
            binding.postLinksContainer.removeView(chip)

            onChange()
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

    private fun updateLayoutOnImagesLoaded() {
        binding.addImagesBtn.hide()
        binding.imagesEditorLayout.show()
    }

    private fun updateBtnOnImageCleared() {
        binding.addImagesBtn.show()
        binding.imagesEditorLayout.hide()
    }

    override fun getViewBinding(): FragmentCreatePostBinding {
        return FragmentCreatePostBinding.inflate(layoutInflater)
    }

    override fun onTagsSelected(tags: List<String>) {
        viewModel.setCurrentPostTags(tags)
    }


}