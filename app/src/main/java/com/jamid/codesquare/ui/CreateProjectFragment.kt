package com.jamid.codesquare.ui

import android.os.Bundle
import android.text.InputType
import android.view.*
import android.webkit.URLUtil
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentCreateProjectBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import java.util.regex.Pattern

class CreateProjectFragment: Fragment(R.layout.fragment_create_project) {

    private lateinit var binding: FragmentCreateProjectBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var imageAdapter: ImageAdapter
    private var imagesCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
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
                loadingLayoutBinding.loadingText.text = "Creating project. Please wait ..."

                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setCancelable(false)
                    .show()

                viewModel.createProject {
                    if (it.isSuccessful) {
                        dialog.dismiss()
                        findNavController().navigateUp()
                        viewModel.setCurrentProject(null)
                        toast("Project uploaded successfully.")
                    } else {
                        dialog.dismiss()
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

    private fun validateTag(tag: String): Boolean {
        val p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(tag)
        return !m.matches()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.hide()

        viewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
            if (currentUser != null) {
                binding.userName.text = currentUser.name
                binding.userImg.setImageURI(currentUser.photo)

                if (viewModel.currentProject.value == null) {
                    val newProject = Project.newInstance(currentUser)
                    viewModel.setCurrentProject(newProject)
                }

            } else {
                findNavController().navigateUp()
            }
        }

        imageAdapter = ImageAdapter { a, b ->
            //
        }

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

                    imageAdapter.submitList(images)

                    imagesCount = images.size

                    updateLayoutOnImagesLoaded()

                    binding.imageCounter.text = "1/$imagesCount"

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
                    binding.projectLocationText.text = "Add Location"

                    binding.projectLocationText .setOnClickListener {
                        viewModel.setCurrentProjectTitle(getTitle())
                        viewModel.setCurrentProjectContent(getContent())

                        val tags = getTags()
                        viewModel.setCurrentProjectTags(tags)

                        findNavController().navigate(R.id.action_createProjectFragment_to_locationFragment)
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
                    binding.imageCounter.text = "${pos + 1}/$imagesCount"
                }
            }
        })

    }

    private fun getTitle() = binding.projectTitleText.editText?.text.toString()

    private fun getContent() = binding.projectContentText.editText?.text.toString()

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

    private fun addLinks(links: List<String>) {
        if (binding.projectLinksContainer.childCount != 1) {
            binding.projectLinksContainer.removeViews(0, binding.projectLinksContainer.childCount - 1)
        }
        for (link in links) {
            addLink(link)
        }
    }

    private fun addLink(link: String) {
        link.trim()
        val chip = Chip(requireContext())

        chip.isCloseIconVisible = true

        val splitLink = link.split(".")
        if (splitLink.size > 2) {
            if (splitLink[2].length > 3) {
                chip.text = link.substring(0, 12) + "..."
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

    companion object {
        private const val TAG = "CreateProjectFragment"
    }

}