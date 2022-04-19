package com.jamid.codesquare

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.paging.ExperimentalPagingApi
import com.airbnb.lottie.LottieAnimationView
import com.algolia.search.saas.Client
import com.algolia.search.saas.IndexQuery
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.TooltipLayoutBinding
import com.jamid.codesquare.ui.*
import com.jamid.codesquare.ui.home.chat.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.math.abs

val colorPalettesDay = mutableListOf(
    Pair(R.color.chip_back_blue_day, R.color.chip_front_blue_day),
    Pair(R.color.chip_back_pink_day, R.color.chip_front_pink_day),
    Pair(R.color.chip_back_purple_day, R.color.chip_front_purple_day),
    Pair(R.color.chip_back_teal_day, R.color.chip_front_teal_day)
)

val colorPalettesNight = mutableListOf(
    Pair(R.color.chip_back_blue_night, R.color.chip_front_blue_night),
    Pair(R.color.chip_back_pink_night, R.color.chip_front_pink_night),
    Pair(R.color.chip_back_purple_night, R.color.chip_front_purple_night),
    Pair(R.color.chip_back_teal_night, R.color.chip_front_teal_night)
)

fun getWindowWidth() = Resources.getSystem().displayMetrics.widthPixels

fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.showWithAnimations() {

    if (!this.isVisible) {
        this.scaleX = 0f
        this.scaleY = 0f

        this.show()

        val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1f)
        val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1f)

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(objAnimator, objAnimator1)
            start()
        }
    }

}

fun View.isVisibleOnScreen(): Boolean {
    if (!this.isShown) {
        return false
    }
    val actualPosition = Rect()
    this.getGlobalVisibleRect(actualPosition)
    val screen = Rect(0, 0, getWindowWidth(), getWindowHeight())
    return actualPosition.intersect(screen)
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.hideWithAnimation() {

    if (this.isVisible) {
        val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f)
        val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f )

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(objAnimator, objAnimator1)
            start()
        }

        this.disappear()
    }

}

@OptIn(ExperimentalPagingApi::class)
fun Fragment.showTooltip(msg: String, container: ViewGroup, anchorView: View, side: AnchorSide): View? {
    // the parameters for this function should be the anchorView and gravity

    var v: View? = null

    // 1. first get the position of the anchor view on the screen and then find the center of the anchor view
    // 2. based on the gravity choose x and y where the arrow should point
    // 3. find the horizontal bias for the arrow based on the ratio made by (startToX/XtoEnd) of the screen
    // 4. apply the layout params to the arrow and set appropriate paddings and constraints
    // 5. set scrim click to dismiss the tooltip

    val pos = intArrayOf(0, 0)
    
    anchorView.getLocationInWindow(pos)

    val anchorX = pos[0]
    val anchorY = pos[1]

    val widthRadius = anchorView.measuredWidth / 2
    val heightRadius = anchorView.measuredHeight / 2

    val anchorCenterX = anchorX + widthRadius
    val anchorCenterY = anchorY + heightRadius

    Log.d(TAG, "showTooltip: $msg, ($anchorX, $anchorY), ($anchorCenterX, $anchorCenterY)")

    when (side) {
        AnchorSide.Left -> {}
        AnchorSide.Top -> {}
        AnchorSide.Right -> {}
        AnchorSide.Bottom -> {
            val arrowY = anchorY /*+ anchorView.measuredHeight*/ + resources.getDimension(R.dimen.generic_len_2)

            val hb = anchorCenterX.toFloat() / getWindowWidth()
            val vb = arrowY/ getWindowHeight()

            Log.d(TAG, "showTooltip: hb=$hb, vb=$vb")

            v = layoutInflater.inflate(R.layout.tooltip_layout, container, false)
            val b = TooltipLayoutBinding.bind(v)

            container.addView(b.root)

            b.tooltipMsg.text = msg

            b.tooltipTail.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = b.root.id
                endToEnd = b.root.id
                topToTop = b.root.id
                bottomToBottom = b.root.id

                horizontalBias = hb
                verticalBias = vb
            }

            b.tooltipMsg.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = b.tooltipTail.id
                endToEnd = b.tooltipTail.id
                topToTop = b.tooltipTail.id
                horizontalBias = hb

                val m = resources.getDimension(R.dimen.unit_len) * 1.5
                setMargins(0, m.toInt(), 0, 0)
            }

            b.root.setOnClickListener {
                val fadeAnimation = ObjectAnimator.ofFloat(b.root, View.ALPHA, 0f)
                fadeAnimation.duration = 300
                fadeAnimation.start()

                fadeAnimation.doOnEnd {
                    container.removeView(b.root)
                }
            }

        }
    }

    return v

}

fun View.disappear() {
    this.visibility = View.INVISIBLE
}

fun Fragment.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().toast(msg, duration)
}

fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).show()
}

fun slideRightNavOptions(): NavOptions {
    return navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }
}

fun randomId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun CharSequence?.isValidPassword() =
    !isNullOrEmpty() && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{4,}\$")
        .matcher(this).matches()

fun getTextForTime(time: Long): String {
    return DateUtils.getRelativeTimeSpanString(time, Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS).toString()
}

fun getTextForChatTime(time: Long): String {
    val oneDay = 24 * 60 * 60 * 1000
    val oneWeek = 7 * oneDay
    val now = System.currentTimeMillis()
    val messageDate = Calendar.getInstance()
    messageDate.time = Date(time)

    val nowDate = Calendar.getInstance()
    nowDate.time = Date(time)

    val diff = abs(now - time)
    val isDifferentDay = messageDate.get(Calendar.DAY_OF_MONTH) != nowDate.get(Calendar.DAY_OF_MONTH)
    return when {
        diff > oneWeek -> {
            SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.UK).format(time)
        }
        diff > oneDay -> {
            SimpleDateFormat("hh:mm a EEEE", Locale.UK).format(time)
        }
        isDifferentDay -> {
            SimpleDateFormat("hh:mm a EEEE", Locale.UK).format(time)
        }
        else -> {
            SimpleDateFormat("hh:mm a", Locale.UK).format(time)
        }
    }
}

fun <T: Any> List<T>.addItemToList(item: T): List<T> {
    val newList = this.toMutableList()
    newList.add(item)
    return newList
}

fun <T: Any> List<T>.removeItemFromList(item: T): List<T> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        val newList = this.toMutableList()
        newList.remove(item)
        newList
    }
}

fun View.slideReset() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun View.slideUp(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun View.slideDown(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard() }
}

@Suppress("DEPRECATION")
private fun Context.showKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (Build.VERSION.SDK_INT > 30) {
        inputMethodManager.showSoftInput((this as Activity).window.decorView, 0)
    } else {
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

/**
 * Extension function to hide the keyboard from the given context
 *
 * @param view
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.isNightMode(): Boolean {
    return requireContext().isNightMode()
}

fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}


/*fun getExtensionForMime(mime: String): String {
    return when (mime) {
        "audio/aac" -> ".aac"
        "video/x-msvideo" -> ".avi"
        "image/bmp" -> ".bmp"
        "text/css" -> ".css"
        "text/csv" -> ".csv"
        "application/msword" -> ".doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
        "application/gzip" -> ".gz"
        "image/gif" -> ".gif"
        "text/html" -> ".html"
        "image/vnd.microsoft.icon" -> ".ico"
        "image/jpeg" -> ".jpg"
        "text/javascript" -> ".js"
        "application/json" -> ".json"
        "audio/mpeg" -> ".mp3"
        "video/mp4" -> ".mp4"
        "video/mpeg" -> ".mpeg"
        "application/vnd.oasis.opendocument.presentation" -> ".odp"
        "application/vnd.oasis.opendocument.spreadsheet" -> ".ods"
        "application/vnd.oasis.opendocument.text" -> ".odt"
        "audio/ogg" -> ".oga"
        "video/ogg" -> ".ogv"
        "font/otf" -> ".otf"
        "image/png" -> ".png"
        "application/pdf" -> ".pdf"
        "application/x-httpd-php" -> ".php"
        "application/vnd.ms-powerpoint" -> ".ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx"
        "application/vnd.rar" -> ".rar"
        "application/rtf" -> ".rtf"
        "application/x-sh" -> ".sh"
        "image/svg+xml" -> ".svg"
        "application/x-tar" -> ".tar"
        "font/ttf" -> ".ttf"
        "text/plain" -> ".txt"
        "audio/wav" -> ".wav"
        "audio/webm" -> ".weba"
        "video/webm" -> ".webm"
        "image/webp" -> ".webp"
        "font/woff" -> ".woff"
        "font/woff2" -> ".woff2"
        "application/xhtml+xml" -> ".xhtml"
        "application/vnd.ms-excel" -> ".xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
        "application/xml" -> ".xml"
        "text/xml" -> ".xml"
        "application/zip" -> ".zip"
        "application/x-7z-compressed" -> ".7z"
        else -> ""
    }
}*/

fun getTextForSizeInBytes(size: Long): String {
    return when {
        size > (1024 * 1024) -> {
            val sizeInMB = size.toFloat()/(1024 * 1024)
            sizeInMB.toString().take(4) + " MB"
        }
        size/1024 > 100 -> {
            val sizeInMB = size.toFloat()/(1024 * 1024)
            sizeInMB.toString().take(4) + " MB"
        }
        else -> {
            val sizeInKB = size.toFloat()/1024
            sizeInKB.toString().take(4) + " KB"
        }
    }
}

// must be in the form [a, b, c]
fun String.toList() : List<Any> {

    if (this.first() != '[') {
        return emptyList()
    }

    if (this.first() != this.last()) {
        return emptyList()
    }

    if (this.length == 2) {
        return emptyList()
    }

    val newList = mutableListOf<Any>()
    val newString = this.substring(1, this.length - 1)
    val words = newString.split(',')

    val sample = words.first()
    when {
        sample.startsWith('{') -> {
            // all are objects
            for (word in words) {
                newList.add(word.toHashMap())
            }
        }
        sample.startsWith('\"') -> {
            // all are strings
            for (word in words) {
                newList.add(word.substring(1, word.length - 1))
            }
        }
        sample.isDigitsOnly() -> {
            // all are numbers
            for (word in words) {
                newList.add(word.toInt())
            }
        }
    }

    return newList
}

// must be in the form { }
fun String.toHashMap(): Map<String, Any> {

    if (this.first() != '{') {
        return emptyMap()
    }

    if (this.first() != this.last()) {
        return emptyMap()
    }

    val map = mutableMapOf<String, Any>()

    fun z(pair: String) {
        val keyValue = pair.split(':')
        val key = keyValue.first()
        val value = keyValue.last()

        // object
        if (value.startsWith('{')) {
            val newValue = value.toHashMap()
            map[key] = newValue
        }

        // list
        if (value.startsWith('[')) {
            val newValue = value.toList()
            map[key] = newValue
        }

        // number
        if (value.isNotEmpty() && value.isDigitsOnly()) {
            val newValue = value.toLong()
            map[key] = newValue
        }

        // string
        if (value.first() == '\"' && value.last() == '\"') {
            map[key] = value
        }

    }

    if (this.contains(',')) {
        // multiple elements
        val fieldsAndValuesList = this.split(',')
        for (pair in fieldsAndValuesList) {
            z(pair)
        }
    } else {
        // single element
        z(this)
    }

    return map
}

/*fun CollectionReference.addOrder(field: String, direction: Query.Direction): Query {
    return this.orderBy(field, direction)
}*/

/*fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap? {
    val widthLight = bitmap.width
    val heightLight = bitmap.height

    val padding = if (widthLight > heightLight) {
        val diff = widthLight - heightLight
        diff/2 to 0
    } else {
        val diff = heightLight - widthLight
        0 to diff/2
    }

    val output = Bitmap.createBitmap(
        bitmap.width, bitmap.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)
    val paintColor = Paint()
    paintColor.flags = Paint.ANTI_ALIAS_FLAG
    val rectF = RectF(Rect(padding.first, padding.second, widthLight - padding.first, heightLight - padding.second))
    Log.d(TAG, rectF.toShortString())
    canvas.drawRoundRect(rectF, (widthLight / 2).toFloat(), (heightLight / 2).toFloat(), paintColor)
    val paintImage = Paint()
    paintImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paintImage)
    return output
}*/

fun Context.accentColor(): Int {
    val typedValue = TypedValue()
    val a = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
    val color = a.getColor(0, 0)
    a.recycle()
    return color
}

/*fun BottomNavigationView.attachToViewPager(viewPager: ViewPager2) {

    val menuItems = this.menu.iterator().withIndex()

    this.setOnItemSelectedListener {
        for (item in menuItems) {
            if (item.value.itemId == it.itemId) {
                viewPager.setCurrentItem(item.index, false)
            }
        }
        true
    }

    viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            for (item in menuItems) {
                if (item.index == position) {
                    this@attachToViewPager.selectedItemId = item.value.itemId
                }
            }
        }
    })
}*/

/*
fun Fragment.search(index: Index, query: String, isProject: Boolean = true, progressBar: ProgressBar? = null, onComplete: (Result<List<SearchQuery>>) -> Unit) {
    search(index, query, isProject, progressBar, onComplete)
}*/

fun search() {

}

fun search(client: Client, queries: List<IndexQuery>, onComplete: (Result<List<SearchQuery>>) -> Unit) {
    client.multipleQueriesAsync(queries, Client.MultipleQueriesStrategy.NONE) { jsonObject, exc ->
        if (exc != null) {
            onComplete(Result.Error(exc))
            Log.e(TAG, exc.localizedMessage.orEmpty())
        }

        if (jsonObject != null) {

            val ss = jsonObject.toString()
            if (ss.isBlank()) {
                Log.d(TAG, "JSON object is blank")
                return@multipleQueriesAsync
            }

            Log.d(TAG, ss)

            val titles = findValuesForKey("name", ss)
            val ids = findValuesForKey("objectId", ss)
            val type = findValuesForKey("type", ss)

            val list = mutableListOf<SearchQuery>()

            for (i in ids.indices) {
                val t = when (type[i]) {
                    "project" -> 0
                    "user" -> 1
                    else -> -1
                }
                list.add(SearchQuery(ids[i], titles[i], System.currentTimeMillis(), t))
            }

            onComplete(Result.Success(list))
        }
    }
}


private fun findValuesForKey(key: String, jsonString: String): List<String> {
    var index: Int
    val result = mutableListOf<String>()
    index = jsonString.indexOf(key, 0, true)
    Log.d(TAG, "Starting index for key = $key => $index")
    while (index != -1) {
        var valueString = ""
        for (i in (index + key.length + 3) until jsonString.length) {
            if (jsonString[i] != '\"') {
                valueString += jsonString[i]
            } else {
                break
            }
        }

        if (!valueString.contains('=') && valueString.isNotEmpty()) {
            result.add(valueString)
        }

        index = jsonString.indexOf(key, index + 1, true)
    }

    return result
}

fun processUsers(vararg users: User): Array<out User> {
    val currentUser = UserManager.currentUser
    val usersList = mutableListOf<User>()
    for (user in users) {
        user.isCurrentUser = currentUser.id == user.id
        user.isLiked = currentUser.likedUsers.contains(user.id)
        usersList.add(user)
    }
    return usersList.toTypedArray()
}

fun processProjects(projects: Array<out Project>): Array<out Project> {
    val currentUser = UserManager.currentUser
    for (project in projects) {
        project.isMadeByMe = project.creator.userId == currentUser.id
        project.isBlocked = project.blockedList.contains(currentUser.id)
        project.isLiked = currentUser.likedProjects.contains(project.id)
        project.isSaved = currentUser.savedProjects.contains(project.id)

        val set1 = project.requests.toSet()
        val set2 = currentUser.projectRequests.toSet()
        val intersection = set1.intersect(set2)

        project.isRequested = intersection.isNotEmpty()
        project.isCollaboration = currentUser.collaborations.contains(project.id)

        project.isArchived = currentUser.archivedProjects.contains(project.id)
    }

    return projects
}

fun View.enable() {
    this.isEnabled = true
}

fun View.disable() {
    this.isEnabled = false
}

fun View.setBackgroundTint(@ColorInt color: Int) {
    val drawable = this.background
    val drawable1 = DrawableCompat.wrap(drawable)
    DrawableCompat.setTint(drawable1, color)
    this.background = drawable1
}

fun LottieAnimationView.doOnAnimationEnd(onAnimationEnd: (p: Animator?) -> Unit) {
    this.addAnimatorListener(object: Animator.AnimatorListener {
        override fun onAnimationStart(p0: Animator?) {}

        override fun onAnimationEnd(p0: Animator?) {
            onAnimationEnd(p0)
        }

        override fun onAnimationCancel(p0: Animator?) {}

        override fun onAnimationRepeat(p0: Animator?) {}
    })
}

/*fun cropToSquare(bitmap: Bitmap): Bitmap? {
    val width = bitmap.width
    val height = bitmap.height
    val newWidth = if (height > width) width else height
    val newHeight = if (height > width) height - (height - width) else height
    var cropW = (width - height) / 2
    cropW = if (cropW < 0) 0 else cropW
    var cropH = (height - width) / 2
    cropH = if (cropH < 0) 0 else cropH
    return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
}*/

fun downloadBitmapUsingFresco(context: Context, photo: String, onComplete: (bitmap: Bitmap?) -> Unit) {
    val imagePipeline = Fresco.getImagePipeline()
    val imageRequest = ImageRequestBuilder.newBuilderWithSource(photo.toUri())
        .build()

    val dataSource = imagePipeline.fetchDecodedImage(imageRequest, context)

    dataSource.subscribe(object: BaseBitmapDataSubscriber() {
        override fun onNewResultImpl(bitmap: Bitmap?) {
            onComplete(bitmap)
        }

        override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
            onComplete(null)
        }

    }, Executors.newSingleThreadExecutor())
}

@OptIn(ExperimentalPagingApi::class)
fun getFragmentByTag(tag: String, bundle: Bundle): Fragment {
    return when (tag) {
        ChatFragment.TAG -> ChatFragment.newInstance(bundle)
        ChatDetailFragment.TAG -> ChatDetailFragment.newInstance(bundle)
        ChatMediaFragment.TAG -> ChatMediaFragment.newInstance(bundle)
        ChannelGuidelinesFragment.TAG -> ChannelGuidelinesFragment.newInstance(bundle)
        ProjectContributorsFragment.TAG -> ProjectContributorsFragment.newInstance(bundle)
        MessageDetailFragment.TAG -> MessageDetailFragment.newInstance(bundle)
        ProjectFragment.TAG -> ProjectFragment.newInstance(bundle)
        CommentsFragment.TAG -> CommentsFragment.newInstance(bundle)
        TagFragment.TAG -> TagFragment.newInstance(bundle)
        ForwardFragment.TAG -> ForwardFragment.newInstance(bundle)
        else -> ChatFragment.newInstance(bundle)
    }
}


fun shouldShowAd(currentDestinationId: Int, isInternetAvailable: Boolean): Boolean {
    val currentUser = UserManager.currentUser
    val eligibleFragments = listOf(R.id.homeFragment, R.id.commentsFragment)

    if (currentUser.premiumState.toInt() != -1) {
        return false
    }

    if (!eligibleFragments.contains(currentDestinationId)) {
        return false
    }

    if (!isInternetAvailable) {
        return false
    }

    return true
}

fun Context.getColorResource(id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Fragment.getColorResource(id: Int): Int {
    return requireContext().getColorResource(id)
}

fun Fragment.attachAdToFragment(adView: AdView, removeBtn: View?) {
    val adRequest = AdRequest.Builder().build()

    adView.loadAd(adRequest)

    adView.adListener = object: AdListener() {
        override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            adView.hide()
            removeBtn?.hide()
        }

        override fun onAdLoaded() {
            super.onAdLoaded()
            adView.show()
            removeBtn?.show()
        }

        override fun onAdOpened() {
            super.onAdOpened()
            adView.show()
            removeBtn?.show()
        }

        override fun onAdClosed() {
            super.onAdClosed()
            adView.hide()
            removeBtn?.hide()
        }

    }
}

const val TAG = "CodesquareLog"