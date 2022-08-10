package com.jamid.codesquare.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentEditProfileBinding
import com.jamid.codesquare.listeners.AddTagsListener
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.ui.AddTagsFragment
import com.jamid.codesquare.ui.DefaultProfileImageSheet
import com.jamid.codesquare.ui.MessageDialogFragment
import kotlinx.coroutines.Job
// something simple
class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>(), AddTagsListener, InterestItemClickListener {

    private var profileImage: String? = null

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

                val name = binding.nameText.editText!!.text.trim().toString()
                val username = binding.usernameText.editText!!.text.trim().toString()
                val tag = binding.tagText.editText!!.text.trim().toString()
                val about = binding.aboutText.editText!!.text.trim().toString()
                val interests = getInterests()

                val (photo, isPreUploadedImage) = if (profileImage == null) {
                    val s: Uri? = null
                    s to false
                } else {
                    val p = profileImage!!.toUri()
                    if (p.scheme == "content") {
                        p to false
                    } else {
                        p to true
                    }
                }

                val userUpdate = UserUpdate(username, name, photo, isPreUploadedImage, tag, about, interests)

                update(userUpdate)
                true
            }
            else -> true
        }
    }


    private fun update(userUpdate: UserUpdate) = runOnBackgroundThread {

        val loading = MessageDialogFragment.builder(getString(R.string.profile_upload_loading_text))
            .setProgress()
            .build()

        loading.show(childFragmentManager, "LoadingFragment")

/*
val userUpdate = if (profileImage != null) {
val image = profileImage!!.toUri()
if (image.scheme == "content") {
UserUpdate(
userEditForm.username,
userEditForm.name,
profileImage!!.toUri(),
false,
userEditForm.tag,
userEditForm.about,
userEditForm.interests
)
} else {
UserUpdate(
userEditForm.username,
userEditForm.name,
profileImage!!.toUri(),
true,
userEditForm.tag,
userEditForm.about,
userEditForm.interests
)
}
} else {
UserUpdate(
userEditForm.username,
userEditForm.name,
null,
false,
userEditForm.tag,
userEditForm.about,
userEditForm.interests
)
}
*/

        when (val result = FireUtility.updateUser3(userUpdate)) {
            is Result.Error -> {
                when (result.exception){
                    is UniqueUsernameException -> {
                        binding.usernameText.isErrorEnabled = true
                        binding.usernameText.error = "Username is already taken"
                    }
                    is ImageUploadException -> {
                        toast("Something went wrong while trying to upload profile picture.")
                    }
                }
            }
            is Result.Success -> {
                runOnMainThread {
                    loading.dismiss()
                    findNavController().navigateUp()
                }
            }
        }

    }

    private fun getInterests(): List<String> {
        val interests = mutableListOf<String>()
        for (child in binding.interestsGroup.children) {
            val chip = child as Chip
            val interest = chip.text.toString()
            if (interest != getString(R.string.add_interest)) {
                interests.add(interest)
            }
        }
        return interests
    }
   
    private fun setFormObserver() {

        binding.nameText.editText?.doAfterTextChanged {
            onTextChange()
        }

        binding.usernameText.editText?.doAfterTextChanged {
            onTextChange()
        }

        binding.tagText.editText?.doAfterTextChanged {
            onTextChange()
        }

        binding.aboutText.editText?.doAfterTextChanged {
            onTextChange()
        }


    }

    private var job: Job? = null

    private fun onTextChange() {

        activity.binding.mainToolbar.menu.getItem(0).isEnabled = false

        binding.nameText.removeError()
        binding.usernameText.removeError()
        binding.tagText.removeError()
        binding.aboutText.removeError()

        job?.cancel()
        job = viewModel.updateUserFormChanged(
            binding.nameText.editText!!.text.toString(),
            binding.usernameText.editText!!.text.toString(),
            binding.tagText.editText!!.text.toString(),
            binding.aboutText.editText!!.text.toString()
        )
    }

    private fun setFormOnStart() {
        val currentUser = UserManager.currentUser
        binding.nameText.editText?.setText(currentUser.name)
        binding.usernameText.editText?.setText(currentUser.username)
        binding.tagText.editText?.setText(currentUser.tag)
        binding.aboutText.editText?.setText(currentUser.about)

        addInterests(currentUser.interests)
        setProfileImage(currentUser.photo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFormOnStart()
        setFormObserver()

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            setProfileImage(image)
        }

        binding.addInterestBtn.setOnClickListener {
            val frag = AddTagsFragment.builder()
                .setTitle("Add interests")
                .setListener(this)
                .build()
            frag.show(childFragmentManager, AddTagsFragment.TAG)
        }

        binding.changePhotoBtn.setOnClickListener {
            val fragment = DefaultProfileImageSheet()
            fragment.show(requireActivity().supportFragmentManager, "DefaultProfileImage")
        }
        
        viewModel.updateUserForm.observe(viewLifecycleOwner) {
            val form = it ?: return@observe

            activity.binding.mainToolbar.menu?.let { menu ->
                if (menu.size > 0) {
                    Log.d(TAG, "onViewCreated: ${form.isValid}")
                    menu.getItem(0).isVisible = form.isValid
                    menu.getItem(0).isEnabled = form.isValid
                }
            }
            
            if (form.nameError != null) {
                if (binding.nameText.editText!!.text.isNotBlank()) {
                    binding.nameText.showError(getString(form.nameError))
                }
            }
            
            if (form.usernameError != null) {
                if (binding.usernameText.editText!!.text.isNotBlank()) {
                    binding.usernameText.showError(getString(form.usernameError))
                }
            }
            
            if (form.tagError != null) {
                if (binding.tagText.editText!!.text.isNotBlank()) {
                    binding.tagText.showError(getString(form.tagError))
                }
            }
            
            if (form.aboutError != null) {
                if (binding.aboutText.editText!!.text.isNotBlank()) {
                    binding.aboutText.showError(getString(form.aboutError))
                }
            }
        }

    }

    private fun setProfileImage(image: Uri? = null) {
        if (image != null) {
            setProfileImage(image.toString())
        } else {
            val s: String? = null
            setProfileImage(s)
        }
    }

    private fun setProfileImage(image: String? = null) {
       /* if (image != null) {
            viewModel.setUserEditFormProfilePhoto(image)
        } else {
            viewModel.setUserEditFormProfilePhoto("")
        }*/
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

    private fun addInterest(interest: String) {
        interest.trim()
        val chip = View.inflate(requireContext(), R.layout.default_chip, null) as Chip

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

    override fun onCreateBinding(inflater: LayoutInflater): FragmentEditProfileBinding {
        return FragmentEditProfileBinding.inflate(inflater)
    }


}