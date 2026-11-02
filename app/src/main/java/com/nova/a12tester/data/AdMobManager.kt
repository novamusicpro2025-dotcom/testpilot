package com.nova.a12tester.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdMobManager manages Google Mobile Ads SDK initialization,
 * pre-loading official Google test rewarded ads, and showing ads
 * to award users coins in real-time.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"

    // Google AdMob Official Test Rewarded Ad Unit ID for Android
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Google AdMob Official Test Banner Ad Unit ID for Android
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Initialize Google Mobile Ads SDK on app startup.
     */
    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: $status")
                loadRewardedAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Loads the official Google test rewarded ad.
     */
    fun loadRewardedAd(context: Context, onLoaded: (() -> Unit)? = null) {
        if (isAdLoading) return
        if (rewardedAd != null) {
            _isAdReady.value = true
            onLoaded?.invoke()
            return
        }

        isAdLoading = true
        _isLoading.value = true
        _lastError.value = null

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Google Test Rewarded Ad loaded successfully.")
                    rewardedAd = ad
                    isAdLoading = false
                    _isLoading.value = false
                    _isAdReady.value = true
                    _lastError.value = null
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Failed to load Google Test Rewarded Ad: ${loadAdError.message}")
                    rewardedAd = null
                    isAdLoading = false
                    _isLoading.value = false
                    _isAdReady.value = false
                    _lastError.value = loadAdError.message
                }
            }
        )
    }

    /**
     * Shows the Google test rewarded ad to the user.
     * When completed, invokes [onUserEarnedReward] to grant coins in real-time.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdClosed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewardedAd(activity)
            onAdFailed("Ad is loading. Please wait a moment and try again.")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Google Test Rewarded Ad dismissed.")
                rewardedAd = null
                _isAdReady.value = false
                // Preload the next rewarded ad immediately
                loadRewardedAd(activity)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Failed to show Google Test Rewarded Ad: ${adError.message}")
                rewardedAd = null
                _isAdReady.value = false
                loadRewardedAd(activity)
                onAdFailed(adError.message)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Google Test Rewarded Ad showed.")
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User completed Google Test Rewarded Ad! Amount: ${rewardItem.amount}")
            onUserEarnedReward()
        }
    }
}
