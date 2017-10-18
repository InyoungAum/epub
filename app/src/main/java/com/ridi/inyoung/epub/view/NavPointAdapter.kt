package com.ridi.inyoung.epub.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.ridi.inyoung.epub.R
import com.ridi.inyoung.epub.model.EpubNavPoint

class NavPointAdapter constructor(context: Context, val resource: Int, val data: List<EpubNavPoint>):
        ArrayAdapter<EpubNavPoint>(context, resource, data) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(resource, null)
        }

        view?.findViewById<TextView>(R.id.titleText)?.text = data[position].label

        return view!!
    }
}