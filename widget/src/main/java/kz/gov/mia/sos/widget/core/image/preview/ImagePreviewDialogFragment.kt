package kz.gov.mia.sos.widget.core.image.preview

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.core.image.load.dispose
import kz.gov.mia.sos.widget.core.image.load.load

internal class ImagePreviewDialogFragment : DialogFragment() {

    companion object {
        private val TAG = simpleNameOf<ImagePreviewDialogFragment>()

        fun newInstance(caption: String?, uri: Uri): ImagePreviewDialogFragment {
            val fragment = ImagePreviewDialogFragment()
            fragment.arguments = bundleOf("caption" to caption, "uri" to uri.toString())
            return fragment
        }

        fun show(fragmentManager: FragmentManager, caption: String?, uri: Uri) {
            fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(android.R.id.content, newInstance(caption, uri))
                .addToBackStack(null)
                .commit()
        }
    }

    private var imageView: ShapeableImageView? = null
    private var closeButton: MaterialButton? = null
    private var textView: MaterialTextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.sos_widget_view_image_preview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        closeButton = view.findViewById(R.id.closeButton)
        textView = view.findViewById(R.id.textView)

        val caption = arguments?.getString("caption")
        val uri = Uri.parse(arguments?.getString("uri"))

        imageView?.load(uri)

        textView?.text = caption

        closeButton?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        imageView?.dispose()
        imageView = null

        closeButton?.setOnClickListener(null)
        closeButton = null

        textView = null

        super.onDestroyView()
    }

}