package com.basic.studentportal.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.basic.studentportal.data.model.ApiError
import com.google.gson.Gson
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Double.toCurrency(): String {
    return NumberFormat.getNumberInstance(Locale.US).format(this) + " ৳"
}

fun Double.toPercent(): String = String.format("%.1f%%", this)

fun <T> Response<T>.parseError(): String {
    return try {
        val json = errorBody()?.string()
        if (json != null) {
            Gson().fromJson(json, ApiError::class.java).message
        } else {
            "Unknown error occurred"
        }
    } catch (e: Exception) {
        "Error: ${message()}"
    }
}

fun String.capitalizeFirst(): String {
    return if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
}

fun String.toSubjectLabel(): String {
    return replace("_", " ").split(" ").joinToString(" ") { it.capitalizeFirst() }
}
