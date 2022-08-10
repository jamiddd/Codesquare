package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.jamid.codesquare.BaseBottomFragment
import com.jamid.codesquare.databinding.FragmentInputSheetBinding
import com.jamid.codesquare.listeners.MessageDialogInterface


class InputSheetFragment: BaseBottomFragment<FragmentInputSheetBinding>() {

    private var title: String = "Collabme"
    private var message: String? = null
    private var positiveLabel: String = "Yes"
    private var negativeLabel: String = "No"
    private var hint: String = "Write something here ..."
    private var onSubmitListener: MessageDialogInterface.OnInputSubmitListener? = null
    private var onNegativeClickListener: MessageDialogInterface.OnClickListener? = null

    init {
        fullscreen = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogHeading.text = title
        binding.dialogMessage.text = message

        binding.positiveBtn.text = positiveLabel
        binding.negativeBtn.text = negativeLabel

        binding.negativeBtn.isVisible = onNegativeClickListener != null

        binding.positiveBtn.isVisible = onSubmitListener != null

        binding.inputText.editText?.hint = hint

        binding.positiveBtn.setOnClickListener {
            // must trim the text so that
            val text = binding.inputText.editText?.text

            // checking whether the text is empty
            if (!text.isNullOrBlank()) {
                // need to check if the url doesn't have spaces at the start or end
                val trimmedString = text.trim().toString()
                onSubmitListener?.onSubmit(this, it, trimmedString)
            }
            dismiss()
        }

        binding.negativeBtn.setOnClickListener {
            onNegativeClickListener?.onClick(this, it)
            dismiss()
        }

        runDelayed(300) {
            binding.inputText.editText?.requestFocus()
        }

    }

    companion object {


        const val TAG = "InputSheetFragment"

        class Builder(msg: String) {

            private val fragment = InputSheetFragment().apply {
                message = msg
            }

            fun setTitle(title: String): Builder {
                fragment.title = title
                return this
            }

            fun setHint(hint: String): Builder {
                fragment.hint = hint
                return this
            }

            fun setMessage(msg: String): Builder {
                fragment.message = msg
                return this
            }

            fun setPositiveButton(label: String, a: (DialogFragment, View, String) -> Unit): Builder {
                fragment.positiveLabel = label
                fragment.onSubmitListener = object : MessageDialogInterface.OnInputSubmitListener {
                    override fun onSubmit(d: DialogFragment, v: View, s: String) {
                        a(d, v, s)
                    }
                }
                return this
            }

            fun setNegativeButton(label: String, a: (DialogFragment, View) -> Unit): Builder {
                fragment.negativeLabel = label
                fragment.onNegativeClickListener = object : MessageDialogInterface.OnClickListener {
                    override fun onClick(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun build(): InputSheetFragment {
                return fragment
            }

        }

        fun builder(msg: String): Builder {
            return Builder(msg)
        }

        fun newInstance(msg: String): InputSheetFragment {
            val s = InputSheetFragment()
            s.message = msg
            return s
        }

    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentInputSheetBinding {
        return FragmentInputSheetBinding.inflate(inflater)
    }

}