package com.jamid.codesquare.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentInputSheetBinding
import com.jamid.codesquare.listeners.MessageDialogInterface


class InputSheetFragment: BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.AppTheme_BottomSheetInput

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    private lateinit var binding: FragmentInputSheetBinding
    private var title: String = "Collab"
    private var message: String? = null
    private var positiveLabel: String = "Yes"
    private var negativeLabel: String = "No"
    private var hint: String = "Write something here ..."
    private var onSubmitListener: MessageDialogInterface.OnInputSubmitListener? = null
    private var onNegativeClickListener: MessageDialogInterface.OnClickListener? = null
    private var isScrimVisible: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputSheetBinding.inflate(inflater)
        return binding.root
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
            onSubmitListener?.onSubmit(this, it, binding.inputText.editText?.text?.toString() ?: "")
            dismiss()
        }

        binding.negativeBtn.setOnClickListener {
            onNegativeClickListener?.onClick(this, it)
            dismiss()
        }


        if (!isScrimVisible) {
            val window = dialog?.window
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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

            fun setScrimVisibility(isVisible: Boolean): Builder {
                fragment.isScrimVisible = isVisible
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

}