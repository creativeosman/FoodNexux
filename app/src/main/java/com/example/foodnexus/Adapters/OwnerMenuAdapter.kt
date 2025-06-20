package com.example.foodnexus.Adapters

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Models.OwnerMenuStructure
import com.example.foodnexus.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase

class OwnerMenuAdapter(
    private var arrayList: ArrayList<OwnerMenuStructure>,
    private var context: Context,
    private var userId: String
) : RecyclerView.Adapter<OwnerMenuAdapter.ViewHolder>() {

    private val dbRef = FirebaseDatabase.getInstance().reference

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.TvItemName)
        val itemRecipe: TextView = itemView.findViewById(R.id.TvRecipe)
        val itemPrice: TextView = itemView.findViewById(R.id.TvPrice)
        val itemMenu: ImageButton = itemView.findViewById(R.id.OwnerMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.owner_menu_recycler_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = arrayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = arrayList[position]

        holder.itemName.text = itemData.itemName
        holder.itemRecipe.text = itemData.itemRecipe
        holder.itemPrice.text = itemData.itemPrice.toString()

        holder.itemMenu.setOnClickListener {
            showPopupMenu(holder.itemMenu, itemData, position)
        }
    }

    private fun showPopupMenu(view: View, itemData: OwnerMenuStructure, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.opt_delete, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.DeleteRecord -> confirmDelete(itemData, position)
                R.id.UpdateRecord -> showUpdateDialog(itemData, position)
            }
            true
        }
        popupMenu.show()
    }

    private fun confirmDelete(itemData: OwnerMenuStructure, position: Int) {
        AlertDialog.Builder(context).apply {
            setTitle("Delete Item")
            setMessage("Are you sure you want to delete this Item?")
            setPositiveButton("Delete") { _, _ ->
                Toast.makeText(context, "Deleting...", Toast.LENGTH_SHORT).show()
                dbRef.child("Restaurants").child(userId).child("Menu").child(itemData.itemId)
                    .removeValue()
                    .addOnSuccessListener {
                        arrayList.removeAt(position)
                        notifyItemRemoved(position)
                        Toast.makeText(context, "Item Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showUpdateDialog(itemData: OwnerMenuStructure, position: Int) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.add_resturant_menu_dialog)
        val itemNameEditText = dialog.findViewById<EditText>(R.id.DialogEtItemName)
        val itemRecipeEditText = dialog.findViewById<EditText>(R.id.DialogEtItemRecipe)
        val itemPriceEditText = dialog.findViewById<EditText>(R.id.DialogEtItemPrice)
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.DialogBtnAdd)

        itemNameEditText.setText(itemData.itemName)
        itemRecipeEditText.setText(itemData.itemRecipe)
        itemPriceEditText.setText(itemData.itemPrice.toString())
        btnUpdate.text = "Update"

        btnUpdate.setOnClickListener {
            val newItemName = itemNameEditText.text.toString().trim()
            val newItemRecipe = itemRecipeEditText.text.toString().trim()
            val newItemPriceText = itemPriceEditText.text.toString().trim()

            if (newItemName.isEmpty() || newItemRecipe.isEmpty() || newItemPriceText.isEmpty()) {
                Toast.makeText(context, "Please enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newItemPrice = newItemPriceText.toDoubleOrNull()
            if (newItemPrice == null || newItemPrice == 0.0) {
                Toast.makeText(context, "Enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(context, "Updating...", Toast.LENGTH_SHORT).show()

            val updatedData = mapOf(
                "itemId" to itemData.itemId,
                "itemName" to newItemName,
                "itemRecipe" to newItemRecipe,
                "itemPrice" to newItemPrice
            )

            dbRef.child("Restaurants").child(userId).child("Menu").child(itemData.itemId)
                .updateChildren(updatedData)
                .addOnSuccessListener {
                    arrayList[position] = OwnerMenuStructure(itemData.itemId, newItemName, newItemRecipe, newItemPrice)
                    notifyItemChanged(position)
                    Toast.makeText(context, "Item Updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update item", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }
}
