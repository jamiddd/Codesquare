package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.jamid.codesquare.darkenColor
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.databinding.CustomPostAdBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.PostClickListener
import com.jamid.codesquare.show
import java.util.*

/* "ca-app-pub-2159166722829360/7384689864" For play store version */
/* "ca-app-pub-3940256099942544/2247696110" For test version*/

class AdViewHolderSuper(v: View, listener: PostClickListener? = null): SuperPostViewHolder(v) {

    private lateinit var binding: CustomPostAdBinding
    private val projectClickListener = listener ?: view.context as PostClickListener
    private var isAdLoaded = false
    private var adOptions: NativeAdOptions? = null
    private var adLoader: AdLoader? = null
    private var adRequest: AdRequest? = null

    override fun bind(mPost: Post2?) {

        binding = CustomPostAdBinding.bind(view)
        binding.root.tag = this

        val nativeAdView = binding.root

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true).build()

        if (adLoader != null && adRequest != null) {
            adLoader!!.loadAd(adRequest!!)
        } else {
            adOptions = NativeAdOptions.Builder()
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE)
                .setVideoOptions(videoOptions)
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT)
                .build()

            binding.adInfoIcon.setOnClickListener {
                projectClickListener.onAdInfoClick()
            }

           /* val adUnitID = if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/2247696110"
            } else {
                "ca-app-pub-2159166722829360/7384689864"
            }*/

            adLoader = AdLoader.Builder(view.context, "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd { nativeAd ->

                    nativeAdView.headlineView = binding.adHeadline
                    nativeAdView.bodyView = binding.adSecondaryText
                    nativeAdView.mediaView = binding.adMediaView
                    nativeAdView.callToActionView = binding.adPrimaryAction
                    nativeAdView.iconView = binding.adAppIcon
                    nativeAdView.priceView = binding.adPriceText
                    nativeAdView.starRatingView = binding.adRating
                    nativeAdView.advertiserView = binding.adAdvertiser

                    (nativeAdView.headlineView as TextView).text = nativeAd.headline
                    nativeAd.mediaContent?.let {
                        nativeAdView.mediaView?.setMediaContent(it)
                        nativeAdView.mediaView?.setOnHierarchyChangeListener(object :
                            ViewGroup.OnHierarchyChangeListener {
                            override fun onChildViewAdded(parent: View, child: View) {
                                if (child is ImageView) {
                                    child.adjustViewBounds = true

                                    val bitmap = (child.drawable as BitmapDrawable).bitmap
                                    if (bitmap != null) {
                                        val palette = createPaletteSync(bitmap)
                                        val vc = palette.vibrantSwatch

                                        vc?.rgb?.let { it1 ->
                                            nativeAdView.callToActionView?.setBackgroundColor(
                                                darkenColor(it1)
                                            )
                                        }
                                    } else {
                                        Log.d(TAG, "onChildViewAdded: Bitmap is null")
                                    }
                                }
                            }

                            override fun onChildViewRemoved(parent: View, child: View) {}
                        })
                    }

                    if (nativeAd.icon != null) {
                        (nativeAdView.iconView as SimpleDraweeView).setImageURI(nativeAd.icon?.uri.toString())
                    } else {
                        (nativeAdView.iconView as SimpleDraweeView).hide()
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
                        (nativeAdView.advertiserView as TextView).text = nativeAd.advertiser
                        nativeAdView.advertiserView?.show()
                    }

                    nativeAdView.setNativeAd(nativeAd)

                    val newText = binding.adPrimaryAction.text.toString().lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    binding.adPrimaryAction.text = newText

                }
                .withAdListener(object: AdListener() {

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)

                        val error =
                            """domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}""""

                        Log.e(TAG, error)

                        if (mPost != null) {
                            if (mPost is Post2.Collab) {
                                projectClickListener.onAdError(mPost.post)
                            }
                        }
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        binding.loadingAdText.hide()
                        binding.adPrimaryAction.show()


                        isAdLoaded = true

                        if (binding.adPrimaryAction.text != "Install") {
                            binding.adPrimaryAction.icon = ContextCompat.getDrawable(view.context, com.jamid.codesquare.R.drawable.ic_round_arrow_forward_24)
                        }

                    }
                })
                .withNativeAdOptions(adOptions!!)
                .build()

            adRequest = AdRequest.Builder().build()

            adLoader?.loadAd(adRequest!!)
        }
    }

    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()


    companion object {
        private const val TAG = "AdViewHolder"
    }

}