package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_RIGHT
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.User
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.ScrollTouchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ProjectViewHolder(val v: View): PostViewHolder(v) {

    private val userImg: SimpleDraweeView = view.findViewById(R.id.project_user_img)
    private val userName: TextView = view.findViewById(R.id.project_user_name)
    private val location: TextView = view.findViewById(R.id.project_location)
    private val optionBtn: Button = view.findViewById(R.id.project_option)
    private val imagesRecycler: RecyclerView = view.findViewById(R.id.project_images_recycler)
    private val title: TextView = view.findViewById(R.id.project_title)
    private val content: TextView = view.findViewById(R.id.project_content)
    private val likeComment: TextView = view.findViewById(R.id.project_like_comment_text)
    private val likeBtn: Button = view.findViewById(R.id.project_like_btn)
    private val commentBtn: Button = view.findViewById(R.id.project_comment_btn)
    private val joinBtn: Button = view.findViewById(R.id.project_join_btn)
    private val saveBtn: Button = view.findViewById(R.id.project_save_btn)
    private val imagesCounter: TextView = view.findViewById(R.id.project_images_counter)
    private val time: TextView = view.findViewById(R.id.project_time)
    private val leftBtn: Button = view.findViewById(R.id.left_btn)
    private val rightBtn: Button = view.findViewById(R.id.right_btn)

    var currentImagePosition = 0
    private var totalImagesCount = 0

    private val projectClickListener = view.context as ProjectClickListener

    fun onSaveProjectClick(project: Project) {
        projectClickListener.onProjectSaveClick(project.copy())
        saveBtn.isSelected = !saveBtn.isSelected
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(project: Project?) {
        if (project != null) {

            val creator = project.creator
            val imagesCount = project.images.size

            totalImagesCount = imagesCount

            val ct = "1/$imagesCount"
            imagesCounter.text = ct

            userImg.setImageURI(creator.photo)

            userName.text = creator.name

            userImg.setOnClickListener {
                projectClickListener.onProjectCreatorClick(project)
            }

            userName.setOnClickListener {
                projectClickListener.onProjectCreatorClick(project)
            }

            if (project.location.address.isNotBlank()) {
                location.text = project.location.address
            } else {
                location.hide()
            }

            title.text = project.name
            content.text = project.content

            content.doOnLayout {
                if (content.lineCount > MAX_LINES) {
                    val lastCharShown = content.layout.getLineVisibleEnd(MAX_LINES - 1)
                    content.maxLines = MAX_LINES
                    val moreString = "Show more"
                    val suffix = "  $moreString"

                    val actionDisplayText: String = project.content.substring(0, lastCharShown - suffix.length - 3) + "..." + suffix
                    val truncatedSpannableString = SpannableString(actionDisplayText)
                    val startIndex = actionDisplayText.indexOf(moreString)

                    val cs1 = object : ClickableSpan() {
                        override fun onClick(p0: View) {
                            content.maxLines = Int.MAX_VALUE
                            content.text = project.content

                            view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                                delay(200)
                                content.setOnClickListener {
                                    projectClickListener.onProjectClick(project.copy())
                                }
                            }
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = view.context.accentColor()
                        }
                    }

                    truncatedSpannableString.setSpan(cs1,
                        startIndex,
                        startIndex + moreString.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    content.movementMethod = LinkMovementMethod.getInstance()

                    content.text = truncatedSpannableString

                    view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                        delay(200)
                        content.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT)

                        content.setOnClickListener {
                            projectClickListener.onProjectClick(project.copy())
                        }

                    }
                }
            }

            val likeCommentText = "${project.likes} Likes • ${project.comments} Comments • ${project.contributors.size} Contributors"
            likeComment.text = likeCommentText

            val imageAdapter = ImageAdapter { _, _ ->
                projectClickListener.onProjectClick(project.copy())
            }
            val helper: SnapHelper = LinearSnapHelper()

            likeComment.setOnClickListener {
                projectClickListener.onProjectCommentClick(project.copy())
            }

            val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

            imagesRecycler.apply {
                adapter = imageAdapter
                layoutManager = manager
                if (onFlingListener == null) {
                    helper.attachToRecyclerView(this)
                }
            }

            val imagePipeline = Fresco.getImagePipeline()

            for (image in project.images) {
                val imageRequest = ImageRequest.fromUri(image)
                imagePipeline.prefetchToDiskCache(imageRequest, view.context)
            }

            imageAdapter.submitList(project.images)

            val scrollInstance = ScrollTouchListener()

            imagesRecycler.addOnItemTouchListener(scrollInstance)

            imagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val pos = manager.findFirstCompletelyVisibleItemPosition()
                    if (pos != -1) {
                        currentImagePosition = pos
                        val counterText = "${pos + 1}/$imagesCount"
                        imagesCounter.text = counterText

                        rightBtn.setOnClickListener {
                            imagesRecycler.smoothScrollToPosition(pos + 1)
                        }

                        leftBtn.setOnClickListener {
                            imagesRecycler.smoothScrollToPosition(pos - 1)
                        }

                        if (imagesCount == 1) {
                            leftBtn.hide()
                            rightBtn.hide()
                            imagesCounter.hide()
                        } else {
                            if (pos == 0) {
                                leftBtn.hide()
                            } else {
                                leftBtn.show()
                            }

                            if (pos == imagesCount - 1) {
                                rightBtn.hide()
                            } else {
                                rightBtn.show()
                            }
                        }
                    }
                }
            })

            time.text = getTextForTime(project.createdAt)

            view.setOnClickListener {
                projectClickListener.onProjectClick(project.copy())
            }

            likeBtn.isSelected = project.isLiked

            likeBtn.setOnClickListener {
                projectClickListener.onProjectLikeClick(project.copy())
                if (project.isLiked) {
                    // dislike
                    project.likes = project.likes - 1
                    project.isLiked = false
                    likeBtn.isSelected = false
                    setLikeDislike(project)
                } else {
                    // like
                    project.likes = project.likes + 1
                    project.isLiked = true
                    likeBtn.isSelected = true
                    setLikeDislike(project)
                }
            }

            saveBtn.isSelected = project.isSaved

            saveBtn.setOnClickListener {
                onSaveProjectClick(project)
            }

            when {
                project.isMadeByMe -> {
                    joinBtn.hide()
                }
                project.isRequested -> {
                    joinBtn.show()
                    joinBtn.text = view.context.getString(R.string.undo)
                }
                project.isCollaboration -> {
                    joinBtn.hide()
                }
                else -> joinBtn.show()
            }

            commentBtn.setOnClickListener {
                projectClickListener.onProjectCommentClick(project)
            }

            optionBtn.setOnClickListener {
                projectClickListener.onProjectOptionClick(this, project)
            }

            setJoinButton(project)

            // check if the project is archived
            Firebase.firestore.collection(PROJECTS).document(project.id)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (it.result.exists()) {
                            // do something here
                        } else {
                            projectClickListener.onProjectNotFound(project)
                        }
                    } else {
                        Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                    }
                }


           /* val currentUser = UserManager.currentUser
            if (currentUser.premiumState.toInt() == -1) {
                setAdView()
            }*/

        }
    }


   /* private fun setAdView() {
        val rand = Random.nextInt(1, 6)
        if (rand == Random.nextInt(1, 6)) {

            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .setAdChoicesPlacement(ADCHOICES_BOTTOM_RIGHT)
                .build()

            val adView = LayoutInflater.from(view.context).inflate(R.layout.custom_post_ad, null, false)
            val nativeAdView = adView as NativeAdView

            val container = view.findViewById<FrameLayout>(R.id.project_ad_container)
            container?.removeAllViews()
            container?.addView(adView)

            val infoIcon = view.findViewById<Button>(R.id.ad_info_icon)
            infoIcon?.setOnClickListener {
                projectClickListener.onAdInfoClick()
            }

            // TODO("Currently set for test ad. Need to change this to dynamically fetch the ad Id")
            val adLoader = AdLoader.Builder(view.context, "ca-app-pub-3940256099942544/1044960115")
                .forNativeAd { nativeAd ->

                    nativeAdView.headlineView = adView.findViewById(R.id.ad_headline)
                    nativeAdView.bodyView = adView.findViewById(R.id.ad_secondary_text)
                    nativeAdView.mediaView = adView.findViewById(R.id.ad_media_view)
                    nativeAdView.callToActionView = adView.findViewById(R.id.ad_primary_action)
                    nativeAdView.iconView = adView.findViewById(R.id.ad_app_icon)
                    nativeAdView.priceView = adView.findViewById(R.id.ad_price_text)
                    nativeAdView.starRatingView = adView.findViewById(R.id.ad_rating)
                    nativeAdView.advertiserView = adView.findViewById(R.id.ad_advertiser)

                    val volumeBtn = adView.findViewById<Button>(R.id.ad_volume_btn)
                    val replayBtn = adView.findViewById<Button>(R.id.ad_replay_btn)

                    (nativeAdView.headlineView as TextView).text = nativeAd.headline
                    nativeAd.mediaContent?.let {
                        nativeAdView.mediaView?.setMediaContent(it)
                    }

                    if (nativeAd.icon != null) {
                        (nativeAdView.iconView as SimpleDraweeView).setImageURI(nativeAd.icon?.uri.toString())
                    }

                    if (nativeAd.body == null) {
                        nativeAdView.bodyView?.hide()
                    } else {
                        nativeAdView.bodyView?.show()
                        (nativeAdView.bodyView as TextView).text = nativeAd.body
                    }

                    if (nativeAd.callToAction == null) {
                        nativeAdView.callToActionView?.hide()
                    } else {
                        nativeAdView.callToActionView?.show()
                        (nativeAdView.callToActionView as Button).text = nativeAd.callToAction
                    }

                    if (nativeAd.price == null) {
                        nativeAdView.priceView?.hide()
                    } else {
                        nativeAdView.priceView?.show()
                        (nativeAdView.priceView as TextView).text = nativeAd.price
                    }

                    if (nativeAd.starRating == null) {
                        nativeAdView.starRatingView?.hide()
                    } else {
                        nativeAdView.starRatingView?.show()
                        (nativeAdView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                    }

                    if (nativeAd.advertiser == null) {
                        nativeAdView.advertiserView?.hide()
                    } else {
                        (adView.advertiserView as TextView).text = nativeAd.advertiser
                        nativeAdView.advertiserView?.show()
                    }

                    nativeAdView.setNativeAd(nativeAd)

                }
                .withAdListener(object: AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)

                        val error =
                            """domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}""""

                        Log.e(TAG, error)
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }*/

    private fun setLikeDislike(project: Project) {
        val likeCommentText = "${getLikesString(project.likes.toInt())} • ${getCommentsString(project.comments.toInt())} • ${getContributorsString(project.contributors.size)}"
        likeComment.text = likeCommentText
    }

    private fun getCommentsString(size: Int): String {
        return if (size == 1) {
            "1 Comment"
        } else {
            "$size Comments"
        }
    }

    private fun getLikesString(size: Int): String{
        return if (size == 1) {
            "1 Like"
        } else {
            "$size Likes"
        }
    }

    private fun getContributorsString(size: Int): String{
        return if (size == 1) {
            "1 Contributor"
        } else {
            "$size Contributors"
        }
    }

    private fun setJoinButton(project: Project) {
        val currentUserId = UserManager.currentUserId

        if (project.isBlocked) {
            joinBtn.hide()
        } else {
            Firebase.firestore.collection(PROJECT_REQUESTS)
                .whereEqualTo(PROJECT_ID, project.id)
                .whereEqualTo(SENDER_ID, currentUserId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, error.localizedMessage.orEmpty())
                        return@addSnapshotListener
                    }

                    if (value != null) {
                        if (value.isEmpty) {
                            // getting the current user and checking if the project has been added to collaborations
                            Firebase.firestore.collection(USERS).document(UserManager.currentUserId)
                                .get()
                                .addOnSuccessListener {
                                    if (it != null && it.exists()) {
                                        val currentUser = it.toObject(User::class.java)!!
                                        if (currentUser.collaborations.contains(project.id)) {
                                            joinBtn.hide()
                                        } else {
                                            joinBtn.text = view.context.getString(R.string.join)
                                            joinBtn.setOnClickListener {
                                                projectClickListener.onProjectJoinClick(project)
                                            }
                                        }
                                    }
                                }.addOnFailureListener {
                                    joinBtn.text = view.context.getString(R.string.join)
                                    joinBtn.setOnClickListener {
                                        projectClickListener.onProjectJoinClick(project)
                                    }
                                }
                        } else {
                            val projectRequest = value.toObjects(ProjectRequest::class.java).first()

                            // already requested
                            joinBtn.text = view.context.getString(R.string.undo)
                            joinBtn.setOnClickListener {
                                projectClickListener.onProjectUndoClick(project, projectRequest)
                            }
                        }
                    }
                }
        }
    }

    companion object {
        private const val TAG = "ProjectViewHolder"

        fun newInstance(parent: ViewGroup): ProjectViewHolder {
            return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
        }

    }

}