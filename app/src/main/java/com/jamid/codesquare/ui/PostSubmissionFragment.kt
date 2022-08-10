package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.FragmentPostSubmissionBinding
import com.jamid.codesquare.getBottomSheetBehavior
import com.jamid.codesquare.isNightMode

class PostSubmissionFragment(val post: Post): FullscreenBottomSheetFragment() {

    private lateinit var binding: FragmentPostSubmissionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostSubmissionBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFullHeight()
        val behavior = getBottomSheetBehavior()
        behavior?.skipCollapsed = true

        binding.submittedPostName.text = post.name

        if (isNightMode()){
            binding.lottieAnimationView.setAnimation(R.raw.done_night)
        } else {
            binding.lottieAnimationView.setAnimation(R.raw.done)
        }

        binding.button2.setOnClickListener {
            val f = RankedRulesFragment()
            f.show(requireActivity().supportFragmentManager, "RankedRules")
        }

//        FireUtility.updatePost(post.id, mapOf("rank" to POST_IN_REVIEW, "rankCategory" to post.rankCategory, "updatedAt" to System.currentTimeMillis())) {
//            if (!it.isSuccessful) {
//                Log.e(TAG, "onViewCreated: ${it.exception?.localizedMessage}")
//                TODO("Since the user has already paid the amount, this call" +
//                        " needs to be delegated to future or the user must be refunded")
//            } else {
//                TODO("Notify the user that their post has been sent for review" +
//                        " and will notify them when it is added to ranked comp")
//                TODO("Also the ticket has been spent, thus, user document must be updated: {hasTicket:false}")
//            }
//          TODO("After everything is done, dismiss this sheet")
//        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

    }

    companion object {
        private const val TAG = "PostSubmissionFragment"
    }

}