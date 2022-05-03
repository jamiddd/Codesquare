package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
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

        val user = arguments?.getParcelable<User>(ARG_OPTION_USER)
        val project = arguments?.getParcelable<Post>(ARG_OPTION_PROJECT)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_OPTION_CHAT_CHANNEL)
        val comment = arguments?.getParcelable<Comment>(ARG_OPTION_COMMENT)
        val tag = arguments?.getString(ARG_STRING)

        binding.optionsTitle.text = title

        if (title == null) {
            binding.optionsTitle.hide()
        } else {
            binding.optionsTitle.show()
        }

        optionsAdapter = if (listener != null) {
            OptionsAdapter(listener!!, user, project, chatChannel, comment, tag)
        } else {
            OptionsAdapter(requireActivity() as MainActivity, user, project, chatChannel, comment, tag)
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

        const val ARG_OPTION_USER = "ARG_OPTION_USER"
        const val ARG_OPTION_PROJECT = "ARG_OPTION_PROJECT"
        const val ARG_OPTION_CHAT_CHANNEL = "ARG_OPTION_CHAT_CHANNEL"
        const val ARG_OPTION_COMMENT = "ARG_OPTION_COMMENT"
        const val ARG_STRING = "ARG_STRING"

        fun newInstance(title: String? = null, options: ArrayList<String>, icons: ArrayList<Int>? = null, listener: OptionClickListener? = null, user: User? = null, post: Post? = null, chatChannel: ChatChannel? = null, comment: Comment? = null, tag: String? = null) = OptionsFragment().apply {
            arguments = Bundle().apply {
                putString(TITLE, title)
                putStringArrayList(ARG_OPTIONS, options)
                putIntegerArrayList(ARG_ICONS, icons)
                putParcelable(ARG_OPTION_USER, user)
                putParcelable(ARG_OPTION_PROJECT, post)
                putParcelable(ARG_OPTION_CHAT_CHANNEL, chatChannel)
                putParcelable(ARG_OPTION_COMMENT, comment)
                putString(ARG_STRING, tag)
            }
            setOptionsListener(listener)
        }
    }

}