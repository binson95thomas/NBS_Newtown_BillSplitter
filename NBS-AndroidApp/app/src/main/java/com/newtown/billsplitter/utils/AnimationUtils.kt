package com.newtown.billsplitter.utils

import android.animation.*
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.newtown.billsplitter.R

/**
 * Utility class for managing animations throughout the app
 */
object AnimationUtils {
    
    /**
     * Show ocean wave loading animation with fallback
     */
    fun showOceanLoadingAnimation(imageView: ImageView, context: Context) {
        try {
            // Try the complex ocean wave animation first
            val oceanWaveDrawable = ContextCompat.getDrawable(context, R.drawable.ocean_wave_animated) as? AnimatedVectorDrawable
            imageView.setImageDrawable(oceanWaveDrawable)
            imageView.visibility = View.VISIBLE
            oceanWaveDrawable?.start()
        } catch (e: Exception) {
            android.util.Log.w("AnimationUtils", "Failed to start complex ocean animation, trying simple version", e)
            try {
                // Try the simpler ocean animation
                val simpleOceanDrawable = ContextCompat.getDrawable(context, R.drawable.simple_ocean_animated) as? AnimatedVectorDrawable
                imageView.setImageDrawable(simpleOceanDrawable)
                imageView.visibility = View.VISIBLE
                simpleOceanDrawable?.start()
            } catch (e2: Exception) {
                android.util.Log.w("AnimationUtils", "Failed to start simple ocean animation, using basic fallback", e2)
                // Final fallback to simple rotation animation
                showSimpleLoadingAnimation(imageView, context)
            }
        }
    }
    
    /**
     * Fallback simple loading animation
     */
    private fun showSimpleLoadingAnimation(imageView: ImageView, context: Context) {
        // Set a simple ocean-themed drawable
        imageView.setImageResource(android.R.drawable.ic_popup_sync)
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.accent_500))
        imageView.visibility = View.VISIBLE
        
        // Start simple rotation animation
        val rotateAnimator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f)
        rotateAnimator.duration = 1000
        rotateAnimator.repeatCount = ObjectAnimator.INFINITE
        rotateAnimator.interpolator = LinearInterpolator()
        rotateAnimator.start()
        
        // Store animator reference to stop later
        imageView.tag = rotateAnimator
    }
    
    /**
     * Hide loading animation with fade out
     */
    fun hideLoadingAnimation(imageView: ImageView, onComplete: (() -> Unit)? = null) {
        // Stop fallback animator if it exists
        (imageView.tag as? ObjectAnimator)?.cancel()
        imageView.tag = null
        
        imageView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                imageView.visibility = View.GONE
                imageView.alpha = 1f
                imageView.clearColorFilter()
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Show success animation with celebration and fallback
     */
    fun showSuccessAnimation(imageView: ImageView, context: Context, onComplete: (() -> Unit)? = null) {
        try {
            val successDrawable = ContextCompat.getDrawable(context, R.drawable.simple_success_animated) as? AnimatedVectorDrawable
            
            // Scale in animation
            imageView.scaleX = 0f
            imageView.scaleY = 0f
            imageView.setImageDrawable(successDrawable)
            imageView.visibility = View.VISIBLE
            
            imageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    try {
                        successDrawable?.start()
                        
                        // Auto-hide after animation completes
                        imageView.postDelayed({
                            hideSuccessAnimation(imageView, onComplete)
                        }, 2000)
                    } catch (e: Exception) {
                        android.util.Log.w("AnimationUtils", "Failed to start success animation, using fallback", e)
                        showSimpleSuccessAnimation(imageView, context, onComplete)
                    }
                }
                .start()
        } catch (e: Exception) {
            android.util.Log.w("AnimationUtils", "Failed to load success animation, using fallback", e)
            showSimpleSuccessAnimation(imageView, context, onComplete)
        }
    }
    
    /**
     * Fallback simple success animation
     */
    private fun showSimpleSuccessAnimation(imageView: ImageView, context: Context, onComplete: (() -> Unit)? = null) {
        // Set a simple checkmark icon
        imageView.setImageResource(android.R.drawable.ic_menu_save)
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.accent_500))
        imageView.visibility = View.VISIBLE
        
        // Scale in with bounce
        imageView.scaleX = 0f
        imageView.scaleY = 0f
        
        imageView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                imageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // Auto-hide after showing
                        imageView.postDelayed({
                            hideSuccessAnimation(imageView, onComplete)
                        }, 1500)
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Hide success animation
     */
    private fun hideSuccessAnimation(imageView: ImageView, onComplete: (() -> Unit)? = null) {
        imageView.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                imageView.visibility = View.GONE
                imageView.scaleX = 1f
                imageView.scaleY = 1f
                imageView.alpha = 1f
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Bounce animation for buttons
     */
    fun bounceButton(view: View) {
        val bounceAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)
        bounceAnimator.duration = 150
        bounceAnimator.interpolator = DecelerateInterpolator()
        
        val bounceAnimatorX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        bounceAnimatorX.duration = 150
        bounceAnimatorX.interpolator = DecelerateInterpolator()
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(bounceAnimator, bounceAnimatorX)
        animatorSet.start()
    }
    
    /**
     * Pulse animation for important elements
     */
    fun pulseView(view: View, count: Int = 1) {
        val pulseAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f)
        pulseAnimator.duration = 300
        pulseAnimator.repeatCount = count - 1
        pulseAnimator.interpolator = AccelerateDecelerateInterpolator()
        
        val pulseAnimatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f)
        pulseAnimatorY.duration = 300
        pulseAnimatorY.repeatCount = count - 1
        pulseAnimatorY.interpolator = AccelerateDecelerateInterpolator()
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(pulseAnimator, pulseAnimatorY)
        animatorSet.start()
    }
    
    /**
     * Slide in from bottom animation
     */
    fun slideInFromBottom(view: View, duration: Long = 300) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Slide out to bottom animation
     */
    fun slideOutToBottom(view: View, duration: Long = 300, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                view.translationY = 0f
                view.alpha = 1f
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Fade in animation
     */
    fun fadeIn(view: View, duration: Long = 300) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
    
    /**
     * Fade out animation
     */
    fun fadeOut(view: View, duration: Long = 300, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Ocean wave entrance animation for cards
     */
    fun oceanWaveEntrance(view: View, delay: Long = 0) {
        view.translationY = 50f
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Celebration burst animation for success states
     */
    fun celebrationBurst(view: View, context: Context) {
        try {
            // Create multiple small views for celebration effect
            val parentView = view.parent as? android.view.ViewGroup ?: return
            val celebrationViews = mutableListOf<View>()
            
            repeat(6) { index ->
                val celebrationDot = View(context).apply {
                    // Use simple drawable instead of animated vector
                    setBackgroundColor(ContextCompat.getColor(context, R.color.accent_400))
                    layoutParams = android.view.ViewGroup.LayoutParams(12, 12)
                    x = view.x + view.width / 2f
                    y = view.y + view.height / 2f
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                }
                
                parentView.addView(celebrationDot)
                celebrationViews.add(celebrationDot)
                
                // Animate each dot in different directions
                val angle = (index * 60f) * Math.PI / 180 // 6 dots at 60-degree intervals
                val distance = 80f
                val endX = view.x + view.width / 2f + (Math.cos(angle) * distance).toFloat()
                val endY = view.y + view.height / 2f + (Math.sin(angle) * distance).toFloat()
                
                celebrationDot.animate()
                    .x(endX)
                    .y(endY)
                    .alpha(1f)
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(500)
                    .setStartDelay((index * 50).toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        celebrationDot.animate()
                            .alpha(0f)
                            .scaleX(0f)
                            .scaleY(0f)
                            .setDuration(200)
                            .withEndAction {
                                try {
                                    parentView.removeView(celebrationDot)
                                } catch (e: Exception) {
                                    android.util.Log.w("AnimationUtils", "Failed to remove celebration dot", e)
                                }
                            }
                            .start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            android.util.Log.w("AnimationUtils", "Failed to create celebration burst", e)
            // Simple fallback - just pulse the original view
            pulseView(view, 2)
        }
    }
}
