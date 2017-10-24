package com.ridi.inyoung.epub.view

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.ridi.inyoung.epub.R
import com.ridi.inyoung.epub.util.EpubParser

class BookshelfAdapter constructor(context: Context, val resource: Int, val data: List<EpubParser.Metadata>):
        ArrayAdapter<EpubParser.Metadata>(context, resource, data) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(resource, null)
        }

        view?.findViewById<TextView>(R.id.titleText)?.text = data[position].title
        view?.findViewById<TextView>(R.id.authorText)?.text = data[position].creator
        BitmapFactory.decodeFile(data[position].coverFile?.absolutePath)?.let {
            view?.findViewById<ImageView>(R.id.bookImage)?.setImageBitmap(it)
        }



        return view!!
    }
}