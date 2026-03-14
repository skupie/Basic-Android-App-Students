package com.basic.studentportal.utils

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Animates a TextView counting up from [from] to [to] as a percentage string.
 * e.g.  0% → 81.8%
 */
fun TextView.animatePercent(
    from: Double = 0.0,
    to: Double,
    durationMs: Long = 1200
) {
    val animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
    animator.duration = durationMs
    animator.interpolator = DecelerateInterpolator()
    animator.addUpdateListener {
        val value = it.animatedValue as Float
        text = String.format("%.1f%%", value)
    }
    animator.start()
}

/**
 * Animates a TextView counting up from 0 to [to] as a whole integer.
 */
fun TextView.animateCount(
    to: Int,
    durationMs: Long = 900
) {
    val animator = ValueAnimator.ofInt(0, to)
    animator.duration = durationMs
    animator.interpolator = DecelerateInterpolator()
    animator.addUpdateListener {
        text = (it.animatedValue as Int).toString()
    }
    animator.start()
}

/**
 * Animates a TextView counting up from 0.0 to [to] with optional [suffix] and decimal precision.
 * Used for hero card stats like "86%", "44%", "73.5".
 */
fun animateCountFloat(
    textView: TextView,
    to: Double,
    suffix: String = "",
    decimals: Int = 0,
    durationMs: Long = 1100
) {
    val animator = ValueAnimator.ofFloat(0f, to.toFloat())
    animator.duration = durationMs
    animator.interpolator = DecelerateInterpolator()
    animator.addUpdateListener {
        val v = it.animatedValue as Float
        textView.text = if (decimals == 0) "${v.toInt()}$suffix"
                        else "${"%.${decimals}f".format(v)}$suffix"
    }
    animator.start()
}

/**
 * Animates a ProgressBar from 0 to [targetProgress].
 */
fun ProgressBar.animateProgress(
    targetProgress: Int,
    durationMs: Long = 1200
) {
    val animator = ValueAnimator.ofInt(0, targetProgress)
    animator.duration = durationMs
    animator.interpolator = DecelerateInterpolator()
    animator.addUpdateListener { progress = it.animatedValue as Int }
    animator.start()
}
