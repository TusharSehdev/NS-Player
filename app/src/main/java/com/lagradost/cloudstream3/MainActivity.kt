package com.lagradost.cloudstream3

import android.app.Activity
import android.widget.Toast
import com.lagradost.nicehttp.Requests

val app: Requests = Requests()

object CommonActivity {
    var activity: Activity? = null

    @JvmStatic
    fun showToast(activity: Activity? = null, message: String, duration: Int? = null) {
        val act = activity ?: this.activity
        act?.runOnUiThread {
            Toast.makeText(act, message, duration ?: Toast.LENGTH_SHORT).show()
        }
    }
}
