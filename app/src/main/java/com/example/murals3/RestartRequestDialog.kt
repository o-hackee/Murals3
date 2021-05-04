package com.example.murals3

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class RestartRequestDialog : DialogFragment() {

    interface ConfirmationListener {
        fun confirmButtonClicked()
        fun cancelButtonClicked()
    }

    private lateinit var listener: ConfirmationListener

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            // Instantiate the ConfirmationListener so we can send events to the host
            listener = activity as ConfirmationListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement ConfirmationListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
                .setMessage("Restart the tour?")
                .setPositiveButton("YES") { _, _ ->
                    listener.confirmButtonClicked()
                }
                .setNegativeButton("NO") { _, _ ->
                    listener.cancelButtonClicked()
                }
                .create()
    }
}