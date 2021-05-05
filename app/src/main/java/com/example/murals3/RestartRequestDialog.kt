package com.example.murals3

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class RestartRequestDialog(val addMsg: String = "") : DialogFragment() {

    interface RestartRequestDialogListener {
        fun confirmButtonClicked()
        fun cancelButtonClicked()
    }

    private lateinit var listener: RestartRequestDialogListener

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            // Instantiate the ConfirmationListener so we can send events to the host
            listener = activity as RestartRequestDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(activity.toString() + " must implement ${RestartRequestDialog::class.java.simpleName}")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
                .setMessage("$addMsg${if (addMsg.isNotEmpty()) ". " else ""}Restart the tour?")
                .setPositiveButton("YES") { _, _ ->
                    listener.confirmButtonClicked()
                }
                .setNegativeButton("NO") { _, _ ->
                    listener.cancelButtonClicked()
                }
                .create()
    }
}