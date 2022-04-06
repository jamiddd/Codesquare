package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.codesquare.databinding.FragmentMessageDialogBinding
import com.jamid.codesquare.listeners.MessageDialogInterface

class MessageDialogFragment: RoundedBottomSheetDialogFragment() {

    private lateinit var binding: FragmentMessageDialogBinding
    private var title: String = "Collab"
    private var message: String? = null
    private var positiveLabel: String = "Yes"
    private var negativeLabel: String = "No"
    private var onPositiveClickListener: MessageDialogInterface.OnClickListener? = null
    private var onNegativeClickListener: MessageDialogInterface.OnClickListener? = null
    private var shouldShowProgress: Boolean = false
    private var isDraggable: Boolean = true
    private var isHideable: Boolean = true
    private var isScrimVisible: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageDialogBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isScrimVisible) {
            setScrimVisibility(false)
        }

        val dialog = dialog!!
        val frame = dialog.window!!.decorView.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(frame)
        behavior.isDraggable = isDraggable
        behavior.isHideable = isHideable

        if (!isHideable) {
            val to = dialog.window!!.decorView.findViewById<View>(com.google.android.material.R.id.touch_outside)
            to.setOnClickListener {

            }
        }

        binding.dialogTitleText.text = title
        binding.dialogMessageText.text = message

        binding.dialogPositiveBtn.text = positiveLabel
        binding.dialogNegativeBtn.text = negativeLabel

        binding.dialogPositiveBtn.isVisible = onPositiveClickListener != null
        binding.dialogNegativeBtn.isVisible = onNegativeClickListener != null

        binding.dialogPositiveBtn.setOnClickListener {
            onPositiveClickListener?.onClick(this, it)
            dismiss()
        }
        binding.dialogNegativeBtn.setOnClickListener {
            onNegativeClickListener?.onClick(this, it)
            dismiss()
        }

        binding.dialogProgressBar.isVisible = shouldShowProgress

    }

    companion object {


        const val TAG = "MessageDialogFragment"

        class Builder(msg: String) {

            private val messageDialogFragment = MessageDialogFragment().apply {
                message = msg
            }

            fun setTitle(title: String): Builder {
                messageDialogFragment.title = title
                return this
            }

            fun setPositiveButton(label: String, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.positiveLabel = label
                messageDialogFragment.onPositiveClickListener = object : MessageDialogInterface.OnClickListener {
                    override fun onClick(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun setNegativeButton(label: String, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.negativeLabel = label
                messageDialogFragment.onNegativeClickListener = object : MessageDialogInterface.OnClickListener {
                    override fun onClick(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun shouldShowProgress(a: Boolean): Builder {
                messageDialogFragment.shouldShowProgress = a
                return this
            }

            fun setIsHideable(isHideable: Boolean): Builder {
                messageDialogFragment.isHideable = isHideable
                return this
            }

            fun setIsDraggable(isDraggable: Boolean): Builder {
                messageDialogFragment.isDraggable = isDraggable
                return this
            }

            fun setScrimVisibility(isVisible: Boolean): Builder {
                messageDialogFragment.isScrimVisible = isVisible
                return this
            }

            fun build(): MessageDialogFragment {
                return messageDialogFragment
            }

        }

        fun builder(msg: String): Builder {
            return Builder(msg)
        }

        fun newInstance(msg: String): MessageDialogFragment {
            val s = MessageDialogFragment()
            s.message = msg
            return s
        }

    }

}