@file:Suppress("UNCHECKED_CAST")

package com.jamid.codesquare.ui.home.chat

//@ExperimentalPagingApi
class ChatFragment/*: DefaultPagingFragment<Message, MessageViewHolder2<Message>>(), MessageListener3 */{

   /* private lateinit var chatChannelId: String
    private lateinit var chatChannel: ChatChannel
    private val mContext: Context by lazy { requireContext() }
//    private var fab: FloatingActionButton? = null
    private lateinit var post: Post


    private var tooltipView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition(700, TimeUnit.MILLISECONDS)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        setHasOptionsMenu(true)
    }

    private fun setChannelIcon(menu: Menu, chatChannelId: String, imageString: String) {
        val channelSavedBitmap = viewModel.chatChannelsBitmapMap[chatChannelId]
        if (channelSavedBitmap != null) {
            setBitmapDrawable(menu, channelSavedBitmap)
        } else {
            downloadBitmapUsingFresco(activity, imageString) { image ->
                activity.runOnUiThread {
                    if (image != null) {
                        viewModel.chatChannelsBitmapMap[chatChannelId] = image
                        setBitmapDrawable(menu, image)
                    }
                }
            }
        }
    }

    private fun setBitmapDrawable(menu: Menu, bitmap: Bitmap) {
        val scaledBitmap = if (bitmap.width >= bitmap.height){
            Bitmap.createBitmap(
                bitmap,
                bitmap.width /2 - bitmap.height /2,
                0,
                bitmap.height,
                bitmap.height
            )

        }else{
            Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height /2 - bitmap.width /2,
                bitmap.width,
                bitmap.width
            )
        }
        val length = resources.getDimension(R.dimen.unit_len) * 6
        val drawable = RoundedBitmapDrawableFactory.create(resources, scaledBitmap).also {
            it.cornerRadius = length
        }
        menu.getItem(0).icon = drawable
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if ((parentFragment as ChatContainerFragment).getCurrentFragmentTag() == TAG){
            Log.d(TAG, "onCreateOptionsMenu: Setting menu chat fragment")
            activity.binding.mainToolbar.menu.clear()
            inflater.inflate(R.menu.chat_fragment_menu, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if ((parentFragment as ChatContainerFragment).getCurrentFragmentTag() == TAG) {
            setChannelIcon(menu, chatChannelId, chatChannel.postImage)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_icon -> {
                (parentFragment as ChatContainerFragment).navigate(ChatDetailFragment.TAG, bundleOf(
                    CHAT_CHANNEL to chatChannel, POST to post))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getScrollPosition(): Int {
        val layoutManager = binding.pagerItemsRecycler.layoutManager as LinearLayoutManager?
        return layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
    }

    private fun setNewMessagesListener() {
        viewModel.getReactiveChatChannel(chatChannelId).observe(viewLifecycleOwner) {
            (parentFragment as ChatContainerFragment).isInProgressMode = false
            scrollToBottom()
        }
    }

    private fun setStaticLayout() {
        // set the pager recycler for extra configurations
        setMessagesRecycler()

        // other items
        binding.pagerNoItemsText.text = getString(R.string.empty_messages_greet)
        binding.noDataImage.setAnimation(R.raw.messages)

        // disabling animation loop
        binding.noDataImage.repeatCount = 0

        // since it's chat we do not need refresher
        binding.pagerRefresher.isEnabled = false


        // listening for new messages
        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)

                if (itemCount != 0) {
                    // hide recyclerview and show info
                    binding.pagerItemsRecycler.show()
                    binding.pagerNoItemsText.hide()
                } else {

                    // hide info and show recyclerview
                    binding.pagerItemsRecycler.hide()
                    binding.pagerNoItemsText.show()
                }
            }
        })

    }

    private fun setMessagesRecycler() {

        val largePadding = resources.getDimension(R.dimen.large_padding).toInt()
        val smallestPadding = resources.getDimension(R.dimen.smallest_padding).toInt()

        binding.pagerItemsRecycler.apply {
            layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, true)
            itemAnimator = null
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
            setPadding(0, smallestPadding, 0, largePadding)

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val scrollPosition = getScrollPosition()

                    val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
                    if (scrollPosition > 15 && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        showToolbarClickTooltip(toolbar)
                    }

                }
            })
        }

    }

    private fun showToolbarClickTooltip(toolbar: View) {

        val container = activity.binding.root
        container.removeView(tooltipView)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val scrollToTopDialogFlag = sharedPref.getBoolean(PREF_SCROLL_TOP_ALT, true)

        if (scrollToTopDialogFlag) {
            tooltipView = showTooltip("Click on toolbar to scroll to bottom", container, toolbar, AnchorSide.Bottom)

            val editor = sharedPref.edit()
            editor.putBoolean(PREF_SCROLL_TOP_ALT, false)
            editor.apply()
        }

    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        chatChannelId = chatChannel.chatChannelId

        shouldShowProgress = false

        setStaticLayout()

        val query = Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannelId)
            .collection(MESSAGES)

        getItems {
            viewModel.getPagedMessages(
                chatChannelId,
                query
            )
        }

        activity.binding.mainToolbar.setOnClickListener {
            scrollToBottom()
        }

        setNewMessagesListener()

        activity.getPostImpulsive(chatChannel.postId) {
            post = it
        }

        startPostponedEnterTransition()

    }

    *//*private fun updateFabUi(scrollPosition: Int) {
        if (scrollPosition != 0) {
            // if the scroll position is not the bottom most check if it is way above or not
            if (scrollPosition > MESSAGE_SCROLL_THRESHOLD) {
                // if the position is way above, show the fab
                fab?.show()
            } else {
                // if the position is slightly above, don't show the fab
                fab?.hide()
            }

            // update current position @Deprecated
            viewModel.chatScrollPositions[chatChannelId] = scrollPosition
        } else {
            fab?.hide()
            // if the scroll position is exactly bottom, check if there are actually any messages
            *//**//*if (pagingAdapter.itemCount != 0) {
                fab?.hide()
            }*//**//*
        }
    }*//*

   *//* private fun setFabLayout() {
        fab = FloatingActionButton(requireContext())
        fab?.apply {
            size = SIZE_MINI
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_keyboard_double_arrow_down_24))
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        }

        binding.pagerRoot.addView(fab)

        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        val extraLargeMargin = resources.getDimension(R.dimen.extra_large_margin).toInt()

        fab?.let {
            it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = binding.pagerRoot.id
                endToEnd = binding.pagerRoot.id
                bottomToBottom = binding.pagerRoot.id
                horizontalBias = 1.0f
                setMargins(smallMargin, 0, smallMargin, extraLargeMargin)
            }

            it.setOnClickListener {
                fab?.hide()
                scrollToBottom()
            }

            it.hide()
        }

    }*//*

    private fun scrollToBottom() = viewLifecycleOwner.lifecycleScope.launch {
        delay(300)
        binding.pagerItemsRecycler.smoothScrollToPosition(0)
    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder2<Message>> {
        return MessageAdapter3(this)
    }

    companion object {

        const val TAG = "ChatFragment"

        fun newInstance(bundle: Bundle) =
            ChatFragment().apply {
                arguments = bundle
            }

    }*/

}

