package com.jamid.codesquare.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ImageUploadException
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.UserUpdate
import com.jamid.codesquare.databinding.FragmentProfileImageBinding
import com.jamid.codesquare.ui.DefaultProfileImageSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// something simple
class ProfileImageFragment : BaseFragment<FragmentProfileImageBinding>() {

    private var profileImage: String? = null

    override fun onCreateBinding(inflater: LayoutInflater): FragmentProfileImageBinding {
        return FragmentProfileImageBinding.inflate(inflater)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setCurrentImage(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.updateImageBtn.setOnClickListener {
            val fragment = DefaultProfileImageSheet()
            fragment.show(activity.supportFragmentManager, DefaultProfileImageSheet.TAG)
        }

        // skipping entirely
        binding.skipImageUpdateBtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileImageFragment_to_navigationHome)
        }

        val currentUser = UserManager.currentUser

        // setting ui for current fragment
        binding.userName.text = currentUser.name

        binding.imageUpdateNextBtn.setOnClickListener {
            // updating UI on next btn click
            binding.profileImageCompleteProgress.show()
            binding.imageUpdateNextBtn.disappear()
            binding.skipImageUpdateBtn.disable()
            binding.updateImageBtn.disable()

            val userUpdate = if (profileImage != null) {
                val image = profileImage!!.toUri()
                if (image.scheme == "content") {
                    UserUpdate(
                        currentUser.username,
                        currentUser.name,
                        profileImage!!.toUri(),
                        false,
                        currentUser.tag,
                        currentUser.about,
                        currentUser.interests
                    )
                } else {
                    UserUpdate(
                        currentUser.username,
                        currentUser.name,
                        profileImage!!.toUri(),
                        true,
                        currentUser.tag,
                        currentUser.about,
                        currentUser.interests
                    )
                }
            } else {
                UserUpdate(
                    currentUser.username,
                    currentUser.name,
                    null,
                    false,
                    currentUser.tag,
                    currentUser.about,
                    currentUser.interests
                )
            }

            runOnBackgroundThread {
                when (val result = withContext(Dispatchers.IO) { FireUtility.updateUser3(userUpdate) }) {
                    is Result.Error -> {
                        /* change this to a separate function*/
                        binding.profileImageCompleteProgress.hide()
                        binding.imageUpdateNextBtn.show()
                        binding.skipImageUpdateBtn.enable()
                        binding.updateImageBtn.enable()

                        when (result.exception){
                            is ImageUploadException -> {
                                toast("Something went wrong while trying to upload profile picture.")
                            }
                        }
                    }
                    is Result.Success -> {
                        findNavController().navigate(
                            R.id.userInfoFragment,
                        )
                    }
                }
            }
        }

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            setProfileImage(image)
        }
    }

    private fun setProfileImage(image: Uri?) {
        if (image != null) {
            setProfileImage(image.toString())
        } else {
            val s: String? = null
            setProfileImage(s)
        }
    }
    private fun setProfileImage(image: String?) {
        profileImage = image
        binding.userImage.setImageURI(image)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

}