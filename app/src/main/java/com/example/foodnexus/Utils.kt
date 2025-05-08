package com.example.foodnexus

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Toast

object Utils {

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showProgress(progressBar: Dialog) {
        progressBar.show()
    }

    fun hideProgress(progressBar: Dialog) {
        progressBar.dismiss()
    }
}
