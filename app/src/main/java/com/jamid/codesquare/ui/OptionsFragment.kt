package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Option
import com.jamid.codesquare.databinding.FragmentOptionsBinding
import com.jamid.codesquare.listeners.OptionClickListener

class OptionsFragment: RoundedBottomSheetDialogFragment() {

    private lateinit var binding: FragmentOptionsBinding
    private lateinit var optionsAdapter: OptionsAdapter

    var listener: OptionClickListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOptionsBinding.inflate(inflater)
        return binding.root
    }

    fun setOptionsListener(mListener: OptionClickListener?) {
        listener = mListener
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = arguments?.getStringArrayList(ARG_OPTIONS) ?: return
        val icons = arguments?.getIntegerArrayList(ARG_ICONS)
        val title = arguments?.getString(ARG_TITLE)

        binding.optionsTitle.text = title

        if (title == null) {
            binding.optionsTitle.hide()
        } else {
            binding.optionsTitle.show()
        }

        optionsAdapter = if (listener != null) {
            OptionsAdapter(listener!!)
        } else {
            OptionsAdapter(requireActivity() as MainActivity)
        }


        binding.optionsList.apply {
            adapter = optionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val list = mutableListOf<Option>()

        for (i in options.indices) {
            list.add(Option(randomId(), options[i], icons?.get(i)))
        }

        optionsAdapter.submitList(list)

        binding.dismissBtn.setOnClickListener {
            dismiss()
        }

    }

    companion object {

        const val TAG = "OptionsFragment"
        const val ARG_OPTIONS = "options"
        const val ARG_ICONS = "icons"
        const val ARG_TITLE = "title"

        fun newInstance(title: String? = null, options: ArrayList<String>, icons: ArrayList<Int>? = null, listener: OptionClickListener? = null) = OptionsFragment().apply {
            arguments = Bundle().apply {
                putString(TITLE, title)
                putStringArrayList(ARG_OPTIONS, options)
                putIntegerArrayList(ARG_ICONS, icons)
            }
            setOptionsListener(listener)
        }
    }

}