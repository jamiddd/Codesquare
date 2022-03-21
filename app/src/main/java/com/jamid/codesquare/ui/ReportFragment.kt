package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SmallImagesAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentReportBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding

@ExperimentalPagingApi
class ReportFragment: Fragment() {

    private lateinit var binding: FragmentReportBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val reportViewModel: ReportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val report = arguments?.getParcelable<Report>("report") ?: return
        binding.contextImg.setImageURI(report.image)
        binding.contextName.text = report.title


        reportViewModel.setReportContext(report.contextId)

//        val contextObject = arguments?.get("contextObject") ?: return

        /*when (contextObject) {
            is Project -> {
                reportViewModel.setReportContext(contextObject.id)
                setContextUi(contextObject.images.first(), contextObject.name)
            }
            is User -> {
                reportViewModel.setReportContext(contextObject.id)
                setContextUi(contextObject.photo, contextObject.name)
            }
            is Comment -> {
                reportViewModel.setReportContext(contextObject.commentId)
                FireUtility.getProject(contextObject.projectId) {
                    when (it) {
                        is Result.Error -> viewModel.setCurrentError(it.exception)
                        is Result.Success -> {
                            val project = it.data
                            val commentByText = "Comment by " + project.creator.name
                            setContextUi(contextObject.sender.photo, commentByText)
                        }
                        null -> Log.w(TAG, "Something went wrong while trying to fetch project with id: ${contextObject.projectId}")
                    }
                }
            }
        }*/

        viewModel.reportUploadImages.observe(viewLifecycleOwner) { reportImages ->
            if (!reportImages.isNullOrEmpty()) {
                reportViewModel.addImagesToReport(reportImages.map { it.toString() })
            }
        }

        binding.reportBtn.setOnClickListener {

            val reason = binding.reportReasonText.editText?.text.toString()
            reportViewModel.setReportContent(reason)

            val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
            val loadingBinding = LoadingLayoutBinding.bind(loadingLayout)
            loadingBinding.loadingText.text = "Sending report. Please wait for a while ... "

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(loadingBinding.root)
                .setCancelable(false)
                .show()

            reportViewModel.sendReportToFirebase {

                dialog.dismiss()

                if (it.isSuccessful) {
                    requireActivity().runOnUiThread {
                        val mainRoot = requireActivity().findViewById<CoordinatorLayout>(R.id.main_container_root)
                        Snackbar.make(mainRoot, "Report sent successfully. We will look into the matter asap!", Snackbar.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                } else {
                    binding.reportReasonText.isErrorEnabled = true
                    binding.reportReasonText.error = it.exception?.localizedMessage.orEmpty()
                }

            }
        }

        binding.reportReasonText.editText?.doAfterTextChanged {
            binding.reportBtn.isEnabled = !it.isNullOrBlank()
            binding.reportReasonText.error = null
        }

        init()

    }

    private fun init() {

        binding.reportBtn.isEnabled = false

        val smallImagesAdapter = SmallImagesAdapter(requireActivity() as MainActivity)
        binding.reportImagesRecycler.apply {
            adapter = smallImagesAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        reportViewModel.currentReport.observe(viewLifecycleOwner) { currentReport ->
            if (currentReport != null) {
                if (currentReport.snapshots.isEmpty()) {
                    binding.reportImagesRecycler.hide()
                } else {
                    binding.reportImagesRecycler.show()
                    smallImagesAdapter.submitList(currentReport.snapshots)
                }

                setSecondaryButton(currentReport)
            }
        }
    }

    private fun setSecondaryButton(currentReport: Report) {
        if (currentReport.snapshots.isNotEmpty()) {
            binding.reportAddScreenshots.text = getString(R.string.clear_images)
            binding.reportAddScreenshots.setOnClickListener {
                reportViewModel.clearAllImagesFromReport()
            }
        } else {
            binding.reportAddScreenshots.text = getString(R.string.add_images)
            binding.reportAddScreenshots.setOnClickListener {
                (activity as MainActivity).selectImage(ImageSelectType.IMAGE_REPORT)
            }
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