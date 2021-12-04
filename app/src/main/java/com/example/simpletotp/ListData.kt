package com.example.simpletotp

class ListData(val name: String, val isOnline: Boolean) {

    companion object {
        private var lastContactId = 0
        fun createContactsList(numContacts: Int): ArrayList<ListData> {
            val contacts = ArrayList<ListData>()
            for (i in 1..numContacts) {
                contacts.add(ListData("Person " + ++lastContactId, i <= numContacts / 2))
            }
            return contacts
        }
    }
}