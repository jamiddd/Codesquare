package com.jamid.codesquare

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.transition.MaterialSharedAxis
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.databinding.PostMediaHelperLayoutBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MediaRecyclerView
import kotlinx.coroutines.*
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import kotlin.math.abs
import kotlin.math.sign

abstract class BaseBottomFragment<T: ViewBinding>: BottomSheetDialogFragment() {

    open val viewModel: MainViewModel by activityViewModels()
    open val activity: MainActivity by lazy { requireActivity() as MainActivity }

    companion object {
        const val TAG = "BaseBottomSheet"
    }

    open lateinit var binding: T

    open var fullscreen = false
    open var draggable = true
    open var scrim = true
    open var cancellable = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = onCreateBinding(inflater)
        return binding.root
    }

    open fun runOnBackgroundThread(block: suspend CoroutineScope.() -> Unit): Job {
        return viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO, block = block)
    }

    open fun runDelayed(duration: Long, block: () -> Unit): Job {
        return runOnBackgroundThread {
            delay(duration)
            if (this@BaseBottomFragment.isVisible) {
                runOnMainThread(block)
            }
        }
    }

    open fun runOnMainThread(block: () -> Unit) {
        activity.runOnUiThread(block)
    }

    abstract fun onCreateBinding(inflater: LayoutInflater): T

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setScrimVisibility(scrim)
        setDraggability(draggable)

        if (!cancellable) {
            getTouchOutsideView()?.setOnClickListener {
                //
            }
        }

        // TODO("when the height is not fullscreen, it is not rounded fix this.")
        if (fullscreen) {
            setHeight(getWindowHeight() - getStatusBarHeight())
        /*    if (roundedExpanded) {
                getBottomSheetBehavior()?.apply {
                    halfExpandedRatio = halfRatio
                    state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
            } else {
                setExpandedAtStart()
            }*/
        } else {
            setHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    }

    private fun setExpandedAtStart() {
        val behavior = getBottomSheetBehavior()
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setScrimVisibility(isVisible: Boolean) {
        if (!isVisible) {
            val dialog = dialog
            if (dialog != null) {
                val window = dialog.window
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                )
                window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun setHeight(h: Int) {
        val frame = getFrameLayout()
        frame?.updateLayoutParams<ViewGroup.LayoutParams> {
            height = h
        }
        val behavior = getBottomSheetBehavior()
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setDraggability(isDraggable: Boolean) {
        val behavior = getBottomSheetBehavior()
        behavior?.isDraggable = isDraggable
    }

}

abstract class BaseFragment<T: ViewBinding>: Fragment() {

    open val viewModel: MainViewModel by activityViewModels()
    open val activity: MainActivity by lazy { requireActivity() as MainActivity }
    open lateinit var binding: T

    val keyboardState = MutableLiveData<Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = onCreateBinding(inflater)
        return binding.root
    }

    fun setMenu(menuId: Int, onItemSelected: (menuItem: MenuItem) -> Boolean, onPrepare: (menu: Menu) -> Unit) {
        val menuHost = activity

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(menuId, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return onItemSelected(menuItem)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                runDelayed(300) {
                    onPrepare(menu)
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    abstract fun onCreateBinding(inflater: LayoutInflater): T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDefaultFragmentAnimations()
    }

    private fun setDefaultFragmentAnimations() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    open fun runOnBackgroundThread(block: suspend CoroutineScope.() -> Unit): Job {
        return viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO, block = block)
    }

    open fun runDelayed(duration: Long, block: () -> Unit): Job {
        return runOnBackgroundThread {
            delay(duration)
            if (this@BaseFragment.isVisible) {
                runOnMainThread(block)
            }
        }
    }

    open fun runOnMainThread(block: () -> Unit) {
        activity.runOnUiThread(block)
    }

    fun SwipeRefreshLayout.setDefaultSwipeRefreshLayoutUi() {
        setColorSchemeColors(requireContext().accentColor())

        if (isNightMode()) {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.darkest_grey_2))
        } else {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        val zero = resources.getDimension(R.dimen.zero).toInt()
        val actionBarOffset = resources.getDimension(R.dimen.action_bar_height).toInt()

        setProgressViewOffset(false, zero, actionBarOffset)
    }

    fun setMediaLayout(
        rv: MediaRecyclerView,
        ma: MediaAdapter,
        postMediaHelperLayoutBinding: PostMediaHelperLayoutBinding,
        observableData: LiveData<List<MediaItem>>,
        isMutable: Boolean = true
    ) {
        val helper: SnapHelper = LinearSnapHelper()

        val lm = LinearLayoutManager(rv.context, LinearLayoutManager.HORIZONTAL, false)

        var totalCount = 0

        fun setCounterText(currentPos: Int) {
            if (totalCount != 0) {
                postMediaHelperLayoutBinding.mediaItemCounter.show()
                val t = "$currentPos/$totalCount"
                postMediaHelperLayoutBinding.mediaItemCounter.text = t
            } else {
                postMediaHelperLayoutBinding.mediaItemCounter.hide()
            }
        }

        // setting up the recycler
        rv.apply {
            mLifecycleOwner = viewLifecycleOwner
            adapter = ma
            onFlingListener = null
            helper.attachToRecyclerView(this)
            layoutManager = lm

            OverScrollDecoratorHelper.setUpOverScroll(rv, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
        }

        val windowWidth = getWindowWidth()

        rv.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // if the scroll distance is greater than half of the screen width
                // select the position of the item which covers more screen in the visible rect
                if (abs(dx + 200) > (windowWidth/2)) {

                    // we need to find the direction of the scroll
                    val sign = sign(dx.toDouble())
                    if (sign == -1.00) {
                        // if dx is -ve, means that the user has swiped from left to right
                        val leftPosition = lm.findFirstVisibleItemPosition()
                        setCounterText(leftPosition + 1)
                    } else {
                        // if dx is +ve, means that the user has swiped from right to left
                        val rightPosition = lm.findLastVisibleItemPosition()
                        setCounterText(rightPosition + 1)
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // while idle, one item will cover the whole screen so it doesn't matter
                        // if we find first or last
                        val pos = lm.findFirstCompletelyVisibleItemPosition()
                        setCounterText(pos + 1)
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {

                    }
                    else -> {
                        // RecyclerView.SCROLL_STATE_SETTLING
                    }
                }
            }
        })

        postMediaHelperLayoutBinding.removeCurrentImgBtn.setOnClickListener {
            val pos = lm.findFirstCompletelyVisibleItemPosition()
            onMediaLayoutItemRemoved(pos)
        }

        postMediaHelperLayoutBinding.clearAllImagesBtn.setOnClickListener {
            onMediaLayoutCleared()
        }

        observableData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                toast(it.size.toString())
                totalCount = it.size
                setCounterText(1)
                ma.submitList(it)
                (rv.parent as View).show()
                onMediaItemsAdded()
            } else {
                (rv.parent as View).hide()
            }
        }

        if (!isMutable) {
            postMediaHelperLayoutBinding.clearAllImagesBtn.hide()
            postMediaHelperLayoutBinding.removeCurrentImgBtn.hide()
        }

    }

    open fun onMediaLayoutItemRemoved(pos: Int) {

    }

    open fun onMediaLayoutCleared() {

    }

    open fun onMediaItemsAdded() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val dy = resources.getDimension(R.dimen.action_height).toInt()
            val diff = getWindowHeight() - dy - (bottom - top)
            keyboardState.postValue(diff > ARB_KEYBOARD_HEIGHT)
        }
    }

    companion object {
        private const val TAG = "BaseBottom2"
        private const val ARB_KEYBOARD_HEIGHT = 200
    }

}


