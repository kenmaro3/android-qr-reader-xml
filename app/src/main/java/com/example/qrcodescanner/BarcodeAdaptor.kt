package com.example.qrcodescanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BarcodeAdapter(private val barcodeList: List<String>) : RecyclerView.Adapter<BarcodeAdapter.BarcodeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val barcode = barcodeList[position]
        holder.bind(barcode)
    }

    override fun getItemCount(): Int {
        return barcodeList.size
    }

    inner class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val barcodeTextView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(barcode: String) {
            barcodeTextView.text = barcode
            itemView.setOnClickListener {
                if (barcode.startsWith("http://") || barcode.startsWith("https://")) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode))
                    itemView.context.startActivity(browserIntent)
                }
            }
        }
    }
}
