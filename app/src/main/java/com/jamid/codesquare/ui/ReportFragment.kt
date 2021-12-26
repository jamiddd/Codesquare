package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.Report
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentReportBinding
import com.jamid.codesquare.toast
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalPagingApi
class ReportFragment: Fragment() {

    private lateinit var binding: FragmentReportBinding
    private val viewModel: MainViewModel by activityViewModels()

    private val report = Report()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contextObject = arguments?.get("contextObject") ?: return
        val now = SimpleDateFormat("hh:mm a, EEEE", Locale.UK).format(System.currentTimeMillis())
        binding.reportTimeText.text = now

        when (contextObject) {
            is Project -> {
                report.contextId = contextObject.id
                val firstImage = contextObject.images.firstOrNull()
                binding.contextImg.setImageURI(firstImage)
                binding.contextName.text = contextObject.name
            }
            is User -> {
                report.contextId = contextObject.id
                val image = contextObject.photo
                binding.contextImg.setImageURI(image)
                binding.contextName.text = contextObject.name
            }
            is Comment -> {

                report.contextId = contextObject.commentId

                Firebase.firestore.collection("projects").document(contextObject.projectId)
                    .get()
                    .addOnCompleteListener {
                        if (it.isSuccessful && it.result.exists()) {
                            val project = it.result.toObject(Project::class.java)!!
                            val firstImage = project.images.firstOrNull()
                            binding.contextImg.setImageURI(firstImage)
                            val commentByText = "Comment by " + project.creator.name
                            binding.contextName.text = commentByText

                        } else {
                            Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                        }
                    }
            }
        }

        viewModel.reportUploadImages.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.uploadImages("${report.id}/images", it) { it1 ->
                    report.snapshots = it1.map {it2 -> it2.toString() }
                }
            }
        }

        binding.reportBtn.setOnClickListener {
            if (binding.reportReasonText.editText?.text?.isBlank() == true) {
                return@setOnClickListener
            } else {
                val reason = binding.reportReasonText.editText?.text.toString()
                report.reason = reason
                report.senderId = UserManager.currentUserId

                viewModel.sendReport(report) {
                    if (it.isSuccessful) {
                        toast("Report sent successfully. We will look into the matter asap!")
                        findNavController().navigateUp()
                    } else {
                        Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                    }
                }

            }
        }

        binding.reportAddScreenshots.setOnClickListener {
            (activity as MainActivity).selectReportUploadImages()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setReportUploadImages(emptyList())
    }

    companion object {
        private const val TAG = "ReportFragment"
    }
}