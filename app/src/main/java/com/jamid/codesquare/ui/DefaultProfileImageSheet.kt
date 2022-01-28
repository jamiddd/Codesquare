package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.CircleImageLayoutBinding
import com.jamid.codesquare.databinding.DefaultProfileImageSheetBinding
import com.jamid.codesquare.userImages

@ExperimentalPagingApi
class DefaultProfileImageSheet: BottomSheetDialogFragment() {

    private lateinit var binding: DefaultProfileImageSheetBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DefaultProfileImageSheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val images = userImages

        binding.defaultProfileImgRecycler.apply {
            adapter = DefaultProfileImagesAdapter(images)
            layoutManager = GridLayoutManager(requireContext(), 3)
        }

        binding.removeSelectedImageBtn.setOnClickListener {
            viewModel.setCurrentImage(null)
            dismiss()
        }

        binding.selectFromGalleryBtn.setOnClickListener {
            (activity as MainActivity).selectImage1()
            dismiss()
        }

    }

    private inner class DefaultProfileImagesAdapter(private val images: List<String>): RecyclerView.Adapter<DefaultProfileImagesAdapter.DefaultProfileImageViewHolder>() {

        inner class DefaultProfileImageViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
            fun bind(image: String) {
                val layoutBinding = CircleImageLayoutBinding.bind(view)
                layoutBinding.defaultProfileImg.setImageURI(image)

                view.setOnClickListener {
                    viewModel.setCurrentImage(image.toUri())
                    dismiss()
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DefaultProfileImageViewHolder {
            return DefaultProfileImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.circle_image_layout, parent, false))
        }

        override fun onBindViewHolder(holder: DefaultProfileImageViewHolder, position: Int) {
            holder.bind(images[position])
        }

        override fun getItemCount(): Int {
            return images.size
        }

    }

}