package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.CustomPostAdBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.show
import java.util.*

class AdViewHolder(v: View): PostViewHolder(v) {

    private lateinit var binding: CustomPostAdBinding
    private val projectClickListener = view.context as ProjectClickListener

    override fun bind(project: Project?) {
        Log.d(TAG, project?.isAd.toString())

        binding = CustomPostAdBinding.bind(view)

        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT)
            .build()

        val nativeAdView = binding.root

        binding.adInfoIcon.setOnClickListener {
            projectClickListener.onAdInfoClick()
        }

        val adLoader = AdLoader.Builder(view.context, "ca-app-pub-3940256099942544/2247696110")
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

                    if (project != null) {
                        projectClickListener.onAdError(project)
                    }
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    binding.loadingAdText.hide()
                    binding.adPrimaryAction.show()
                }
            })
            .withNativeAdOptions(adOptions)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

    }

    companion object {
        private const val TAG = "AdViewHolder"
    }

}