package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ToolbarAdjustment
import com.jamid.codesquare.databinding.ProjectFragmentContainerBinding

@ExperimentalPagingApi
class ProjectFragmentContainer: Fragment() {

    private lateinit var binding: ProjectFragmentContainerBinding
    private lateinit var project: Project
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var toolbar: MaterialToolbar
    private var isPrimaryFragment = true
    private var flagForSnackbar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        project = arguments?.getParcelable(PROJECT) ?: return
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (project.isMadeByMe) {
            inflater.inflate(R.menu.project_frag_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.project_menu_option -> {
                viewModel.setCurrentFocusedProject(project)

                val option = if (project.isArchived) {
                    OPTION_13
                } else {
                    OPTION_12
                }

                val options = arrayListOf(OPTION_15, option)
                val icons = arrayListOf(R.drawable.ic_edit, R.drawable.ic_round_archive_24)

                (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(options = options, icons = icons)
                (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ProjectFragmentContainerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        project = arguments?.getParcelable(PROJECT) ?: return
        toolbar = requireActivity().findViewById(R.id.main_toolbar)

        if (toolbar.title.isBlank()) {
            toolbar.title = project.name
        }

        val initialImagePos = arguments?.getInt("image_pos") ?: 0

        if (childFragmentManager.backStackEntryCount == 0) {
            val frag = ProjectFragment.newInstance(bundleOf(PROJECT to project, TITLE to project.name, "image_pos" to initialImagePos))
            childFragmentManager.beginTransaction()
                .add(binding.projectFragContainer.id, frag, ProjectFragment.TAG)
                .addToBackStack(ProjectFragment.TAG)
                .commit()
        }

        viewModel.getReactiveProject(project.id).observe(viewLifecycleOwner) { updatedProject ->
            if (updatedProject != null) {
                project = updatedProject

                setJoinBtn()
            }
        }

        childFragmentManager.addOnBackStackChangedListener {

            isPrimaryFragment = childFragmentManager.backStackEntryCount == 1
            setJoinBtn()


            val topFragment = childFragmentManager.fragments.lastOrNull()
            if (topFragment != null) {
                setToolbarForFragment(topFragment)
            }

            /*if (topFragment != null) {
                when (topFragment) {
                    is ProjectContributorsFragment -> {
                        toolbar.title = "Contributors"
                        toolbar.menu.clear()
                    }
                    is CommentsFragment -> {
                        toolbar.title = "Comments"
                        toolbar.menu.clear()
                    }
                    is ProjectFragment -> {
                        toolbar.title = project.name
                        if (!toolbar.menu.hasVisibleItems() && project.isMadeByMe) {
                            toolbar.inflateMenu(R.menu.project_menu)
                        }
                    }
                }
            }*/

            updateNavigation()

        }

    }

    override fun onResume() {
        super.onResume()
        updateNavigation()
    }

    private fun updateNavigation() {
        if (isPrimaryFragment) {
            // We care about select mode only if the current child fragment is chat fragment
            setNavigation {
                findNavController().navigateUp()
            }
        } else {
            setNavigation {
                childFragmentManager.popBackStack()
            }
        }
    }

    private fun setNavigation(onNavigation: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onNavigation()
        }

        toolbar.setNavigationOnClickListener {
            onNavigation()
        }
    }


    /**
     * To send a request to join in the current project
     *
     * Check :
     * 1. if the project is made by the current user, in which case current user can send request to himself
     * 2. if the project is already a collaboration of the current user in which case no need to show
     * 3. if the project is already requested, in which case show the button but set it to undo functionality
     * 4. and lastly idle state, which is normal state
     *
     * */
    private fun setJoinBtn() {
        val activity = requireActivity() as MainActivity
        binding.projectJoinBtn.apply {
            when {
                !isPrimaryFragment -> hide()
                project.isMadeByMe -> hide()
                project.isCollaboration -> hide()
                project.isRequested -> {
                    if (flagForSnackbar) {
                        Snackbar.make(binding.root, "Project request sent", Snackbar.LENGTH_LONG).show()
                    }

                    show()
                    text = getString(R.string.undo_request)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_undo_20)

                    viewModel.getRequestByProject(project) { projectRequest ->
                        if (projectRequest != null) {
                            setOnClickListener {
                                activity.onProjectUndoClick(project, projectRequest) {
                                    project = it
                                    setJoinBtn()
                                }
                            }
                        }
                    }
                }
                else -> {
                    show()
                    text = getString(R.string.join_project)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)

                    setOnClickListener {
                        flagForSnackbar = true
                        activity.onProjectJoinClick(project) {
                            project = it
                            setJoinBtn()
                        }
                    }
                }
            }
        }
    }


    /**
     * This function assumes that a title is provided in the fragment arguments for all child fragments
     * for a unified design structure
     * */
    private fun setToolbarForFragment(fragment: Fragment) {

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)!!
        val args = fragment.arguments

        if (args != null) {
            val title = args.getString(TITLE)
            val subtitle = args.getString(SUB_TITLE)

            toolbar.title = title
            toolbar.subtitle = subtitle
        }

       /* when (fragment) {
            is ProjectFragment -> {

            }
            is ProjectContributorsFragment -> {

            }
            is TagFragment -> {

            }
        }*/
    }














    /**
     * This function assumes that title and/or subtitle will be provided in the bundle for any particular
     * child fragment of this current fragment
     * */
    fun navigate(tag: String, bundle: Bundle = bundleOf()) {
        val fragment = getFragmentByTag(tag, bundle)
        hideKeyboard()
        childFragmentManager.beginTransaction()
            .add(binding.projectFragContainer.id, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    companion object {
        private const val TAG = "ProjectFragmentContainer"
    }

}