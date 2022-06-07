package kz.gov.mia.sos.widget.ui.presentation.call.map

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kz.garage.recyclerview.adapter.viewholder.view.bind
import kz.garage.view.inflater.inflate
import kz.gov.mia.sos.widget.R

internal class MarkersAdapter constructor(
    private val callback: (marker: Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var markers: List<Long> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = markers.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ViewHolder(parent.inflate(R.layout.sos_widget_cell_marker))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(markers[position])
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView by bind<MaterialTextView>(R.id.labelView)
        private val valueView by bind<MaterialTextView>(R.id.valueView)

        fun bind(marker: Long) {
            labelView.setText(R.string.sos_widget_arm_force_number)

            @SuppressLint("SetTextI18n")
            valueView.text = "№ $marker"

            itemView.setOnClickListener { callback(marker) }
        }

    }

}