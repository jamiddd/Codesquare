package com.jamid.codesquare.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentEditProfileBinding
import com.jamid.codesquare.ui.DefaultProfileImageSheet
import com.jamid.codesquare.ui.InputSheetFragment
import com.jamid.codesquare.ui.MessageDialogFragment

@ExperimentalPagingApi
class EditProfileFragment: Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var profileImage: String = userImages.random()
    private var loadingFragment: MessageDialogFragment? = null
    private lateinit var currentUser: User
    private val needToUpdate = MutableLiveData<Boolean>()

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
                val updatedUser = currentUser.copy()

                loadingFragment = MessageDialogFragment.builder( getString(R.string.profile_upload_loading_text))
                    .setIsHideable(false)
                    .setIsDraggable(false)
                    .shouldShowProgress(true)
                    .build()

                loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)

                val changes = mutableMapOf(
                    "name" to binding.nameText.editText?.text.toString(),
                    "about" to binding.aboutText.editText?.text.toString(),
                    "tag" to binding.tagText.editText?.text.toString(),
                    "interests" to getInterests(),
                    "photo" to  profileImage
                )

                updatedUser.name = changes["name"] as String
                updatedUser.about = changes["about"] as String
                updatedUser.tag = changes["tag"] as String
                updatedUser.interests = getInterests()
                updatedUser.photo = profileImage

                if (currentUser.username != username) {
                    viewModel.checkIfUsernameTaken(username) {
                        if (it.isSuccessful) {
                            val snapshot = it.result
                            if (snapshot.isEmpty) {
                                // no username .. good to go
                                changes["username"] = username
                                updatedUser.username = username

                                updateUser(updatedUser, changes)
                            } else {
                                binding.usernameText.isErrorEnabled = true
                                binding.usernameText.error = "Username already exists"
                            }
                        } else {
                            toast("Something went wrong while uploading changes.")
                        }
                    }
                } else {
                    updateUser(updatedUser, changes)
                }

                true
            }
            else -> true
        }
    }

    private fun updateUser(updatedUser: User, changes: Map<String, Any?>) {
        if (changes.isEmpty()) {
            findNavController().navigateUp()
        } else {
            viewModel.updateUser(updatedUser, changes) { it1 ->
                loadingFragment?.dismiss()
                if (it1.isSuccessful) {
                    Snackbar.make(binding.root, "Saved changes successfully", Snackbar.LENGTH_LONG).show()
                    viewModel.setCurrentImage(null)
                    findNavController().navigateUp()
                } else {
                    Snackbar.make(binding.root, "Something went wrong. Try again.", Snackbar.LENGTH_LONG).show()
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
            binding.nameText.isErrorEnabled = true
            binding.nameText.error = "Name cannot be empty"
            return false
        }

        if (nameText.toString().length !in 4..30) {
            binding.nameText.isErrorEnabled = true
            binding.nameText.error = "Name is either too short or too long"
            return false
        }

        val usernameText = binding.usernameText.editText?.text

        if (usernameText.isNullOrBlank()) {
            binding.usernameText.isErrorEnabled = true
            binding.usernameText.error = "Username cannot be empty"
            return false
        }

        if (usernameText.toString().contains(" ")) {
            binding.usernameText.isErrorEnabled = true
            binding.usernameText.error = "Username cannot contain spaces"
            return false
        }


        if (usernameText.toString().length !in 4..16) {
            binding.usernameText.isErrorEnabled = true
            binding.usernameText.error = "Username is either too short or too long"
            return false
        }

        val aboutText = binding.aboutText.editText?.text
        if (!aboutText.isNullOrBlank() && aboutText.toString().length > 240) {
            binding.aboutText.isErrorEnabled = true
            binding.aboutText.error = "The about text is too long."
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
        currentUser = UserManager.currentUser

        binding.userImg.setOnClickListener(onImageUpdateClick)

        // setting views on load
        profileImage = currentUser.photo
        binding.userImg.setImageURI(profileImage)
        binding.nameText.editText?.setText(currentUser.name)
        binding.usernameText.editText?.setText(currentUser.username)
        binding.tagText.editText?.setText(currentUser.tag)
        binding.aboutText.editText?.setText(currentUser.about)
        addInterests(currentUser.interests)

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            onNewImageOrNullSet(image)

            if (image != null) {
                setProfileImage(image)

                if (image.authority?.contains("googleapis.com") == true) {
                    // already uploaded image, no need to upload
                    onImageUploaded()
                } else {
                    uploadImage(image)
                }
            }
        }

        binding.addInterestBtn.setOnClickListener {

            val inputSheet = InputSheetFragment.builder("Adding interest helps us to search for related projects for you.")
                .setTitle("Add Interest")
                .setHint("Add interest ... ")
                .setPositiveButton("Add") { _, _, s ->
                    if (s.isNotBlank()) {
                        addInterest(s)
                    }
                }
                .setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.build()

            inputSheet.show(childFragmentManager, InputSheetFragment.TAG)

        }

        binding.nameText.editText?.doAfterTextChanged {
            binding.nameText.isErrorEnabled = false
            binding.nameText.error = null
            if (!it.isNullOrBlank()) {
                checkState()
            } else {
                disableSaveBtn()
            }
        }

        binding.usernameText.editText?.doAfterTextChanged {
            binding.usernameText.isErrorEnabled = false
            binding.usernameText.error = null
            if (!it.isNullOrBlank()) {
                checkState()
            } else {
                disableSaveBtn()
            }
        }

        binding.aboutText.editText?.doAfterTextChanged {
            binding.aboutText.isErrorEnabled = false
            binding.aboutText.error = null
            if (!it.isNullOrBlank()) {
                checkState()
            } else {
                if (currentUser.about == "") {
                    disableSaveBtn()
                } else {
                    enableSaveBtn()
                }
            }
        }

        binding.tagText.editText?.doAfterTextChanged {
            binding.tagText.isErrorEnabled = false
            binding.tagText.error = null
            if (!it.isNullOrBlank()) {
                checkState()
            } else {
                if (currentUser.tag == "") {
                    disableSaveBtn()
                } else {
                    enableSaveBtn()
                }
            }
        }


    }

    private fun checkState() {
        onChange()
    }

    private fun shouldDisableSaveBtn(): Boolean {
        val e1 = currentUser.name == binding.nameText.editText?.text.toString()
        val e2 = currentUser.photo == profileImage
        val e3 = currentUser.tag == binding.tagText.editText?.text.toString()
        val e4 = currentUser.username == binding.usernameText.editText?.text.toString()
        val e5 = currentUser.about == binding.aboutText.editText?.text.toString()
        val e6 = currentUser.interests.sorted().toString() == getInterests().sorted().toString()

        return e1 && e2 && e3 && e4 && e5 && e6
    }

    private fun disableSaveBtn() {
        needToUpdate.postValue(false)
    }

    private fun enableSaveBtn() {
        needToUpdate.postValue(true)
    }

    private fun uploadImage(image: Uri) {
        viewModel.uploadImage(UserManager.currentUserId, image) { downloadUri ->
            onImageUploaded()
            if (downloadUri != null) {
                setProfileImage(downloadUri.toString())
            } else {
                onImageUploadFailed(Exception("Image could not be uploaded"))
            }
        }
    }

    private fun removeImage() {
        profileImage = userImages.random()
        setProfileImage(profileImage)
    }

    private fun onImageUploadFailed(e: Exception) {
        removeImage()
        toast(e.message.toString())
    }


    private fun onImageUploaded() {
        binding.userImageProgress.hide()
        binding.userImg.colorFilter = null
    }

    private fun onNewImageOrNullSet(image: Uri? = null) {

        onChange()

        val currentUser = UserManager.currentUser

        val colorFilter =  ContextCompat.getColor(
            requireContext(),
            R.color.darkest_transparent
        )

        // show that there is some progress
        binding.userImageProgress.show()
        binding.userImg.setColorFilter(colorFilter)

        // if there was no image
        if (image == null) {
            // update UI
            binding.userImg.colorFilter = null
            binding.userImageProgress.hide()

            // setting the already existing image as profile image
            setProfileImage(currentUser.photo)
        }

    }

    private fun setProfileImage(image: Uri) {
        setProfileImage(image.toString())
    }

    private fun setProfileImage(image: String) {
        profileImage = image
        binding.userImg.setImageURI(profileImage)
    }

    private fun addInterests(interests: List<String>) {
        if (binding.interestsGroup.childCount != 1) {
            binding.interestsGroup.removeViews(0, binding.interestsGroup.childCount - 1)
        }
        for (interest in interests) {
            addInterest(interest)
        }
    }

    private val onImageUpdateClick = View.OnClickListener {
        val fragment = DefaultProfileImageSheet()
        fragment.show(requireActivity().supportFragmentManager, "DefaultProfileImage")
    }

    private fun addInterest(interest: String) {
        interest.trim()
        val chip = Chip(requireContext())

        chip.apply {
            text = interest
            isCheckable = false
            isCloseIconVisible = true
        }

        chip.setOnCloseIconClickListener {
            binding.interestsGroup.removeView(chip)
            onChange()
        }

        binding.interestsGroup.addView(chip, 0)

        onChange()

    }

    private fun onChange() {
        if (shouldDisableSaveBtn()) {
            needToUpdate.postValue(false)
        } else {
            needToUpdate.postValue(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

    companion object {
        const val TAG = "EditProfileFragment"
    }

}