package com.jamid.codesquare.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentEditProfileBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.ui.MainActivity

class EditProfileFragment: Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var profileImage: String? = null
    private var loadingDialog: AlertDialog? = null
    private var firstTime: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_profle_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.edit_profile_save -> {
                if (!validateUser())
                    return true

                val username = binding.usernameText.editText?.text.toString()

                val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
                val loadingLayoutBinding = LoadingLayoutBinding.bind(loadingLayout)

                loadingLayoutBinding.loadingText.text = "Updating profile. Please wait ..."

                val currentUser = viewModel.currentUser.value!!

                loadingDialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(loadingLayout)
                    .setCancelable(false)
                    .show()

                val changes = mutableMapOf<String, Any?>()

                val name = binding.nameText.editText?.text.toString()

                if (currentUser.name != name) {
                    changes["name"] = name
                }

                val about = binding.aboutText.editText?.text.toString()

                if (currentUser.about != about) {
                    changes["about"] = about
                }

                val tag = binding.tagText.editText?.text.toString()

                if (currentUser.tag != tag) {
                    changes["tag"] = tag
                }

                val interests = getInterests()

                if (currentUser.interests != interests) {
                    changes["interests"] = interests
                }

                if (currentUser.photo != profileImage) {
                    changes["photo"] = profileImage
                }

                if (currentUser.username != username) {
                    viewModel.checkIfUsernameTaken(username) {
                        if (it.isSuccessful) {
                            val snapshot = it.result
                            if (snapshot.isEmpty) {
                                // no username .. good to go
                                changes["username"] = username

                                updateUser(currentUser.id, changes)
                            } else {
                                binding.usernameText.isErrorEnabled = true
                                binding.usernameText.error = "Username already exists"
                            }
                        } else {
                            toast("Something went wrong while uploading changes.")
                        }
                    }
                } else {
                    updateUser(currentUser.id, changes)
                }

                true
            }
            else -> true
        }
    }

    private fun updateUser(userId: String, changes: Map<String, Any?>) {
        if (changes.isEmpty()) {
            findNavController().navigateUp()
        } else {
            viewModel.updateUser(userId, changes) { it1 ->
                loadingDialog?.dismiss()
                if (it1.isSuccessful) {
                    viewModel.updateUserLocally(changes)
                    toast("Saved changes successfully")
                    viewModel.setCurrentImage(null)
                    findNavController().navigateUp()
                } else {
                    toast("Something went wrong. Try again.")
                }
            }
        }
    }

    private fun getInterests(): List<String> {
        val interests = mutableListOf<String>()
        for (child in binding.interestsGroup.children) {
            val chip = child as Chip
            val interest = chip.text.toString()
            if (interest != "Add Interest") {
                interests.add(interest)
            }
        }
        return interests
    }

    private fun validateUser(): Boolean {
        val nameText = binding.nameText.editText?.text

        if (nameText.isNullOrBlank()) {
            toast("Name cannot be empty")
            return false
        }

        if (nameText.toString().length !in 4..30) {
            toast("Name is either too short or too long")
            return false
        }

        val usernameText = binding.usernameText.editText?.text

        if (usernameText.isNullOrBlank()) {
            toast("Username cannot be empty")
            return false
        }

        if (usernameText.toString().contains(" ")) {
            toast("Username cannot contain spaces")
            return false
        }

        if (usernameText.toString().length !in 4..16) {
            toast("Username is either too short or too long")
            return false
        }

        val aboutText = binding.aboutText.editText?.text
        if (!aboutText.isNullOrBlank() && aboutText.toString().length > 240) {
            toast("The about text is too long.")
            return false
        }

        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        val mainProgress = activity.findViewById<ProgressBar>(R.id.main_progress_bar)

        binding.userImg.setOnClickListener {
            val popupMenu = PopupMenu(activity, it)

            popupMenu.inflate(R.menu.image_menu)

            popupMenu.setOnMenuItemClickListener { it1 ->
                when (it1.itemId) {
                    R.id.select_image -> {
                        (activity as MainActivity).selectImage()
                    }
                    R.id.remove_image -> {
                        viewModel.setCurrentImage(null)
                    }
                }
                true
            }

            popupMenu.show()
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
            if (currentUser != null) {

                profileImage = currentUser.photo

                binding.userImg.setImageURI(profileImage)

                binding.nameText.editText?.setText(currentUser.name)
                binding.usernameText.editText?.setText(currentUser.username)

                binding.tagText.editText?.setText(currentUser.tag)

                binding.aboutText.editText?.setText(currentUser.about)

                addInterests(currentUser.interests)
            }
        }

        viewModel.currentImage.observe(viewLifecycleOwner) {
            mainProgress.show()
            if (it != null) {
                val currentUser = viewModel.currentUser.value!!
                viewModel.uploadImage(currentUser.id, it) { downloadUri ->
                    mainProgress.hide()
                    if (downloadUri != null) {
                        profileImage = downloadUri.toString()
                        binding.userImg.setImageURI(profileImage)
                    } else {
                        profileImage = null
                        binding.userImg.setImageURI(profileImage)
                        toast("Something went wrong while uploading the profile picture.")
                    }
                }
            } else {
                mainProgress.hide()
                profileImage = null
                binding.userImg.setImageURI(profileImage)

                if (firstTime) {
                    firstTime = false
                    profileImage = viewModel.currentUser.value?.photo
                    binding.userImg.setImageURI(profileImage)
                }
            }
        }

        binding.addInterestBtn.setOnClickListener {

            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add interest .. "

            MaterialAlertDialogBuilder(requireContext())
                .setView(inputLayout)
                .setTitle("Add Interest")
                .setMessage("Adding interest helps us to search for related projects for you.")
                .setPositiveButton("Add") { _, _ ->
                    val interestText = inputLayoutBinding.inputTextLayout.text
                    if (!interestText.isNullOrBlank()) {
                        val interest = interestText.toString()
                        addInterest(interest)
                    }
                }
                .setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }
                .show()
        }

    }

    private fun addInterests(interests: List<String>) {
        if (binding.interestsGroup.childCount != 1) {
            binding.interestsGroup.removeViews(0, binding.interestsGroup.childCount - 1)
        }
        for (interest in interests) {
            addInterest(interest)
        }
    }

    private fun addInterest(interest: String) {
        interest.trim()
        val chip = Chip(requireContext())
        chip.text = interest
        chip.isCheckable = false
        chip.setOnCloseIconClickListener {
            binding.interestsGroup.removeView(chip)
        }
        binding.interestsGroup.addView(chip, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

}