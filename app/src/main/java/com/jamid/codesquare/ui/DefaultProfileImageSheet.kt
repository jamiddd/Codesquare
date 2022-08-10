package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.databinding.CircleImageLayoutBinding
import com.jamid.codesquare.databinding.DefaultProfileImageSheetBinding
import com.jamid.codesquare.listeners.ItemSelectResultListener
// something simple
class DefaultProfileImageSheet : BaseBottomFragment<DefaultProfileImageSheetBinding>(),
    ItemSelectResultListener<MediaItem> {

    override fun onCreateBinding(inflater: LayoutInflater): DefaultProfileImageSheetBinding {
        return DefaultProfileImageSheetBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val images = userImages

        binding.selectFromDefault.setOnClickListener {
            if (binding.defaultProfileImgRecycler.isVisible) {
                binding.defaultProfileImgRecycler.hide()
                binding.selectFromDefault.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_round_keyboard_arrow_down_24,
                    0
                )
            } else {
                binding.defaultProfileImgRecycler.show()
                binding.selectFromDefault.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_round_keyboard_arrow_up_24,
                    0
                )
            }
        }

        binding.defaultProfileImgRecycler.apply {
            adapter = DefaultProfileImagesAdapter(images)
            layoutManager = GridLayoutManager(activity, 3)
        }

        binding.removeSelectedImageBtn.setOnClickListener {
            viewModel.setCurrentImage(null)
            dismiss()
        }

        binding.selectFromGalleryBtn.setOnClickListener {
            val frag = GalleryFragment(false, ItemSelectType.GALLERY_ONLY_IMG, this)
            frag.title = "Select image"
            frag.primaryActionLabel = "Done"
            frag.show(activity.supportFragmentManager, "GalleryFrag")
        }

    }

    private inner class DefaultProfileImagesAdapter(private val images: List<String>) :
        RecyclerView.Adapter<DefaultProfileImagesAdapter.DefaultProfileImageViewHolder>() {

        inner class DefaultProfileImageViewHolder(private val view: View) :
            RecyclerView.ViewHolder(view) {
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
            return DefaultProfileImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.circle_image_layout, parent, false)
            )
        }

        override fun onBindViewHolder(holder: DefaultProfileImageViewHolder, position: Int) {
            holder.bind(images[position])
        }

        override fun getItemCount(): Int {
            return images.size
        }

    }

    override fun onItemsSelected(items: List<MediaItem>, externalSelect: Boolean) {
        if (items.isNotEmpty()) {
            val singleImage = items.first().url
            viewModel.setCurrentImage(singleImage.toUri())
            val options = CropImageOptions().apply {
                fixAspectRatio = true
                aspectRatioX = 1
                aspectRatioY = 1
                cropShape = CropImageView.CropShape.OVAL
                outputRequestHeight = 300
                outputRequestWidth = 300
            }

            val frag = CropFragment2().apply {
                image = singleImage
                this.options = options
            }
            frag.show(activity.supportFragmentManager, CropFragment2.TAG)
        }
        dismiss()
    }

    companion object {
        const val TAG = "DefaultProfileImage"
    }

}