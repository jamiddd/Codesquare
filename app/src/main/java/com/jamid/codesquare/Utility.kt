package com.jamid.codesquare

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.util.Log
import android.util.Patterns
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.jamid.codesquare.adapter.recyclerview.HorizontalMediaAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.TooltipLayoutBinding
import com.jamid.codesquare.listeners.ChipClickListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.collections.set
import kotlin.math.roundToInt

fun TextInputLayout.removeError() {
    this.error = null
    this.isErrorEnabled = false
}

fun TextInputLayout.showError(error: String) {
    this.isErrorEnabled = true
    this.error = error
}

fun TextInputLayout.showError(error: Throwable?) {
    error?.localizedMessage?.let {
        showError(it)
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val calendar1 = Calendar.getInstance()
    calendar1.time = date1
    val calendar2 = Calendar.getInstance()
    calendar2.time = date2
    return calendar1[Calendar.YEAR] == calendar2[Calendar.YEAR] && calendar1[Calendar.MONTH] == calendar2[Calendar.MONTH] && calendar1[Calendar.DAY_OF_MONTH] == calendar2[Calendar.DAY_OF_MONTH]
}

fun isYesterday(date: Date): Boolean {
    val currentDate = Date(System.currentTimeMillis())
    val currentCalendar = Calendar.getInstance()
    currentCalendar.time = currentDate

    val nextCalendar = Calendar.getInstance()
    nextCalendar.time = date

    return currentCalendar[Calendar.DAY_OF_MONTH] - nextCalendar[Calendar.DAY_OF_MONTH] == 1
}

fun isThisWeek(date: Date): Boolean {
    val currentDate = Date(System.currentTimeMillis())
    val currentCalendar = Calendar.getInstance()
    currentCalendar.time = currentDate

    val nextCalendar = Calendar.getInstance()
    nextCalendar.time = date

    return currentCalendar[Calendar.WEEK_OF_MONTH] == nextCalendar[Calendar.WEEK_OF_MONTH]
}

fun compressImage(context: Context, item: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(item)
        val imageBitmap = BitmapFactory.decodeStream(inputStream)

        val file = context.convertBitmapToFile(imageBitmap, 60)
        /*
        val mWidth = imageBitmap.width.toFloat()
        val mHeight = imageBitmap.height.toFloat()

        val fWidth = minOf(mWidth, 600f).toInt()
        val fHeight = ((mHeight / mWidth) * fWidth).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, fWidth, fHeight, true)
        val file = File.createTempFile(randomId(), ".jpg", context.cacheDir)

        storeBitmapToFile(file, scaledBitmap)*/
        if (file != null) {
            FileProvider.getUriForFile(context, FILE_PROV_AUTH, file)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "compressImage: ${e.localizedMessage}")
        null
    }

}

/*fun TextView.setDrawableColor(color: Int) {
    for (drawable in compoundDrawables) {
        drawable?.setTint(color)
    }
}*/

fun Context.getOrderFromString(s: String?): FeedOrder {
    return when (s) {
        null -> FeedOrder.DESC
        "desc" -> FeedOrder.DESC
        "asc" -> FeedOrder.ASC
        else -> FeedOrder.DESC
    }
}

fun Fragment.getOrderFromString(s: String?): FeedOrder {
    return requireContext().getOrderFromString(s)
}

fun Fragment.getSortFromString(s: String?): FeedSort {
    return requireContext().getSortFromString(s)
}

fun Context.getSortFromString(s: String?): FeedSort {
    return when (s) {
        null -> FeedSort.MOST_RECENT
        getString(R.string.sort_time) -> FeedSort.MOST_RECENT
        getString(R.string.sort_location) -> FeedSort.LOCATION
        getString(R.string.sort_relevance) -> FeedSort.MOST_VIEWED
        getString(R.string.sort_likes) -> FeedSort.LIKES
        else -> FeedSort.CONTRIBUTORS
    }
}


fun darkenColor(color: Int): Int {
    return ColorUtils.blendARGB(color, Color.BLACK, 0.5f)
}

/*fun lightenColor(color: Int): Int {
    return ColorUtils.blendARGB(color, Color.WHITE, 0.5f)
}*/

/*fun manipulateColor(color: Int, factor: Float): Int {
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).roundToInt()
    val g = (Color.green(color) * factor).roundToInt()
    val b = (Color.blue(color) * factor).roundToInt()
    return Color.argb(
        a,
        r.coerceAtMost(255),
        g.coerceAtMost(255),
        b.coerceAtMost(255)
    )
}*/

fun getWindowWidth() = Resources.getSystem().displayMetrics.widthPixels

fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.fadeOut(): Animator {
    val objAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 0f)
    objAnimator.duration = 250
    objAnimator.start()
    return objAnimator
}

fun View.fadeIn(): Animator {
    this.visibility = View.VISIBLE
    val objAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 1f)
    objAnimator.duration = 250
    objAnimator.start()
    return objAnimator
}


fun View.showWithAnimations(): AnimatorSet {
    this.scaleX = 0f
    this.scaleY = 0f

    this.show()

    val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1f)
    val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1f)

    return AnimatorSet().apply {
        duration = 250
        interpolator = AccelerateDecelerateInterpolator()
        playTogether(objAnimator, objAnimator1)
        start()
    }
}

/*fun View.isVisibleOnScreen(): Boolean {
    if (!this.isShown) {
        return false
    }
    val actualPosition = Rect()
    this.getGlobalVisibleRect(actualPosition)
    val screen = Rect(0, 0, getWindowWidth(), getWindowHeight())
    return actualPosition.intersect(screen)
}*/

fun View.hide() {
    this.visibility = View.GONE
}

fun Fragment.getMimeType(uri: Uri) = requireContext().getMimeType(uri)

fun Context.getMimeType(uri: Uri): String? {
    return if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        contentResolver.getType(uri)
    } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(
            uri
                .toString()
        )
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.lowercase(Locale.getDefault())
        )
    }
}

fun View.hideWithAnimation(): AnimatorSet {
    val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f)
    val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f)

    return AnimatorSet().apply {
        duration = 250
        interpolator = AccelerateDecelerateInterpolator()
        playTogether(objAnimator, objAnimator1)
        start()
    }
}

fun Fragment.getStatusBarHeight() = requireActivity().getStatusBarHeight()

fun Activity.getStatusBarHeight(): Int {
    val rectangle = Rect()
    window.decorView.getWindowVisibleDisplayFrame(rectangle)
    return rectangle.top
}

fun Fragment.checkPermission(permission: String, onCheck: (isGranted: Boolean) -> Unit) =
    requireActivity().checkPermission(permission, onCheck)

fun Activity.checkPermission(permission: String, onCheck: (isGranted: Boolean) -> Unit) {
    when {
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED -> {
            onCheck(true)
        }
        shouldShowRequestPermissionRationale(permission) -> {
            toast("Grant permission: $permission")
        }
        else -> {
            onCheck(false)
        }
    }
}


@OptIn(ExperimentalPagingApi::class)
fun Fragment.showTooltip(
    msg: String,
    container: ViewGroup,
    anchorView: View,
    side: AnchorSide
): View? {
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

    when (side) {
        AnchorSide.Left -> {}
        AnchorSide.Top -> {}
        AnchorSide.Right -> {}
        AnchorSide.Bottom -> {
            val arrowY =
                anchorY /*+ anchorView.measuredHeight*/ + resources.getDimension(R.dimen.generic_len_2)

            val hb = anchorCenterX.toFloat() / getWindowWidth()
            val vb = arrowY / getWindowHeight()

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
                b.root.fadeOut().doOnEnd {
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

/*fun slideRightNavOptions(): NavOptions {
    return navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }
}*/

fun randomId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun CharSequence?.isValidPassword() =
    !isNullOrEmpty() && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{4,}\$")
        .matcher(this).matches()

fun getTextForTime(time: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        time,
        Calendar.getInstance().timeInMillis,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

/*fun getTextForChatTime(time: Long): String {
    val oneDay = 24 * 60 * 60 * 1000
    val oneWeek = 7 * oneDay
    val now = System.currentTimeMillis()
    val messageDate = Calendar.getInstance()
    messageDate.time = Date(time)

    val nowDate = Calendar.getInstance()
    nowDate.time = Date(time)

    val diff = abs(now - time)
    val isDifferentDay =
        messageDate.get(Calendar.DAY_OF_MONTH) != nowDate.get(Calendar.DAY_OF_MONTH)
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
}*/

fun <T : Any> List<T>.addItemToList(item: T): List<T> {
    val newList = this.toMutableList()
    newList.add(item)
    return newList
}

fun <T : Any> List<T>.removeItemFromList(item: T): List<T> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        val newList = this.toMutableList()
        newList.remove(item)
        newList
    }
}

fun View.slideReset(): Animator {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
    return animator
}

fun View.slideUp(offset: Float): Animator {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
    return animator
}

fun View.slideDown(offset: Float): Animator {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
    return animator
}

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard() }
}

@Suppress("DEPRECATION")
fun Context.showKeyboard() {
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

fun Fragment.getTextForSizeInBytes(size: Long) = requireContext().getTextForSizeInBytes(size)

fun Context.getTextForSizeInBytes(size: Long): String {
    return android.text.format.Formatter.formatShortFileSize(
        this,
        size
    )
}

// must be in the form [a, b, c]
fun String.toList(): List<Any> {

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
fun Fragment.search(index: Index, query: String, isPost: Boolean = true, progressBar: ProgressBar? = null, onComplete: (Result<List<SearchQuery>>) -> Unit) {
    search(index, query, isPost, progressBar, onComplete)
}*/

fun search() {

}

fun search(
    client: Client,
    queries: List<IndexQuery>,
    onComplete: (Result<List<SearchQuery>>) -> Unit
) {
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
                    "post" -> 0
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

fun EditText.onDone(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke()
            return@setOnEditorActionListener true
        }
        false
    }
}

fun processUser(user: User) {
    val currentUser = UserManager.currentUser
    user.isCurrentUser = currentUser.id == user.id
}

fun processUsers(users: List<User>) {
    for (user in users) {
        processUser(user)
    }
}

fun processUsers(vararg users: User): Array<out User> {
    val currentUser = UserManager.currentUser
    val usersList = mutableListOf<User>()
    for (user in users) {
        user.isCurrentUser = currentUser.id == user.id
        usersList.add(user)
    }
    return usersList.toTypedArray()
}

/* isSaved and isLiked is not checked here */
/*fun processPosts(posts: Array<out Post>): Array<out Post> {
    val currentUser = UserManager.currentUser
    for (post in posts) {
        post.isMadeByMe = post.creator.userId == currentUser.id
        post.isBlocked = post.blockedList.contains(currentUser.id)

        val set1 = post.requests.toSet()
        val set2 = currentUser.postRequests.toSet()
        val intersection = set1.intersect(set2)

        post.isRequested = intersection.isNotEmpty()
        post.isCollaboration = currentUser.collaborations.contains(post.id)
    }

    return posts
}*/

fun processPosts(posts: List<Post>) {
    for (post in posts) {
        processPost(post)
    }
}

fun processPost(post: Post) {
    val currentUser = UserManager.currentUser
    post.isMadeByMe = post.creator.userId == currentUser.id
    post.isBlocked = post.blockedList.contains(currentUser.id)
    val set1 = post.requests.toSet()
    val set2 = currentUser.postRequests.toSet()
    val intersection = set1.intersect(set2)
    post.isRequested = intersection.isNotEmpty()
    post.isCollaboration = currentUser.collaborations.contains(post.id)
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
    this.addAnimatorListener(object : Animator.AnimatorListener {
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

fun downloadBitmapUsingFresco(
    context: Context,
    photo: String,
    onComplete: (bitmap: Bitmap?) -> Unit
) {
    val imagePipeline = Fresco.getImagePipeline()
    val imageRequest = ImageRequestBuilder.newBuilderWithSource(photo.toUri())
        .build()

    val dataSource = imagePipeline.fetchDecodedImage(imageRequest, context)

    dataSource.subscribe(object : BaseBitmapDataSubscriber() {
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
    /* return when (tag) {
         ChatDetailFragment.TAG -> ChatDetailFragment.newInstance(bundle)
         ChatMediaFragment.TAG -> ChatMediaFragment.newInstance(bundle)
         ChannelGuidelinesFragment.TAG -> ChannelGuidelinesFragment.newInstance(bundle)
         PostContributorsFragment.TAG -> PostContributorsFragment.newInstance(bundle)
         MessageDetailFragment.TAG -> MessageDetailFragment.newInstance(bundle)
         PostFragment.TAG -> PostFragment.newInstance(bundle)
         CommentsFragment.TAG -> CommentsFragment.newInstance(bundle)
         TagFragment.TAG -> TagFragment.newInstance(bundle)
 //        ForwardFragment.TAG -> ForwardFragment.newInstance(bundle)
         else -> throw java.lang.NullPointerException("")
     }*/
    TODO("To return some fragment based on tag @Deprecated")
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

fun Context.getAppSpecificFileUri(fullPath: String, name: String): Uri? {
    val destDir = getNestedDir(filesDir, fullPath) ?: return null
    val destFile = getFile(destDir, name) ?: return null
    return FileProvider.getUriForFile(this, FILE_PROV_AUTH, destFile)
}

fun Fragment.getMetadataForFile(uri: Uri) = requireActivity().getMetadataForFile(uri)

fun Context.getMetadataForFile(uri: Uri): Metadata? {
    val cursor = contentResolver.query(uri, null, null, null, null)

    return try {
        cursor?.moveToFirst()
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
        val name =
            cursor?.getString(nameIndex ?: 0) ?: throw NullPointerException("Name of $uri is null")

        val size = (cursor.getLong(sizeIndex ?: 0))
        cursor.close()
        val ext = "." + name.split('.').last()

        Metadata(size, name, uri.toString(), ext, 0, 0)
    } catch (e: Exception) {
        Log.e(TAG, "getMetadataForFile: ${e.localizedMessage}")
        null
    }
}


fun Fragment.getFileAsMediaItem(fullPath: String, name: String) =
    requireContext().getFileAsMediaItem(fullPath, name)

fun Context.getFileAsMediaItem(fullPath: String, name: String): MediaItem? {

    val destDir = getNestedDir(filesDir, fullPath) ?: return null
    val destFile = getFile(destDir, name) ?: return null

    val destUri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, destFile)
    val mimeType = getMimeType(destUri) ?: ""
    val (type, thumbnail) = when {
        mimeType.contains(video) -> {
            video to getObjectThumbnail(fullPath, name)
        }
        mimeType.contains(image) -> {
            image to getObjectThumbnail(fullPath, name)
        }
        mimeType.contains("application") -> {
            document to null
        }
        else -> {
            document to null
        }
    }

    val ext = "." + name.substringAfterLast('.', "")
    val size = destFile.length() / 1024

    return MediaItem(
        destUri.toString(), name, type,
        mimeType, size, ext, destFile.path, thumbnail
    )

}

fun Fragment.getObjectThumbnail(fullPath: String, name: String) =
    requireContext().getObjectThumbnail(fullPath, name)

@SuppressLint("NewApi")
@Suppress("DEPRECATION")
fun Context.getObjectThumbnail(fullPath: String, name: String): Bitmap? {
    val destDir = getNestedDir(filesDir, fullPath) ?: return null
    val destFile = getFile(destDir, name) ?: return null

    val destUri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, destFile)
    val mimeType = getMimeType(destUri)

    return if (Build.VERSION.SDK_INT >= 29) {
        try {
            if (mimeType?.contains(video) == true) {
                ThumbnailUtils.createVideoThumbnail(destFile, Size(400, 300), null)
            } else {
                ThumbnailUtils.createImageThumbnail(destFile, Size(200, 200), null)
            }
        } catch (e: Exception) {
            null
        }
    } else {
        try {
            if (mimeType?.contains(video) == true) {
                ThumbnailUtils.createVideoThumbnail(
                    destFile.path,
                    MediaStore.Video.Thumbnails.MICRO_KIND
                )
            } else {
                ThumbnailUtils.createImageThumbnail(
                    destFile.path,
                    MediaStore.Video.Thumbnails.MICRO_KIND
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun Fragment.getObjectThumbnail(uri: Uri) = requireContext().getObjectThumbnail(uri)

@Suppress("DEPRECATION")
fun Context.getObjectThumbnail(uri: Uri): Bitmap? {
    val mimeType = getMimeType(uri) ?: return null

    return if (Build.VERSION.SDK_INT >= 29) {
        return try {
            contentResolver.loadThumbnail(uri, Size(200, 200), null)
        } catch (e: Exception) {
            val file = File(uri.path!!)
            Log.e(TAG, "getObjectThumbnail: ${file.path} ---- ${e.localizedMessage}")
            null
        }
    } else {
        val id = ContentUris.parseId(uri)
        if (mimeType.contains(video)) {
            MediaStore.Video.Thumbnails.getThumbnail(
                contentResolver,
                id,
                MediaStore.Video.Thumbnails.MICRO_KIND,
                BitmapFactory.Options()
            )
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver,
                id,
                MediaStore.Images.Thumbnails.MICRO_KIND,
                BitmapFactory.Options()
            )
        }
    }
}


fun Context.getVideoThumbnailFromMessage(message: Message): Uri? {
    val name = "thumb_" + message.content + ".jpg"
    val fullPath = "images/thumbnails/${message.chatChannelId}"
    val fileDest = getNestedDir(filesDir, fullPath)
    return if (fileDest != null) {
        val f = getFile(fileDest, name)
        if (f != null) {
            FileProvider.getUriForFile(this, FILE_PROV_AUTH, f)
        } else {
            null
        }
    } else {
        null
    }
}

fun convertMediaListToMediaItemList(
    list: List<String>,
    mediaString: String
): List<MediaItem> {
    return list.mapIndexed { index, s ->
        val type = if (mediaString[index] == '0') {
            image
        } else {
            video
        }
        MediaItem(s, type = type)
    }
}

fun Context.getImageUriFromMessage(message: Message): Uri? {

    val fullPath = "images/${message.chatChannelId}"
    val name = message.content + message.metadata!!.ext

    val dest = getNestedDir(filesDir, fullPath)
    return if (dest != null) {
        val f = getFile(dest, name)
        if (f != null) {
            FileProvider.getUriForFile(this, FILE_PROV_AUTH, f)
        } else {
            null
        }
    } else {
        null
    }

}

fun Fragment.getImageUriFromMessage(message: Message): Uri? {
    return requireContext().getImageUriFromMessage(message)
}


fun Context.getColorResource(id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Fragment.getColorResource(id: Int): Int {
    return requireContext().getColorResource(id)
}

fun Context.getImageResource(id: Int): Drawable? {
    return ContextCompat.getDrawable(this, id)
}

fun Fragment.getImageResource(id: Int): Drawable? {
    return requireContext().getImageResource(id)
}

fun Context.attachAd(adView: AdView, removeBtn: View?) {
    val adRequest = AdRequest.Builder().build()

    adView.loadAd(adRequest)

    adView.adListener = object : AdListener() {
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

fun Fragment.attachAd(adView: AdView, removeBtn: View?) {
    requireContext().attachAd(adView, removeBtn)
}

fun ViewGroup.onChildrenChanged(onChange: (Sequence<View>) -> Unit) {
    this.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(p0: View?, p1: View?) {
            onChange(this@onChildrenChanged.children)
        }

        override fun onChildViewRemoved(p0: View?, p1: View?) {
            onChange(this@onChildrenChanged.children)
        }
    })
}

fun setHorizontalMediaRecycler(
    recyclerView: RecyclerView,
    horizontalMediaAdapter: HorizontalMediaAdapter
) {
    recyclerView.apply {
        adapter = horizontalMediaAdapter
        layoutManager =
            LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
    }
}

fun Fragment.getMediaDir(name: String) = requireContext().getMediaDir(name)

fun Context.getMediaDir(name: String): File? {
    val root = filesDir
    val imagesDir = File(root, name)
    return if (imagesDir.exists()) {
        imagesDir
    } else {
        try {
            if (imagesDir.mkdir()) {
                imagesDir
            } else {
                throw Exception("Could not create $name directory")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getImagesDir: ${e.localizedMessage}")
            null
        }
    }
}

fun getNestedDir(parent: File, fullPath: String): File? {
    val args = fullPath.split('/')
    var dir = parent

    return try {
        for (arg in args) {
            if (arg.length > 1) {
                dir = File(dir, arg)
                if (dir.exists())
                    continue
                else
                    if (dir.mkdir())
                        continue
                    else
                        throw Exception("Couldn't create a dir")
            }
        }
        dir
    } catch (e: Exception) {
        Log.e(TAG, "getNestedDir: ${e.localizedMessage}")
        null
    }
}

fun checkFileExists(parent: File, name: String): Boolean {
    val file = File(parent, name)
    return file.exists() && file.length() > 0
}

fun getFile(parent: File, name: String): File? {
    val file = File(parent, name)
    return try {
        if (file.exists()) {
            file
        } else {
            if (file.createNewFile()) {
                file
            } else {
                throw Exception("Couldn't create new file.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "getFile: ${e.localizedMessage}")
        null
    }
}

fun String.toPlural(): String {
    return if (this == "child") {
        "children"
    } else {
        this + "s"
    }
}

fun Fragment.getMediaItemsFromMessages(messages: List<Message>) =
    requireContext().getMediaItemsFromMessages(messages)

fun Context.getMediaItemsFromMessages(messages: List<Message>): List<MediaItemWrapper> {
    return messages.map { message ->
        val fullPath = "${message.type.toPlural()}/${message.chatChannelId}"
        val name = message.content + message.metadata!!.ext

        getFileAsMediaItem(fullPath, name)?.apply {
            this.name = message.metadata!!.name
            dateCreated = message.createdAt
            dateModified = message.updatedAt
            type = message.type
        }
    }.mapNotNull {
        it?.let {
            MediaItemWrapper(it, false, -1)
        }
    }
}



fun View.setSelectableItemBackground() {
    val outValue = TypedValue()
    this.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
    this.setBackgroundResource(outValue.resourceId)
}


fun Fragment.getAllImagesInChannel(channelId: String) =
    requireContext().getAllImagesInChannel(channelId)

fun Context.getAllImagesInChannel(channelId: String): List<MediaItem> {

    val path = "images/$channelId"
    val files = getAllFilesInDir(path)
    val mediaItems = mutableListOf<MediaItem>()

    for (file in files) {
        val uri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, file)
        val name = file.name

        val mimeType = getMimeType(uri)!!
        val size = file.length() / 1024

        val ext = "." + name.substringAfterLast('.', "")
        val thumbnail = getObjectThumbnail(uri)

        mediaItems.add(
            MediaItem(
                uri.toString(),
                name,
                image,
                mimeType,
                size,
                ext,
                file.path,
                thumbnail
            )
        )
    }

    return mediaItems

}

fun Context.getAllFilesInDir(fullPath: String): List<File> {
    val dir = getNestedDir(filesDir, fullPath)
    return if (dir != null) {
        val files = dir.listFiles() ?: emptyArray()
        files.toList()
    } else {
        emptyList()
    }
}

fun BottomSheetDialogFragment.getFrameLayout(): FrameLayout? {
    return dialog?.window!!.decorView.findViewById(com.google.android.material.R.id.design_bottom_sheet)
}

fun BottomSheetDialogFragment.getBottomSheetBehavior(): BottomSheetBehavior<FrameLayout>? {
    val frame = getFrameLayout()
    return if (frame != null) BottomSheetBehavior.from(frame) else null
}

fun BottomSheetDialogFragment.getTouchOutsideView(): View? {
    return dialog?.window?.decorView?.findViewById(com.google.android.material.R.id.touch_outside)
}

fun showMoreSomething() {
    /*binding.postContent.doOnLayout {
            if (binding.postContent.lineCount > MAX_LINES && !isTextExpanded) {
                val lastCharShown = binding.postContent.layout.getLineVisibleEnd(MAX_LINES - 1)
                binding.postContent.maxLines = MAX_LINES
                val moreString = "Show more"
                val suffix = "  $moreString"

                val actionDisplayText: String = post.content.substring(0, lastCharShown - suffix.length - 3) + "..." + suffix
                val truncatedSpannableString = SpannableString(actionDisplayText)
                val startIndex = actionDisplayText.indexOf(moreString)

                val cs = object: ClickableSpan() {

                    override fun onClick(p0: View) {
                        postClickListener.onPostClick(post.copy())
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        val color = if (view.context.isNightMode()) {
                            Color.WHITE
                        } else {
                            Color.BLACK
                        }
                        ds.color = color
                    }

                }

                val cs1 = object : ClickableSpan() {
                    override fun onClick(p0: View) {

                        isTextExpanded = true

                        binding.postContent.maxLines = Int.MAX_VALUE
                        binding.postContent.text = post.content

                        view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {

                            delay(200)

                            binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }

                            binding.postContent.text = post.content

                            delay(200)

                            binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }

                            binding.postContent.setOnClickListener {
                                postClickListener.onPostClick(post.copy())
                            }
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        val greyColor = ContextCompat.getColor(view.context, R.color.darker_grey)
                        ds.color = greyColor
                    }

                }

                truncatedSpannableString.setSpan(cs,
                    0,
                    startIndex - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                truncatedSpannableString.setSpan(cs1,
                    startIndex,
                    startIndex + moreString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                binding.postContent.movementMethod = LinkMovementMethod.getInstance()

                binding.postContent.text = truncatedSpannableString

                view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                    delay(200)
                    binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }*/
}


/*fun Activity.setStatusBarColor() {
    val window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

// finally change the color

// finally change the color
    window.setStatusBarColor(ContextCompat.getColor(activity, R.color.my_statusbar_color))
}*/

fun Activity.setLightStatusBar() {
    val decorView = window.decorView
    val wic = ViewCompat.getWindowInsetsController(decorView)
    if (!isNightMode()) {
        wic?.isAppearanceLightStatusBars = true // true or false as desired.
        window.statusBarColor = Color.TRANSPARENT
    }
}

fun Activity.removeLightStatusBar() {
    val decorView = window.decorView
    val wic = ViewCompat.getWindowInsetsController(decorView)
    wic?.isAppearanceLightStatusBars = false // true or false as desired.
    window.statusBarColor = Color.BLACK
}

fun storeBitmapToFile(file: File, bitmap: Bitmap, quality: Int = 100) {
    try {
        if (!file.isDirectory) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val fos = FileOutputStream(file)
            fos.write(byteArray)
            fos.flush()
            fos.close()
        } else {
            Log.e(TAG, "storeBitmapToFile: File is a directory.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "storeBitmapToFile: ${e.localizedMessage}")
    }
}

fun Fragment.convertBitmapToFile(bitmap: Bitmap, quality: Int = 100) =
    requireContext().convertBitmapToFile(bitmap, quality)

fun Context.convertBitmapToFile(bitmap: Bitmap, quality: Int = 100): File? {
    return try {
        val file = File.createTempFile(randomId(), ".jpg", cacheDir)
        storeBitmapToFile(file, bitmap, quality)
        file
    } catch (e: Exception) {
        Log.e(TAG, "convertBitmapToFile: ${e.localizedMessage}")
        null
    }
}

fun ViewGroup.addTagChips(
    labels: List<String>,
    isMutable: Boolean = false,
    isDefaultTheme: Boolean = true,
    chipIcon: Drawable? = null,
    shortenText: Boolean = false,
    insertAtStart: Boolean = false,
    checkable: Boolean = false,
    chipClickListener: ChipClickListener? = null,
    tag: String? = null
) {
    removeAllViews()
    for (label in labels) {
        addTagChip(
            label,
            isMutable,
            isDefaultTheme,
            chipIcon,
            shortenText,
            insertAtStart,
            checkable,
            chipClickListener,
            tag
        )
    }
}

fun ViewGroup.addTagChip(
    label: String,
    isMutable: Boolean = false,
    isDefaultTheme: Boolean = true,
    chipIcon: Drawable? = null,
    shortenText: Boolean = false,
    insertAtStart: Boolean = false,
    checkable: Boolean = false,
    chipClickListener: ChipClickListener? = null,
    tag: String? = ""
) {
    label.trim()

    val chip = if (isDefaultTheme) {
        View.inflate(this.context, R.layout.default_chip, null) as Chip
    } else {
        // change this later
        View.inflate(this.context, R.layout.default_chip, null) as Chip
    }

    chip.apply {
        if (tag != null) {
            this.tag = tag
        }

        text = if (shortenText) {
            val (requireDots, len) = if (label.length > 16) {
                true to 16
            } else {
                false to label.length
            }
            label.take(len) + if (requireDots) "..." else ""
        } else {
            label
        }

        this.chipIcon = chipIcon

        isCheckedIconVisible = chipIcon != null
        isCheckable = checkable

        if (isMutable) {
            isCloseIconVisible = true

            setOnCloseIconClickListener {
                chipClickListener?.onCloseIconClick(chip)
            }
        } else {
            isCloseIconVisible = false

            setOnClickListener {
                chipClickListener?.onClick(chip)
            }

            setOnLongClickListener {
                chipClickListener?.onLongClick(chip)
                true
            }
        }
    }

    if (insertAtStart) {
        this.addView(chip, 0)
    } else {
        this.addView(chip)
    }

}

//exp
fun Activity.getRootView(): View {
    return findViewById(android.R.id.content)
}

fun Context.convertDpToPx(dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        this.resources.displayMetrics
    )
}

fun Activity.isKeyboardOpen(): Boolean {
    val visibleBounds = Rect()
    this.getRootView().getWindowVisibleDisplayFrame(visibleBounds)
    val heightDiff = getRootView().height - visibleBounds.height()
    val marginOfError = this.convertDpToPx(50F).roundToInt()
    return heightDiff > marginOfError
}

fun Activity.isKeyboardClosed(): Boolean {
    return !this.isKeyboardOpen()
}


const val TAG = "CodesquareLog"