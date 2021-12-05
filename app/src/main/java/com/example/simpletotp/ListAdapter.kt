package com.example.simpletotp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView



public class ListAdapter(context: Context, var coffee: ArrayList<String>, var scoffee: ArrayList<Boolean> )
    : RecyclerView.Adapter<ListAdapter.KoffeeHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : ListAdapter.KoffeeHolder {
        val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false)
        return KoffeeHolder(view)
    }

    override fun getItemCount() = coffee.size

    override fun onBindViewHolder(holder: KoffeeHolder, position: Int) {
        val acoffee = coffee[position]
        holder.apply {
            titleTextView.text = acoffee
            var sscolor = "#ffffff"
            if (scoffee[position]) {
                sscolor = "#cccccc"
            }
            titleTextView.setBackgroundColor(Color.parseColor(sscolor))
        }
    }

//    class KoffeeHolder(view: View) : RecyclerView.ViewHolder(view) {
//
//        val titleTextView: TextView = view.findViewById(R.id.item_name)
//    }

    inner class KoffeeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {
        val mostParentView: View = view
        val firstChild: View = view.findViewById(R.id.list_item)
        val titleTextView: TextView = firstChild.findViewById(R.id.contact_name)
        var kSelect: Boolean = false

        // add init into the class object
        init {
            //The next line is the og provided line that didn't work
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            //toggle selection
            var apos = bindingAdapterPosition
            kSelect = scoffee[apos]
            kSelect = !kSelect
            scoffee[apos] = kSelect

            var sscolor = "#ffffff"
            if (kSelect) {
                sscolor = "#cccccc"
            }
            titleTextView.setBackgroundColor(Color.parseColor(sscolor))
        }
    }
}