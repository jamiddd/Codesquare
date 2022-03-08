package com.jamid.codesquare.ui

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.webkit.URLUtil
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentUpdateProjectBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class UpdateProjectFragment: Fragment() {

    private lateinit var binding: FragmentUpdateProjectBinding
    private lateinit var imageAdapter: ImageAdapter
    private var imagesCount = 0
    private val viewModel: MainViewModel by activityViewModels()
    private var currentPos: Int = 0

    private val updateProjectViewModel: UpdateProjectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.update_project_menu, menu)
    }

    private fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        for (child in binding.projectTagsGroup.children) {
            val chip = child as Chip
            val tag = chip.text.toString()
            if (tag != "Add tag") {
                tags.add(tag)
            }
        }
        return tags
    }

    private fun getLinks(): List<String> {
        val links = mutableListOf<String>()
        for (child in binding.projectLinksGroup.children) {
            val chip = child as Chip
            val link = chip.text.toString()
            if (link != "Add link") {
                links.add(link)
            }
        }
        return links
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.update_project -> {

                if (imagesCount <= 0) {
                    toast("Must contain at least 1 image")
                    return true
                }

                if (binding.projectContentText.editText?.text?.isBlank() == true) {
                    toast("Content cannot empty.")
                    return true
                }

                if (binding.projectNameText.editText?.text?.isBlank() == true) {
                    toast("Title cannot be empty.")
                    return true
                }

                if (binding.projectNameText.editText?.text.toString().length !in 6..30) {
                    toast("Title must be longer than 5 characters and shorter than 31 characters.")
                    return false
                }

                val view = layoutInflater.inflate(R.layout.loading_layout, null, false)
                val loadingLayoutBinding = LoadingLayoutBinding.bind(view)
                loadingLayoutBinding.loadingText.text = getString(R.string.update_project_loading)

                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setCancelable(false)
                    .show()


                val currentProject = updateProjectViewModel.currentProject.value
                if (currentProject != null) {

                    currentProject.name = binding.projectNameText.editText?.text?.trim().toString()
                    currentProject.content = binding.projectContentText.editText?.text?.trim().toString()
                    currentProject.tags = getTags()
                    currentProject.sources = getLinks()

                    viewLifecycleOwner.lifecycleScope.launch {
                        FireUtility.updateProject(currentProject) { newProject, task ->
                            dialog.dismiss()
                            if (task.isSuccessful) {
                                val activity = requireActivity()
                                activity.runOnUiThread {
                                    val mainRoot = activity.findViewById<CoordinatorLayout>(R.id.main_container_root)
                                    Snackbar.make(mainRoot, "Project updated successfully", Snackbar.LENGTH_LONG).show()

                                    viewModel.multipleImagesContainer.postValue(emptyList())

                                    // updating local project
                                    viewModel.insertProjectsWithoutProcessing(newProject)

                                    findNavController().navigateUp()
                                }

                            } else {
                                viewModel.setCurrentError(task.exception)
                            }
                        }
                    }

                } else {
                    Log.i(TAG, "Attempted to update project when current project was not initialized.")
                }


                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUpdateProjectBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val project = arguments?.getParcelable<Project>(PROJECT) ?: return

        imageAdapter = ImageAdapter()
        val helper = LinearSnapHelper()
        val imagesManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            onFlingListener = null
            helper.attachToRecyclerView(this)
            layoutManager = imagesManager
        }

        a(project)

        binding.removeCurrentImgBtn.setOnClickListener {
            val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
            updateProjectViewModel.removeImageAtPosition(pos)
        }

        binding.addMoreImagesBtn.setOnClickListener {
            (activity as MainActivity).selectMultipleImages()
        }

        binding.clearAllImagesBtn.setOnClickListener {
            updateProjectViewModel.removeAllImagesFromProject()
        }

        // EXTRAS
        viewModel.multipleImagesContainer.observe(viewLifecycleOwner) { multipleImages ->
            if (!multipleImages.isNullOrEmpty()) {
                updateProjectViewModel.addImagesToExistingProject(multipleImages.map { it.toString() })
            }
        }

        binding.projectImagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = imagesManager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                    currentPos = pos
                    setCounterText()
                }
            }
        })

        binding.addLinkBtn.setOnClickListener {
            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add link .. "
            inputLayoutBinding.inputTextLayout.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            inputLayoutBinding.inputTextLayout.hint = "Ex: https://wwww.google.com"

            val alertDialog = MaterialAlertDialogBuilder(requireContext())
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

        binding.addTagBtn.setOnClickListener {
            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add tag .. "

            val alertDialog = MaterialAlertDialogBuilder(requireContext())
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

    }

    private fun setCounterText() {
        val counterText = "${currentPos + 1}/$imagesCount"
        binding.imageCounter.text = counterText
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

    private fun addTag(tag: String) {
        tag.trim()
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            binding.projectTagsGroup.removeView(chip)
        }
        binding.projectTagsGroup.addView(chip, 0)
    }

    fun a(project: Project) {
        updateProjectViewModel.currentProject.observe(viewLifecycleOwner) { currentProject ->
            if (currentProject != null) {

                binding.projectNameText.editText?.setText(currentProject.name)
                binding.projectContentText.editText?.setText(currentProject.content)

                if (binding.projectLinksGroup.childCount != 1) {
                    binding.projectLinksGroup.removeViews(0, binding.projectLinksGroup.childCount - 1)
                }

                for (link in currentProject.sources) {
                    addLink(link)
                }

                if (binding.projectTagsGroup.childCount != 1) {
                    binding.projectTagsGroup.removeViews(0, binding.projectTagsGroup.childCount - 1)
                }

                for (tag in currentProject.tags) {
                    addTag(tag)
                }

                // get the image count for future reference
                imagesCount = currentProject.images.size

                if (imagesCount == 0) {
                    currentPos = -1
                    setCounterText()

                    binding.addMoreImagesBtn.text = getString(R.string.add_images)
                    binding.imageCounter.hide()
                    binding.clearAllImagesBtn.hide()
                    binding.removeCurrentImgBtn.hide()

                    val len = resources.getDimension(R.dimen.unit_len) * 30
                    binding.imagesEditorLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        horizontalBias = 0.5f
                        setMargins(0, len.toInt(), 0, 0)
                    }
                } else {

                    setCounterText()

                    binding.addMoreImagesBtn.text = getString(R.string.add_more_images)
                    binding.imageCounter.show()
                    binding.clearAllImagesBtn.show()
                    binding.removeCurrentImgBtn.show()

                    binding.imagesEditorLayout.updateLayoutParams<ConstraintLayout.LayoutParams>{
                        horizontalBias = 1f
                        setMargins(0)
                    }
                }

                imageAdapter.submitList(project.images)
                imageAdapter.notifyDataSetChanged()
            } else {
                updateProjectViewModel.setCurrentProject(project)
            }
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
            binding.projectLinksGroup.removeView(chip)
        }

        binding.projectLinksGroup.addView(chip, 0)
    }

    companion object {
        const val TAG = "UpdateProjectFragment"
    }

}
