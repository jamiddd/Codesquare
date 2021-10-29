package com.jamid.codesquare.ui.home.chat

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GuidelinesAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentChannelGuidelinesBinding
import com.jamid.codesquare.ui.SuperBottomSheetFragment

class ChannelGuidelinesFragment: BottomSheetDialogFragment() {

    private lateinit var binding: FragmentChannelGuidelinesBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val currentGuidelines = MutableLiveData<List<String>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelGuidelinesBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)

        dialog.setOnShowListener {
            val bottomSheet =
                (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)

            behavior.isDraggable = false
        }

        return dialog
    }

    private fun getNewAdapter(): GuidelinesAdapter {
        return GuidelinesAdapter { v, p ->
            val existingList = currentGuidelines.value.orEmpty().toMutableList()
            existingList.removeAt(p)
            currentGuidelines.postValue(existingList)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rulesToolbar.setNavigationOnClickListener {
            dismiss()
        }

        val project = arguments?.getParcelable<Project>("project") ?: return

        currentGuidelines.postValue(project.rules)

        val rulesAdapter = getNewAdapter()

        binding.rulesRecycler.apply {
            adapter = rulesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        currentGuidelines.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.rulesRecycler.show()
                val newAdapter = getNewAdapter()
                binding.rulesRecycler.adapter = newAdapter
                newAdapter.submitList(it)
            } else {
                binding.rulesRecycler.hide()
            }
        }



        binding.addRuleBtn.setOnClickListener {
            val newRuleText = binding.newRuleText.text
            if (newRuleText != null) {
                val newRule = newRuleText.toString()

                val newList = currentGuidelines.value.orEmpty().toMutableList()
                newList.add(newRule)

                currentGuidelines.postValue(newList)
                binding.newRuleText.text.clear()
            }
        }

        binding.rulesToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.update_guidelines -> {

                    val changes = mapOf(
                        "rules" to currentGuidelines.value.orEmpty()
                    )

                    viewModel.updateProject(project.id, changes) {
                        if (it.isSuccessful) {
                            project.rules = currentGuidelines.value.orEmpty()
                            viewModel.updateLocalProject(project)
                            toast("Project guidelines updated")
                            dismiss()
                        } else {
                            toast("Something went wrong!")
                        }
                    }
                }
            }
            true
        }

    }

    companion object {
        fun newInstance(project: Project) = ChannelGuidelinesFragment().apply {
            arguments = Bundle().apply {
                putParcelable("project", project)
            }
        }
    }

}