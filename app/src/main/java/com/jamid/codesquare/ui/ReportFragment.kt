package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SmallImagesAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentReportBinding
import com.jamid.codesquare.listeners.ImageClickListener

@ExperimentalPagingApi
class ReportFragment: Fragment(), ImageClickListener {

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

        viewModel.reportUploadImages.observe(viewLifecycleOwner) { reportImages ->
            if (!reportImages.isNullOrEmpty()) {
                reportViewModel.addImagesToReport(reportImages.map { it.toString() })
            }
        }

        binding.reportBtn.setOnClickListener {

            val reason = binding.reportReasonText.editText?.text.toString()
            reportViewModel.setReportContent(reason)

            val frag = MessageDialogFragment.builder("Sending report. Please wait for a while ... ")
                .setIsDraggable(false)
                .setIsHideable(false)
                .shouldShowProgress(true)
                .build()

            frag.show(childFragmentManager, MessageDialogFragment.TAG)

            reportViewModel.sendReportToFirebase {

                frag.dismiss()

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

        val smallImagesAdapter = SmallImagesAdapter(this)
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

    override fun onImageClick(view: View, image: Image) {
        (activity as MainActivity).showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        //
    }
}