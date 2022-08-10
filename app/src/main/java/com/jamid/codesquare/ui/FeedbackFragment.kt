package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Feedback
import com.jamid.codesquare.databinding.FragmentFeedbackBinding
import com.jamid.codesquare.showKeyboard
import com.jamid.codesquare.toast
// something simple
class FeedbackFragment: BaseFragment<FragmentFeedbackBinding>() {

    private val feedback = Feedback()

    override fun onCreateBinding(inflater: LayoutInflater): FragmentFeedbackBinding {
        return FragmentFeedbackBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.feedbackBtn.setOnClickListener {
            if (binding.feedbackText.editText?.text?.isBlank() == true) {
                return@setOnClickListener
            } else {
                feedback.content = binding.feedbackText.editText?.text.toString()
                feedback.senderId = UserManager.currentUserId

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

        runDelayed(300) {
            binding.feedbackText.editText?.requestFocus()
            showKeyboard()
        }

    }

    companion object {
        private const val TAG = "FeedbackFrag"
    }

}