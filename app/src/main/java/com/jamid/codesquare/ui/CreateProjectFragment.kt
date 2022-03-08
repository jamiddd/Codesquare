package com.jamid.codesquare.ui

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.*
import android.webkit.URLUtil
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentCreateProjectBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding

@ExperimentalPagingApi
class CreateProjectFragment: Fragment(R.layout.fragment_create_project) {

    private lateinit var binding: FragmentCreateProjectBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var imageAdapter: ImageAdapter
    private var imagesCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateProjectBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.create_project_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.create_project -> {

                // instead of toasts we will use helper text of text input layout
                if (!validateProjectContent())
                    return true

                viewModel.setCurrentProjectTitle(getTitle())
                viewModel.setCurrentProjectTags(getTags())
                viewModel.setCurrentProjectContent(getContent())
                viewModel.setCurrentProjectLinks(getLinks())

                val view = layoutInflater.inflate(R.layout.loading_layout, null, false)
                val loadingLayoutBinding = LoadingLayoutBinding.bind(view)
                loadingLayoutBinding.loadingText.text = getString(R.string.create_project_loading)

                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setCancelable(false)
                    .show()

                viewModel.createProject {
                    dialog.dismiss()
                    if (it.isSuccessful) {
                        findNavController().navigateUp()
                        viewModel.setCurrentProject(null)
                        toast("Project uploaded successfully.")
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }

                true
            }
            else -> true
        }
    }

    private fun getLinks(): List<String> {
        val links = mutableListOf<String>()
        for (child in binding.projectLinksContainer.children) {
            val chip = child as Chip
            val link = chip.text.toString()
            if (link != "Add Link") {
                links.add(link)
            }
        }
        return links
    }

    private fun validateProjectContent(): Boolean {
        if (binding.projectTitleText.editText?.text.isNullOrBlank()) {
            toast("Title cannot be empty.")
            binding.projectTitleText.editText?.requestFocus()
            return false
        }

        if (binding.projectTitleText.editText?.text.toString().length !in 6..30) {
            toast("Title must be longer than 5 characters and shorter than 31 characters.")
            binding.projectTitleText.editText?.requestFocus()
            return false
        }

        if (binding.projectContentText.editText?.text.isNullOrBlank()) {
            toast("Content must not be empty.")
            binding.projectContentText.editText?.requestFocus()
            return false
        }

        val currentProject = viewModel.currentProject.value
        return if (currentProject != null) {
            if (currentProject.images.isEmpty()) {
                toast("Must include at least one image for the project.")
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
        val activity = requireActivity()

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.hide()

        val currentUser = UserManager.currentUser
        binding.userName.text = currentUser.name
        binding.userImg.setImageURI(currentUser.photo)

        if (viewModel.currentProject.value == null) {
            val newProject = Project.newInstance(currentUser)
            viewModel.setCurrentProject(newProject)
        }

        imageAdapter = ImageAdapter()

        val helper: SnapHelper = LinearSnapHelper()
        val imagesManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            onFlingListener = null
            helper.attachToRecyclerView(this)
            layoutManager = imagesManager
        }

        viewModel.currentProject.observe(viewLifecycleOwner) { currentProject ->
            if (currentProject != null) {
                val images = currentProject.images
                if (images.isNotEmpty()) {

                    val newImages = checkForSizeIssues(images)

                    if (newImages.size > images.size) {
                        viewModel.setCurrentProjectImages(newImages)
                        return@observe
                    }

                    imageAdapter.submitList(images)

                    imagesCount = images.size

                    updateLayoutOnImagesLoaded()

                    val counterText = "1/$imagesCount"
                    binding.imageCounter.text = counterText

                    binding.removeCurrentImgBtn.show()

                } else {
                    imageAdapter.submitList(emptyList())
                    updateBtnOnImageCleared()

                    binding.removeCurrentImgBtn.hide()
                }

                if (currentProject.name.isNotBlank()) {
                    binding.projectTitleText.editText?.setText(currentProject.name)
                }

                if (currentProject.content.isNotBlank()) {
                    binding.projectContentText.editText?.setText(currentProject.content)
                }

                if (currentProject.location.address.isNotBlank()) {
                    binding.projectLocationText.text = currentProject.location.address

                    binding.projectLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_location_on_small, 0, 0, 0)

                    binding.projectLocationText.setOnClickListener {

                        val alertDialog = MaterialAlertDialogBuilder(activity)
                            .setTitle("Removing location ...")
                            .setMessage("Are you sure you want to remove location attached to this project?")
                            .setPositiveButton("Remove") { _, _ ->
                                viewModel.setCurrentProjectLocation(Location())
                            }.setNegativeButton("Cancel") { a, _ ->
                                a.dismiss()
                            }.show()

                        alertDialog.window?.setGravity(Gravity.BOTTOM)

                    }
                } else {
                    binding.projectLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_add_location_small, 0, 0, 0)
                    binding.projectLocationText.text = getString(R.string.add_location)

                    binding.projectLocationText .setOnClickListener {
                        viewModel.setCurrentProjectTitle(getTitle())
                        viewModel.setCurrentProjectContent(getContent())

                        val tags = getTags()
                        viewModel.setCurrentProjectTags(tags)

                        findNavController().navigate(R.id.action_createProjectFragment_to_locationFragment, null, slideRightNavOptions())
                    }
                }

                if (currentProject.tags.isNotEmpty()) {
                    addTags(currentProject.tags)
                }

            } else {
                updateBtnOnImageCleared()
            }
        }

        binding.removeCurrentImgBtn.setOnClickListener {
            val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deleteProjectImageAtPosition(pos)
        }

        binding.addImagesBtn.setOnClickListener {
            (activity as MainActivity).selectProjectImages()
        }

        binding.clearAllImagesBtn.setOnClickListener {
            viewModel.setCurrentProjectImages(emptyList())
        }

        binding.addMoreImagesBtn.setOnClickListener {
            (activity as MainActivity).selectMoreProjectImages()
        }

        binding.addTagBtn.setOnClickListener {
            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add tag .. "

            val alertDialog = MaterialAlertDialogBuilder(activity)
                .setTitle("Add Tag")
                .setMessage("Tags are helpful in searches and also to make sense of the project as to what category it belongs to. Don't use '#'")
                .setView(inputLayout)
                .setPositiveButton("Add") { _, _ ->
                    if (!inputLayoutBinding.inputTextLayout.text.isNullOrBlank()) {
                        val tag = inputLayoutBinding.inputTextLayout.text.trim().toString()

                        val tags = processTagText(tag)
                        for (t in tags) {
                            addTag(t)
                        }

                    }
                }.setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)

        }

        binding.addLinkBtn.setOnClickListener {
            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add link .. "
            inputLayoutBinding.inputTextLayout.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            inputLayoutBinding.inputTextLayout.hint = "Ex: https://wwww.google.com"

            val alertDialog = MaterialAlertDialogBuilder(activity)
                .setTitle("Add Link")
                .setMessage("Add links to your existing project sources or files. Ex. Github, Google drive, etc.")
                .setView(inputLayout)
                .setPositiveButton("Add") { _, _ ->
                    if (!inputLayoutBinding.inputTextLayout.text.isNullOrBlank()) {
                        val link = inputLayoutBinding.inputTextLayout.text.trim().toString()
                        if (URLUtil.isValidUrl(link)) {
                            addLink(link)
                        } else {
                            toast("Not a proper link.")
                        }
                    }
                }.setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {

            if (binding.projectTitleText.editText?.text.isNullOrBlank() && binding.projectContentText.editText?.text.isNullOrBlank() && imagesCount == 0) {
                findNavController().navigateUp()
                viewModel.setCurrentProject(null)
            } else {
                val alertDialog = MaterialAlertDialogBuilder(activity)
                    .setTitle("Save project ...")
                    .setMessage("Save the content of this unfinished project?")
                    .setPositiveButton("Save") { _, _ ->
                        findNavController().navigateUp()
                    }.setNegativeButton("No") { a, _ ->
                        viewModel.setCurrentProject(null)
                        findNavController().navigateUp()
                        a.dismiss()
                    }.show()

                alertDialog.window?.setGravity(Gravity.BOTTOM)
            }
        }

        binding.projectImagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                    val counterText = "${pos + 1}/$imagesCount"
                    binding.imageCounter.text = counterText
                }
            }
        })

        val sharedPref = activity.getSharedPreferences("codesquare_shared", MODE_PRIVATE)
        val isCreatingProjectFirstTime = sharedPref.getBoolean("isCreatingProjectFirstTime", true)
        if (isCreatingProjectFirstTime) {
              binding.createProjectInfo.root.show()
        }

        binding.createProjectInfo.closeInfoBtn.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putBoolean("isCreatingProjectFirstTime", false)
            editor.apply()
            binding.createProjectInfo.root.hide()
        }
    }

    private fun checkForSizeIssues(images: List<String>): List<String> {
        val imagesUris = images.map {
            it.toUri()
        }

        val newImages = images.toMutableList()

        imagesUris.forEachIndexed { _, uri ->
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)

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

    private fun getTitle() = binding.projectTitleText.editText?.text?.trim().toString()

    private fun getContent() = binding.projectContentText.editText?.text?.trim().toString()

    private fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        for (child in binding.projectTagsContainer.children) {
            val chip = child as Chip
            val tag = chip.text.toString()
            if (tag != "Add Tag") {
                tags.add(tag)
            }
        }
        return tags
    }

    private fun addTags(tags: List<String>) {
        if (binding.projectTagsContainer.childCount != 1) {
            binding.projectTagsContainer.removeViews(0, binding.projectTagsContainer.childCount - 1)
        }
        for (tag in tags) {
            addTag(tag)
        }
    }

    private fun addTag(tag: String) {
        tag.trim()
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            binding.projectTagsContainer.removeView(chip)
        }
        binding.projectTagsContainer.addView(chip, 0)
    }

    private fun processTagText(tagText: String): List<String> {
        val re = Regex("[^A-Za-z0-9 ]")
        val newTagText = re.replace(tagText, "")

        return if (newTagText.contains(' ')) {
            newTagText.split(' ')
        } else {
            listOf(newTagText)
        }
    }

    private fun addLink(link: String) {
        link.trim()
        val chip = Chip(requireContext())

        chip.isCloseIconVisible = true

        val splitLink = link.split(".")
        if (splitLink.size > 2) {
            if (splitLink[2].length > 3) {
                val linkText = link.substring(0, 12) + "..."
                chip.text = linkText
                // trim
            } else {
                chip.text = link
            }
        } else {
            chip.text = link
        }

        if (link.contains("github.com")) {
            chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_github)
        }

        chip.isCheckable = false
        chip.setOnCloseIconClickListener {
            binding.projectLinksContainer.removeView(chip)
        }

        binding.projectLinksContainer.addView(chip, 0)
    }

    private fun updateLayoutOnImagesLoaded() {
        binding.addImagesBtn.hide()
        binding.imagesEditorLayout.show()
    }

    private fun updateBtnOnImageCleared() {
        binding.addImagesBtn.show()
        binding.imagesEditorLayout.hide()
    }

}