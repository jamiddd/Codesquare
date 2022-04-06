package com.jamid.codesquare.listeners

import android.view.View
import androidx.fragment.app.DialogFragment
import com.jamid.codesquare.ui.MessageDialogFragment

interface MessageDialogInterface {
    interface OnClickListener {
        fun onClick(d: DialogFragment, v: View)
    }

    interface OnInputSubmitListener {
        fun onSubmit(d: DialogFragment, v: View, s: String)
    }
}