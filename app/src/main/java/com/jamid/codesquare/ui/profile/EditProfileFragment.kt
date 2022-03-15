package com.jamid.codesquare.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.*
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentEditProfileBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.ui.DefaultProfileImageSheet

@ExperimentalPagingApi
class EditProfileFragment: Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var profileImage: String = userImages.random()
    private var loadingDialog: AlertDialog? = null
    private lateinit var currentUser: User

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

                loadingLayoutBinding.loadingText.text = getString(R.string.profile_upload_loading_text)

                val updatedUser = currentUser.copy()

                loadingDialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(loadingLayout)
                    .setCancelable(false)
                    .show()

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
                loadingDialog?.dismiss()
                if (it1.isSuccessful) {
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

            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Add interest .. "

            val alertDialog = MaterialAlertDialogBuilder(requireContext())
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

            alertDialog.window?.setGravity(Gravity.BOTTOM)

        }

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
//        binding.imageUpdateNextBtn.enable()
//        binding.skipImageUpdateBtn.enable()
        binding.userImg.colorFilter = null
    }

    private fun onNewImageOrNullSet(image: Uri? = null) {
        val currentUser = UserManager.currentUser

        val colorFilter =  ContextCompat.getColor(
            requireContext(),
            R.color.darkest_transparent
        )

        // show that there is some progress
        binding.userImageProgress.show()
        binding.userImg.setColorFilter(colorFilter)

        // disable actions because a work is in progress
//        binding.imageUpdateNextBtn.disable()
//        binding.skipImageUpdateBtn.disable()

        // if there was no image
        if (image == null) {
            // update UI
            binding.userImg.colorFilter = null
            binding.userImageProgress.hide()
//            binding.imageUpdateNextBtn.enable()
//            binding.skipImageUpdateBtn.enable()

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
        val lContext = requireContext()

       /* val (backgroundColor, textColor) = if (isNightMode()) {
            val colorPair = colorPalettesNight.random()
            ContextCompat.getColor(lContext, colorPair.first) to
                    ContextCompat.getColor(lContext, colorPair.second)
        } else {
            val colorPair = colorPalettesDay.random()
            ContextCompat.getColor(lContext, colorPair.first) to
                    ContextCompat.getColor(lContext, colorPair.second)
        }*/

        chip.apply {
            text = interest
            isCheckable = false
            isCloseIconVisible = true
           /* chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            setTextColor(textColor)*/
        }

        chip.setOnCloseIconClickListener {
            binding.interestsGroup.removeView(chip)
        }
        binding.interestsGroup.addView(chip, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

    companion object {
        const val TAG = "EditProfileFragment"
    }

}