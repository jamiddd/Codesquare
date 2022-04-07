package com.jamid.codesquare.ui

import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import android.webkit.URLUtil
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.fragment.app.Fragment
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
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentCreateProjectBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class CreateProjectFragment: Fragment(R.layout.fragment_create_project) {

    private lateinit var binding: FragmentCreateProjectBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var imageAdapter: ImageAdapter
    private var imagesCount = 0

    private var isUpdateMode = false

    private fun onUpdateProject(project: Project) {
        requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar).title = getString(R.string.update_project)
        isUpdateMode = true
        viewModel.setCurrentProject(project)
    }

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

                val d = View.inflate(requireContext(), R.layout.loading_layout, null)

                val loadingLayoutBinding = LoadingLayoutBinding.bind(d)
                val activity = requireActivity()

                if (isUpdateMode) {
                    loadingLayoutBinding.loadingText.text = getString(R.string.update_project_loading)

                    val dialogFragment = MessageDialogFragment.builder(getString(R.string.update_project_loading))
                        .setIsHideable(false)
                        .setIsDraggable(false)
                        .shouldShowProgress(true)
                        .build()

                    dialogFragment.show(childFragmentManager, MessageDialogFragment.TAG)

                    viewModel.updateProject { newProject, task ->
                        activity.runOnUiThread {
                            dialogFragment.dismiss()
                            if (task.isSuccessful) {
                                val mainRoot = activity.findViewById<CoordinatorLayout>(R.id.main_container_root)
                                Snackbar.make(mainRoot, "Project updated successfully", Snackbar.LENGTH_LONG).show()

                                viewModel.deleteProjectById(newProject.id)

                                // updating local project
                                viewModel.insertProjects(newProject)
                                viewModel.setCurrentProject(null)
                                findNavController().navigateUp()
                            } else {
                                viewModel.setCurrentError(task.exception)
                            }
                        }
                    }
                } else {
                    loadingLayoutBinding.loadingText.text = getString(R.string.create_project_loading)

                    val dialogFragment = MessageDialogFragment.builder(getString(R.string.create_project_loading))
                        .setIsDraggable(false)
                        .setIsHideable(false)
                        .shouldShowProgress(true)
                        .build()

                    dialogFragment.show(childFragmentManager, MessageDialogFragment.TAG)

                    viewModel.createProject {
                        requireActivity().runOnUiThread {
                            dialogFragment.dismiss()
                            if (it.isSuccessful) {
                                findNavController().navigateUp()
                                viewModel.setCurrentProject(null)

                                val mainRoot = activity.findViewById<CoordinatorLayout>(R.id.main_container_root)
                                Snackbar.make(mainRoot, "Project uploaded successfully.", Snackbar.LENGTH_LONG).show()

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

        if (binding.projectTitleText.editText?.text.toString().length !in 6..100) {
            toast("Title must be longer than 5 characters and shorter than 100 characters.")
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
        val prevProject = arguments?.getParcelable<Project>(PREVIOUS_PROJECT)
        if (prevProject != null) {
            onUpdateProject(prevProject)
        } else {
            Log.d(TAG, "onViewCreated: Creating new project")
            val newProject = Project.newInstance(currentUser)
            viewModel.setCurrentProject(newProject)
        }

        binding.userName.text = currentUser.name
        binding.userImg.setImageURI(currentUser.photo)

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
            Log.d(TAG, "onViewCreated: Current project has changed")
            if (currentProject != null) {
                val images = currentProject.images
                if (images.isNotEmpty()) {

                    Log.d(TAG, "onViewCreated: Images not empty")

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

                    Log.d(TAG, "onViewCreated: Images empty")

                    imageAdapter.submitList(emptyList())
                    updateBtnOnImageCleared()

                    binding.removeCurrentImgBtn.hide()
                }


                binding.projectTitleText.editText?.setText(currentProject.name)

                binding.projectContentText.editText?.setText(currentProject.content)

                if (currentProject.location.address.isNotBlank()) {
                    binding.projectLocationText.text = currentProject.location.address

                    binding.projectLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_location_on_small, 0, 0, 0)

                    binding.projectLocationText.setOnClickListener {

                        val frag = MessageDialogFragment.builder("Are you sure you want to remove location attached to this project?")
                            .setTitle("Removing location ...")
                            .setPositiveButton("Remove") { _, _ ->
                                viewModel.setCurrentProjectLocation(Location())
                            }
                            .setNegativeButton("Cancel") { d, _ ->
                                d.dismiss()
                            }
                            .build()

                        frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
                    }
                } else {
                    binding.projectLocationText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_add_location_small, 0, 0, 0)
                    binding.projectLocationText.text = getString(R.string.add_location)

                    binding.projectLocationText .setOnClickListener {
                        viewModel.setCurrentProjectTitle(getTitle())
                        viewModel.setCurrentProjectContent(getContent())

                        val tags = getTags()
                        viewModel.setCurrentProjectTags(tags)

                        val frag = LocationFragment()
                        frag.show(requireActivity().supportFragmentManager, "LocationFragment")
                    }
                }

                addTags(currentProject.tags)
                addLinks(currentProject.sources)

            } else {
                updateBtnOnImageCleared()
            }
        }

        binding.removeCurrentImgBtn.setOnClickListener {
            val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deleteProjectImageAtPosition(pos)
        }

        binding.addImagesBtn.setOnClickListener {
            (activity as MainActivity).selectImage(ImageSelectType.IMAGE_PROJECT)
        }

        binding.clearAllImagesBtn.setOnClickListener {
            viewModel.setCurrentProjectImages(emptyList())
        }

        binding.addMoreImagesBtn.setOnClickListener {
            (activity as MainActivity).selectImage(ImageSelectType.IMAGE_PROJECT)
        }

        binding.addTagBtn.setOnClickListener {
            val frag = AddTagFragment()
            frag.show(requireActivity().supportFragmentManager, "AddTagFragment")
        }

        binding.addLinkBtn.setOnClickListener {

            val frag = InputSheetFragment.builder("Add links to your existing project sources or files. Ex. Github, Google drive, etc.")
                .setTitle("Add link to project")
                .setHint("Ex: https://wwww.google.com")
                .setPositiveButton("Add link") { _, _, s ->
                    if (URLUtil.isValidUrl(s)) {
                        addLink(s)
                    } else {
                        toast("Not a proper link.")
                    }
                }.setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.build()

            frag.show(requireActivity().supportFragmentManager, "InputSheetFrag")
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

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isCreatingProjectFirstTime = sharedPref.getBoolean("isCreatingProjectFirstTime", true)
        if (isCreatingProjectFirstTime) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                if (this@CreateProjectFragment.isVisible) {
                    val frag = MessageDialogFragment.builder(getString(R.string.create_project_info))
                        .setTitle("Creating your project .. ")
                        .build()

                    frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
                    val editor = sharedPref.edit()
                    editor.putBoolean("isCreatingProjectFirstTime", false)
                    editor.apply()
                }
            }
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

    private fun addLinks(links: List<String>) {
        if (binding.projectLinksContainer.childCount != 1) {
            binding.projectLinksContainer.removeViews(0, binding.projectLinksContainer.childCount - 1)
        }
        for (link in links) {
            addLink(link)
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

            val tags = getTags()
            viewModel.setCurrentProjectTags(tags)
        }
        binding.projectTagsContainer.addView(chip, 0)
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