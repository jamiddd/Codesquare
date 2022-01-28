package com.jamid.codesquare.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentProfileImageBinding
import com.jamid.codesquare.ui.DefaultProfileImageSheet

@ExperimentalPagingApi
class ProfileImageFragment : Fragment() {

    private lateinit var binding: FragmentProfileImageBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var profileImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // just a precaution, need to remove later, because the fragment responsible
        // for getting current image is also responsible for clearing it out.
        viewModel.setCurrentImage(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileImageBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setting listeners
        binding.userImage.setOnClickListener(onImageUpdateClick)
        binding.updateImageBtn.setOnClickListener(onImageUpdateClick)

        // skipping entirely
        binding.skipImageUpdateBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_profileImageFragment_to_homeFragment,
                null,
                slideRightNavOptions()
            )
        }

        val currentUser = UserManager.currentUser

        binding.imageUpdateNextBtn.setOnClickListener {

            // updating UI on next btn click
            binding.profileImageCompleteProgress.show()
            binding.imageUpdateNextBtn.disappear()
            binding.skipImageUpdateBtn.disable()

            // updating user with latest profile image
            if (profileImage == null) {
                profileImage = currentUser.photo
            }

            FireUtility.updateUser2(mapOf("photo" to profileImage)) {

                binding.profileImageCompleteProgress.hide()

                if (it.isSuccessful) {
                    // navigate to next fragment
                    findNavController().navigate(
                        R.id.action_profileImageFragment_to_userInfoFragment,
                        null,
                        slideRightNavOptions()
                    )
                } else {
                    // updating UI on failed update
                    binding.skipImageUpdateBtn.enable()
                    binding.imageUpdateNextBtn.show()
                    viewModel.setCurrentError(it.exception)
                }
            }
        }

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            // when new image or empty image is being fetched
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


        // setting ui for current fragment
        binding.userName.text = currentUser.name


    }

    private fun setProfileImage(image: Uri?) {
        setProfileImage(image.toString())
    }

    private fun setProfileImage(image: String?) {
        profileImage = image
        binding.userImage.setImageURI(profileImage)
    }

    private fun onImageUploaded() {
        binding.userImgProgressBar.hide()
        binding.imageUpdateNextBtn.enable()
        binding.skipImageUpdateBtn.enable()
        binding.userImage.colorFilter = null
    }

    private fun onImageUploadFailed(e: Exception) {
        removeImage()
        toast(e.message.toString())
    }

    private fun removeImage() {
        profileImage = null
        setProfileImage(profileImage)
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

    private fun onNewImageOrNullSet(image: Uri? = null) {
        val currentUser = UserManager.currentUser

        val colorFilter =  ContextCompat.getColor(
            requireContext(),
            R.color.darkest_transparent
        )

        // show that there is some progress
        binding.userImgProgressBar.show()
        binding.userImage.setColorFilter(colorFilter)

        // disable actions because a work is in progress
        binding.imageUpdateNextBtn.disable()
        binding.skipImageUpdateBtn.disable()

        // if there was no image
        if (image == null) {
            // update UI
            binding.userImage.colorFilter = null
            binding.userImgProgressBar.hide()
            binding.imageUpdateNextBtn.enable()
            binding.skipImageUpdateBtn.enable()

            // setting the already existing image as profile image
            setProfileImage(currentUser.photo)
        }

    }

    private val onImageUpdateClick = View.OnClickListener {
        val fragment = DefaultProfileImageSheet()
        fragment.show(requireActivity().supportFragmentManager, "DefaultProfileImage")
    }

    // resetting variable
    override fun onDestroy() {
        super.onDestroy()
        viewModel.setCurrentImage(null)
    }

    // resetting variable
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

}