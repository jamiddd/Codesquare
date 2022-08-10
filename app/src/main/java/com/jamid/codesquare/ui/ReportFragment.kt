package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SmallImagesAdapter
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.Report
import com.jamid.codesquare.databinding.FragmentReportBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.ItemSelectResultListener

class ReportFragment: BaseFragment<FragmentReportBinding>(), ImageClickListener, ItemSelectResultListener<MediaItem> {

    private val reportViewModel: ReportViewModel by viewModels()

    override fun onCreateBinding(inflater: LayoutInflater): FragmentReportBinding {
        return FragmentReportBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val report = arguments?.getParcelable<Report>(REPORT) ?: return

        binding.contextImg.setImageURI(report.image)
        binding.contextName.text = report.title

        reportViewModel.setReportContext(report.contextId)

        /*viewModel.reportUploadImages.observe(viewLifecycleOwner) { reportImages ->
            if (!reportImages.isNullOrEmpty()) {
                reportViewModel.addImagesToReport(reportImages.map { it.toString() })
            }
        }*/

        binding.reportBtn.setOnClickListener {
            val reason = binding.reportReasonText.editText?.text.toString()
            reportViewModel.setReportContent(reason)

            val frag = MessageDialogFragment.builder("Sending report. Please wait for a while ... ")
                .setProgress()
                .build()

            frag.show(childFragmentManager, MessageDialogFragment.TAG)

            reportViewModel.sendReportToFirebase {

                frag.dismiss()

                if (it.isSuccessful) {
                    runOnMainThread {
                        Snackbar.make(activity.binding.root, "Report sent successfully. We will look into the matter asap!", Snackbar.LENGTH_LONG).show()
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

        initialize()

    }

    private fun initialize() {

        binding.reportBtn.isEnabled = false

        val smallImagesAdapter = SmallImagesAdapter(this)
        binding.reportImagesRecycler.apply {
            adapter = smallImagesAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
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
                val frag = GalleryFragment(type = ItemSelectType.GALLERY_ONLY_IMG, itemSelectResultListener = this)
                frag.title = "Select images"
                frag.primaryActionLabel = "Select"
                frag.show(activity.supportFragmentManager, "GalleryFrag")
            }
        }
    }

    override fun onImageClick(view: View, image: Image) {
        activity.showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        //
    }

    override fun onItemsSelected(items: List<MediaItem>, externalSelect: Boolean) {
        if (!items.isNullOrEmpty()) {
            reportViewModel.addImagesToReport(items.map { it.url })
        }
    }
}