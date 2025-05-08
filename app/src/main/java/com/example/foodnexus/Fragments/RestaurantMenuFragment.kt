package com.example.foodnexus.Fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.OwnerMenuAdapter
import com.example.foodnexus.R
import com.example.foodnexus.Structures.OwnerMenuStructure
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentRestaurantMenuBinding
import com.example.foodnexus.databinding.AddResturantMenuDialogBinding
import com.google.firebase.firestore.FirebaseFirestore

class RestaurantMenuFragment : Fragment() {
    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var restaurantName: String
    private lateinit var arrayList: ArrayList<OwnerMenuStructure>
    private lateinit var adapter: OwnerMenuAdapter
    private lateinit var loadingDialog: Dialog
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        loadMenu()

        binding.RestaurantMenuImgBtnAdd.setOnClickListener {
            openAddItemDialog()
        }
        binding.RestaurantMenuImgBtnMenu.setOnClickListener {
            showPopupMenu(binding.RestaurantMenuImgBtnMenu)
        }

    }
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.resturant_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.RestaurantSales -> findNavController().navigate(R.id.action_restaurantMenuFragment_to_resturantsSalesFragment)
                R.id.copyId-> {

                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("User ID", userId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "User ID copied to clipboard", Toast.LENGTH_SHORT).show()


                }
            }
            true
        }
        popupMenu.show()
    }

    private fun init() {
        firestore = FirebaseFirestore.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)

        userId = preferences.getString("userId", null).toString()
        restaurantName=preferences.getString("restaurantName",null).toString()

        binding.RestaurantMenuTvName.setText(restaurantName)


        arrayList = ArrayList()
        adapter = OwnerMenuAdapter(arrayList, requireContext(), userId)

        binding.RestaurantMenuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.RestaurantMenuRecyclerView.adapter = adapter

        // Setup loading dialog
        loadingDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            setCancelable(false)
        }
    }

    private fun openAddItemDialog() {
        val dialogBinding = AddResturantMenuDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
        }

        dialogBinding.DialogBtnAdd.setOnClickListener {
            val name = dialogBinding.DialogEtItemName.text.toString().trim()
            val recipe = dialogBinding.DialogEtItemRecipe.text.toString().trim()
            val price = dialogBinding.DialogEtItemPrice.text.toString().trim()

            if (name.isEmpty() || recipe.isEmpty()||price.isEmpty()) {
                Utils.showToast(requireContext(), "Please fill out all fields.")
            } else {
                dialog.dismiss()
                addItem(name, recipe,price)
            }
        }

        dialog.show()
    }

    private fun addItem(name: String, recipe: String,price:String) {
        try {
            Utils.showProgress(loadingDialog)

            val itemRef = firestore.collection("Restaurants")
                .document(userId)
                .collection("Menu")
                .document()

            val itemData = hashMapOf(
                "Item Name" to name,
                "Item Recipe" to recipe,
                "Item Price" to price,
                "Time Stamp" to System.currentTimeMillis()
            )

            itemRef.set(itemData)
                .addOnSuccessListener {
                    arrayList.add(OwnerMenuStructure(itemRef.id, name, recipe,price))
                    adapter.notifyItemInserted(arrayList.lastIndex)
                    Utils.showToast(requireContext(), "Item added successfully.")
                    updateEmptyState()
                }
                .addOnFailureListener { e ->
                    Utils.showToast(requireContext(), "Failed to add item: ${e.localizedMessage}")
                }
                .addOnCompleteListener {
                    Utils.hideProgress(loadingDialog)
                }

        } catch (e: Exception) {
            Utils.showToast(requireContext(), "Error: ${e.localizedMessage}")
            Utils.hideProgress(loadingDialog)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMenu() {
        Utils.showProgress(loadingDialog)

        firestore.collection("Restaurants")
            .document(userId)
            .collection("Menu")
            .get()
            .addOnSuccessListener { documents ->
                arrayList.clear()
                for (doc in documents) {
                    val itemId = doc.id
                    val itemName = doc.getString("Item Name") ?: "Unknown"
                    val itemRecipe = doc.getString("Item Recipe") ?: "Unknown"
                    val itemPrice = doc.getString("Item Price") ?: "Unknown"
                    arrayList.add(OwnerMenuStructure(itemId, itemName, itemRecipe,itemPrice))
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener { e ->
                Utils.showToast(requireContext(), "Failed to load menu: ${e.localizedMessage}")
            }
            .addOnCompleteListener {
                Utils.hideProgress(loadingDialog)
            }
    }

    private fun updateEmptyState() {
        binding.RestaurantMenuTvAddClasses.text =
            if (arrayList.isEmpty()) "No items available" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
