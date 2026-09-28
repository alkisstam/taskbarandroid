package com.alkisstam.taskbar.service

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntRect
import com.alkisstam.taskbar.R

/**
 * Window sized to the dock or a floating panel that frosts only what's under it.
 *
 * FLAG_BLUR_BEHIND always blurs the whole screen, so this uses window *background* blur
 * instead, which is clipped to the window's bounds and its background's rounded outline. That
 * API only exists on [Window], which WindowManager.addView() views don't have, hence the Dialog.
 *
 * It is always TYPE_APPLICATION_OVERLAY: with the a11y service running the dock is an
 * accessibility overlay and sits above it anyway, and without it the caller must attach this
 * before the dock so it stacks below. A Dialog can't carry the a11y service's window token.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class DockBlurWindow(private val context: Context) {
    private var dialog: Dialog? = null
    private var fadeAnimator: ValueAnimator? = null
    private var shownBounds: IntRect? = null
    private var shownCornerPx = -1f
    private var blurRadiusPx = (24 * context.resources.displayMetrics.density).toInt()

    val isAttached: Boolean get() = dialog?.isShowing == true

    fun attach() {
        if (isAttached) return
        detach()
        val d = Dialog(context, R.style.Theme_TaskBar_DockBlur)
        d.setCancelable(false)
        d.setContentView(View(context))
        val w = d.window ?: return
        w.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        w.setFormat(PixelFormat.TRANSLUCENT)
        w.setWindowAnimations(0)
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        w.attributes = w.attributes.apply {
            gravity = Gravity.TOP or Gravity.START
            width = 1
            height = 1
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        d.show()
        // Dialog.hide() only sets the decor GONE (surface destroyed, window kept in z-order);
        // a later show() just makes it visible again.
        d.hide()
        dialog = d
        shownBounds = null
        shownCornerPx = -1f
    }

    /** Places the blur under [bounds] (screen px) and fades it in. */
    fun show(bounds: IntRect, cornerRadiusPx: Float) {
        val d = dialog ?: return
        val w = d.window ?: return
        if (bounds == shownBounds && d.window?.decorView?.visibility == View.VISIBLE) return
        fadeAnimator?.cancel()
        try {
            w.attributes = w.attributes.apply {
                x = bounds.left
                y = bounds.top
                width = bounds.width
                height = bounds.height
                alpha = 0f
            }
            d.show()
            // Radius stays constant; the fade is done with window alpha, which SurfaceFlinger
            // also applies to the blur. Set it before the background so the corner radius is
            // picked up from the outline while blur is active.
            w.setBackgroundBlurRadius(blurRadiusPx)
            val corner = cornerRadiusPx.coerceAtMost(minOf(bounds.width, bounds.height) / 2f)
            if (corner != shownCornerPx) {
                w.setBackgroundDrawable(GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = corner
                })
                shownCornerPx = corner
            }
            shownBounds = bounds
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = FADE_MS
                addUpdateListener { anim ->
                    val win = dialog?.window ?: return@addUpdateListener
                    win.attributes = win.attributes.apply { alpha = anim.animatedValue as Float }
                }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to show dock blur", e)
        }
    }

    fun setBlurRadius(radiusPx: Int) {
        if (radiusPx == blurRadiusPx) return
        blurRadiusPx = radiusPx
        val w = dialog?.window ?: return
        if (w.decorView.visibility != View.VISIBLE) return
        try {
            w.setBackgroundBlurRadius(radiusPx)
            // Re-set the background so the rounded outline is re-applied to the new blur.
            w.setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = shownCornerPx.coerceAtLeast(0f)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update dock blur radius", e)
        }
    }

    fun hide() {
        fadeAnimator?.cancel()
        fadeAnimator = null
        shownBounds = null
        try { dialog?.hide() } catch (e: Exception) { Log.w(TAG, "Failed to hide dock blur", e) }
    }

    fun detach() {
        fadeAnimator?.cancel()
        fadeAnimator = null
        try { dialog?.dismiss() } catch (e: Exception) { Log.w(TAG, "Failed to remove dock blur", e) }
        dialog = null
        shownBounds = null
    }

    private companion object {
        const val TAG = "DockBlurWindow"
        const val FADE_MS = 160L
    }
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun Context.crossWindowBlurEnabled(): Boolean =
    getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

// One UI doesn't ship the AOSP cross-window blur (isCrossWindowBlurEnabled is always false);
// it blurs through its own SemBlurInfo API instead. BLUR_MODE_WINDOW (0) frosts what's behind
// the view's window within the view's bounds. Reflection because it's not in the public SDK.
internal fun View.semSetBlur(radiusPx: Int, cornerRadiusPx: Float): Boolean = try {
    val infoClass = semBlurInfoClass ?: return false
    val setBlurInfo = View::class.java.getMethod("semSetBlurInfo", infoClass)
    if (radiusPx <= 0) {
        setBlurInfo.invoke(this, *arrayOf<Any?>(null))
    } else {
        val builderClass = Class.forName("android.view.SemBlurInfo\$Builder")
        val builder = builderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(0)
        builderClass.getMethod("setRadius", Int::class.javaPrimitiveType).invoke(builder, radiusPx)
        builderClass.getMethod("setBackgroundColor", Int::class.javaPrimitiveType).invoke(builder, Color.TRANSPARENT)
        builderClass.getMethod("setBackgroundCornerRadius", Float::class.javaPrimitiveType).invoke(builder, cornerRadiusPx)
        setBlurInfo.invoke(this, builderClass.getMethod("build").invoke(builder))
    }
    true
} catch (e: Throwable) {
    Log.w("DockBlurWindow", "SemBlurInfo unavailable", e)
    false
}

internal fun semBlurSupported(): Boolean = semBlurInfoClass != null

private val semBlurInfoClass: Class<*>? by lazy { runCatching { Class.forName("android.view.SemBlurInfo") }.getOrNull() }
