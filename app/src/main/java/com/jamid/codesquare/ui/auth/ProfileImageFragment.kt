package com.jamid.codesquare.ui.auth

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
import com.jamid.codesquare.ui.MainActivity

@ExperimentalPagingApi
class ProfileImageFragment: Fragment() {

    private lateinit var binding: FragmentProfileImageBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var profileImage: String? = null
    private var firstTime: Boolean = true

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

        binding.userImage.setOnClickListener(onImageUpdateClick)
        binding.updateImageBtn.setOnClickListener(onImageUpdateClick)

        val currentUser = UserManager.currentUser

        val users = listOf(R.drawable.user1, R.drawable.user2, R.drawable.user3, R.drawable.user4, R.drawable.user5, R.drawable.user6)

        binding.userImage.setActualImageResource(users.random())

        binding.skipImageUpdateBtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileImageFragment_to_homeFragment, null, slideRightNavOptions())
        }

        binding.imageUpdateNextBtn.setOnClickListener {
            binding.profileImageCompleteProgress.show()
            binding.imageUpdateNextBtn.disappear()
            FireUtility.updateUser2(currentUser, mapOf("photo" to profileImage)) {
                binding.profileImageCompleteProgress.show()
                if (it.isSuccessful) {
                    findNavController().navigate(R.id.action_profileImageFragment_to_userInfoFragment, null, slideRightNavOptions())
                } else {
                    binding.imageUpdateNextBtn.show()
                    viewModel.setCurrentError(it.exception)
                }
            }
        }

        binding.userName.text = currentUser.name

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            binding.userImgProgressBar.show()
            binding.userImage.setColorFilter(ContextCompat.getColor(requireContext(), R.color.darkest_transparent))
            binding.imageUpdateNextBtn.isEnabled = false
            binding.skipImageUpdateBtn.isEnabled = false
            if (image != null) {
                binding.userImage.setImageURI(image.toString())
                viewModel.uploadImage(currentUser.id, image) { downloadUri ->
                    binding.userImgProgressBar.hide()
                    binding.imageUpdateNextBtn.isEnabled = true
                    binding.skipImageUpdateBtn.isEnabled = true
                    binding.userImage.colorFilter = null
                    if (downloadUri != null) {
                        profileImage = downloadUri.toString()
//                        binding.userImage.setImageURI(profileImage)
                    } else {
                        profileImage = null
                        binding.userImage.setImageURI(profileImage)
                        toast("Something went wrong while uploading the profile picture.")
                    }
                }
            } else {
                binding.userImgProgressBar.hide()
                profileImage = null
                binding.userImage.setImageURI(profileImage)

                if (firstTime) {
                    firstTime = false
                    profileImage = currentUser.photo
                    binding.userImage.setImageURI(profileImage)
                }
            }
        }
    }

    private val onImageUpdateClick = View.OnClickListener {
        (activity as MainActivity).selectImage1()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setCurrentImage(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImage(null)
    }

}