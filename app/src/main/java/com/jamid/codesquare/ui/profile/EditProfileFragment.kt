package com.jamid.codesquare.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentEditProfileBinding
import com.jamid.codesquare.listeners.AddTagsListener
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class EditProfileFragment: Fragment(), AddTagsListener, InterestItemClickListener {

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

                val userEditForm = viewModel.userEditForm.value ?: return true

                val updatedUser = currentUser.copy()

                val username = userEditForm.username

                val changes = mutableMapOf(
                    "name" to userEditForm.name,
                    "about" to userEditForm.about,
                    "tag" to userEditForm.tag,
                    "interests" to userEditForm.interests,
                    "photo" to  userEditForm.photo
                )

                updatedUser.name = userEditForm.name
                updatedUser.about = userEditForm.about
                updatedUser.tag = userEditForm.tag
                updatedUser.interests = userEditForm.interests
                updatedUser.photo = userEditForm.photo

                if (currentUser.username != username) {
                    viewModel.checkIfUsernameTaken(username) {
                        if (it.isSuccessful) {
                            val snapshot = it.result
                            if (snapshot.isEmpty) {
                                loadingFragment = MessageDialogFragment.builder( getString(R.string.profile_upload_loading_text))
                                    .setIsHideable(false)
                                    .setIsDraggable(false)
                                    .shouldShowProgress(true)
                                    .build()

                                loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)

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

                    Snackbar.make(binding.root, "Saved changes successfully", Snackbar.LENGTH_LONG)
                        .setBehavior(NoSwipeBehavior())
                        .show()
                    viewModel.setCurrentImage(null)
                    findNavController().navigateUp()
                } else {
                    Snackbar.make(binding.root, "Something went wrong. Try again.", Snackbar.LENGTH_LONG)
                        .setBehavior(NoSwipeBehavior())
                        .show()
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
        currentUser = UserManager.currentUser
        return binding.root
    }


    private fun setFormObserver() {

        binding.nameText.editText?.doAfterTextChanged {
            binding.nameText.isErrorEnabled = false
            binding.nameText.error = null

            val name = if (it.isNullOrBlank()) {
                ""
            } else {
                it.trim().toString()
            }

            viewModel.setUserEditFormName(name)
        }

        binding.usernameText.editText?.doAfterTextChanged {
            binding.usernameText.isErrorEnabled = false
            binding.usernameText.error = null

            val username = if (it.isNullOrBlank()) {
                ""
            } else {
                it.trim().toString()
            }

            viewModel.setUserEditFormUsername(username)
        }

        binding.tagText.editText?.doAfterTextChanged {
            binding.tagText.isErrorEnabled = false
            binding.tagText.error = null

            val tag = if (it.isNullOrBlank()) {
                ""
            } else {
                it.trim().toString()
            }

            viewModel.setUserEditFormTag(tag)
        }

        binding.aboutText.editText?.doAfterTextChanged {
            binding.aboutText.isErrorEnabled = false
            binding.aboutText.error = null

            val about = if (it.isNullOrBlank()) {
                ""
            } else {
                it.trim().toString()
            }

            viewModel.setUserEditFormAbout(about)
        }

        binding.interestsGroup.onChildrenChanged {
            val interests = getInterests()
            viewModel.setUserEditFormInterests(interests)
        }

    }

    private fun setFormOnStart() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            val userEditForm = viewModel.userEditForm.value
            if (userEditForm == null) {
                val currentUser = UserManager.currentUser
                viewModel.setUserEditForm(currentUser)

                setFormOnStart()
            } else {
                binding.nameText.editText?.setText(userEditForm.name)
                binding.usernameText.editText?.setText(userEditForm.username)
                binding.tagText.editText?.setText(userEditForm.tag)
                binding.aboutText.editText?.setText(userEditForm.about)

                addInterests(userEditForm.interests)
                setProfileImage(userEditForm.photo)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFormOnStart()
        setFormObserver()

        binding.userImg.setOnClickListener(onImageUpdateClick)

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

            val frag = AddTagsFragment.builder()
                .setTitle("Add interests")
                .setListener(this)
                .build()

            frag.show(childFragmentManager, AddTagsFragment.TAG)

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
        profileImage = ""
        setProfileImage(profileImage)
    }

    private fun onImageUploadFailed(e: Exception) {
        removeImage()
    }

    private fun onImageUploaded() {
        binding.userImageProgress.hide()
        binding.userImg.colorFilter = null
    }

    private fun onNewImageOrNullSet(image: Uri? = null) {
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
            setProfileImage("")
        }

    }

    private fun setProfileImage(image: Uri) {
        setProfileImage(image.toString())
    }

    private fun setProfileImage(image: String) {
        viewModel.setUserEditFormProfilePhoto(image)
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
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip

        chip.apply {
            text = interest
            isCheckable = false
            isCloseIconVisible = true
        }

        chip.setOnCloseIconClickListener {
            binding.interestsGroup.removeView(chip)
        }

        binding.interestsGroup.addView(chip, 0)

        chip.updateLayoutParams<FlexboxLayout.LayoutParams> {
            marginEnd = resources.getDimension(R.dimen.generic_len).toInt()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

    companion object {
        const val TAG = "EditProfileFragment"
    }

    override fun onTagsSelected(tags: List<String>) {
        val existingInterests = getInterests()
        val allInterests = mutableListOf<String>()

        allInterests.addAll(existingInterests)
        allInterests.addAll(tags)

        addInterests(allInterests.distinct())
    }



    override fun onInterestClick(interestItem: InterestItem) {
        if (interestItem.isChecked) {
            viewModel.uncheckInterestItem(interestItem)
        } else {
            viewModel.checkInterestItem(interestItem)
        }
    }




}