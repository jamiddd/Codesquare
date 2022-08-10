package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.jamid.codesquare.BaseBottomFragment
import com.jamid.codesquare.databinding.FragmentMessageDialogBinding
import com.jamid.codesquare.listeners.MessageDialogInterface
// something simple
class MessageDialogFragment: BaseBottomFragment<FragmentMessageDialogBinding>() {

    private var title: String = "Collabme"
    private var message: String? = null
    private var positiveLabel: String = "Yes"
    private var negativeLabel: String = "No"
    private var onPositiveClickListener: MessageDialogInterface.OnClickListener? = null
    private var onNegativeClickListener: MessageDialogInterface.OnClickListener? = null
    private var onInflateListener: MessageDialogInterface.OnInflateListener? = null
    private var shouldShowProgress: Boolean = false
    private var layoutId: Int = -1

    init {
        // be default let all message dialog be non cancellable if there is progress
        if (shouldShowProgress) {
            cancellable = false
        }
        fullscreen = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        if (layoutId != -1) {
            binding.dialogExtraContainer.layoutResource = layoutId
            val v = binding.dialogExtraContainer.inflate()
            onInflateListener?.onInflate(this, v)
        }

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

            fun setCustomView(@LayoutRes id: Int, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.layoutId = id
                messageDialogFragment.onInflateListener = object: MessageDialogInterface.OnInflateListener {
                    override fun onInflate(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun setProgress(): Builder {
                messageDialogFragment.shouldShowProgress = true
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

    override fun onCreateBinding(inflater: LayoutInflater): FragmentMessageDialogBinding {
        return FragmentMessageDialogBinding.inflate(inflater)
    }

}