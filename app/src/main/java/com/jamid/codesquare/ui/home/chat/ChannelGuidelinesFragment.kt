package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GuidelinesAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentChannelGuidelinesBinding

@ExperimentalPagingApi
class ChannelGuidelinesFragment : Fragment() {

    private lateinit var binding: FragmentChannelGuidelinesBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val currentGuidelines = MutableLiveData<List<String>>()
    private lateinit var project: Project

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelGuidelinesBinding.inflate(inflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.update_guidelines_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                        findNavController().navigateUp()
                    } else {
                        toast("Something went wrong!")
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /*   override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
   */
    private fun getNewAdapter(): GuidelinesAdapter {
        return GuidelinesAdapter { _, p ->
            val existingList = currentGuidelines.value.orEmpty().toMutableList()
            existingList.removeAt(p)
            currentGuidelines.postValue(existingList)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        project = arguments?.getParcelable("project") ?: return

        currentGuidelines.postValue(project.rules)

        val rulesAdapter = getNewAdapter()

        binding.rulesRecycler.apply {
            adapter = rulesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
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
            if (!newRuleText.isNullOrBlank()) {
                val newRule = newRuleText.toString()
                val newList = currentGuidelines.value.orEmpty().toMutableList()
                newList.add(newRule)

                currentGuidelines.postValue(newList)
                binding.newRuleText.text.clear()
            }
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