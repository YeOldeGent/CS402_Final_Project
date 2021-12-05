package com.example.simpletotp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simpletotp.ListAdapter
import com.example.simpletotp.R
import com.example.simpletotp.totp.SafeTOTPEntry
import com.example.simpletotp.totp.TOTPWrapper

class ListViewActivity : AppCompatActivity() {
    private lateinit var kRecyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        var counter = 0
        val itemList = arrayListOf<String>("Arabic", "Robusta","Sumatra","Kona")
        val scoffee = arrayListOf<Boolean>(false, false, false, false)
        //Creates the view
        kRecyclerView =
            findViewById(R.id.recyclerView) as RecyclerView
        kRecyclerView.layoutManager = LinearLayoutManager(this)

        //adapter goes here
        val kadapter: ListAdapter = ListAdapter(this, itemList, scoffee)

        kRecyclerView.setAdapter(kadapter)

        /**
         * Adds an item to the list and updates the
         * arrays and kadapter
         */
        fun addItem(newItem: String){
            val intent = Intent(this, QRActivity::class.java)
            startActivity(intent)
            Singleton.printEntries()
            counter += 1
            scoffee.add(false)
            itemList.add(newItem)
            kadapter.notifyDataSetChanged()
        }

        /**
         * Removes the items in both of the arrays and
         * updates the list
         */
        fun removeItem(index: Int){
            scoffee.removeAt(index)
            itemList.removeAt(index)
        }


        val addButton: Button = findViewById(R.id.addButton)
        // set on-click listener
        addButton.setOnClickListener {
            addItem("New item " + counter)
        }

//        val deleteButton: Button = findViewById(R.id.deleteButton)
//        deleteButton.setOnClickListener{
//            for (i in scoffee.size downTo 1){
//                if(scoffee[i-1]){
//                    removeItem(i-1)
//                }
//            }
//            kadapter.notifyDataSetChanged()
//        }
//
//
//        //splits the item's name directly in half
//        val splitButton: Button = findViewById(R.id.splitButton)
//        splitButton.setOnClickListener{
//            for (i in scoffee.size downTo 1){
//                if(scoffee[i-1]){
//                    if(itemList[i-1].length > 1) {
//                        addItem((itemList[i - 1].substring((itemList[i - 1].length) / 2)))
//                        itemList[i - 1] = itemList[i - 1].substring(0, (itemList[i - 1].length) / 2)
//                    }else{
//                        Toast.makeText(this@ListItself, "Cannot split items with one character", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//            kadapter.notifyDataSetChanged()
//        }

        //takes all of the selected items, puts them in one item, and deletes the others
//        val joinButton: Button = findViewById(R.id.joinButton)
//        joinButton.setOnClickListener{
//            var firstSelection: Int = -1
//            for (i in scoffee.size downTo 1){
//                if(scoffee[i-1]){
//                    if(firstSelection > -1){
//                        itemList[firstSelection] =  itemList[firstSelection] + " " + itemList[i-1]
//                        removeItem(i-1);
//                        firstSelection -= 1
//                    }else{
//                        firstSelection = i-1
//                    }
//                }
//            }
//            kadapter.notifyDataSetChanged()
//        }
    }
}
    //collapse any open ones when the
    //pencil to edit, trash to delete, star to favorite


