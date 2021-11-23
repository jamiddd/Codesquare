package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.data.Feedback
import com.jamid.codesquare.databinding.FragmentFeedbackBinding
import com.jamid.codesquare.toast

class FeedbackFragment: Fragment() {

    private lateinit var binding: FragmentFeedbackBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val feedback = Feedback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = viewModel.currentUser.value!!

        binding.feedbackBtn.setOnClickListener {
            if (binding.feedbackText.editText?.text?.isBlank() == true) {
                return@setOnClickListener
            } else {
                feedback.content = binding.feedbackText.editText?.text.toString()
                feedback.senderId = currentUser.id

                viewModel.sendFeedback(feedback) {
                    if (it.isSuccessful) {
                        toast("Thank you for the feedback. We will work on it asap.")
                        findNavController().navigateUp()
                    } else {
                        Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                    }
                }

            }
        }

    }

    companion object {
        private const val TAG = "FeedbackFrag"
    }

}