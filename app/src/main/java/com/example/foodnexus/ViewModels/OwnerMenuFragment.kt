package com.example.foodnexus.ViewModels

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.OwnerMenuAdapter
import com.example.foodnexus.Models.OwnerMenuStructure
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.caloryApi
import com.example.foodnexus.databinding.AddResturantMenuDialogBinding
import com.example.foodnexus.databinding.FragmentRestaurantMenuBinding
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class OwnerMenuFragment : Fragment() {
    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var caloryApiClient: caloryApi
    private lateinit var userId: String
    private lateinit var restaurantName: String
    private lateinit var arrayList: ArrayList<OwnerMenuStructure>
    private lateinit var adapter: OwnerMenuAdapter
    private lateinit var loadingDialog: Dialog
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
                R.id.copyId -> {
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
        database = FirebaseDatabase.getInstance().reference
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)

        userId = preferences.getString("userId", null).toString()
        restaurantName = preferences.getString("restaurantName", null).toString()
        binding.RestaurantMenuTvName.text = restaurantName

        arrayList = ArrayList()
        adapter = OwnerMenuAdapter(arrayList, requireContext(), userId)
        binding.RestaurantMenuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.RestaurantMenuRecyclerView.adapter = adapter

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.nutritionix.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        caloryApiClient = retrofit.create(caloryApi::class.java)

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
            val priceText = dialogBinding.DialogEtItemPrice.text.toString().trim()

            if (name.isEmpty() || recipe.isEmpty() || priceText.isEmpty()) {
                Utils.showToast(requireContext(), "Please fill out all fields.")
                return@setOnClickListener
            }

            val price = priceText.toDoubleOrNull() ?: 0.0
            if (price == 0.0) {
                Utils.showToast(requireContext(), "Enter a valid price.")
                return@setOnClickListener
            }

            dialog.dismiss()
            addItem(name, recipe, price)
        }

        dialog.show()
    }

    private fun addItem(name: String, recipe: String, price: Double) {
        Utils.showProgress(loadingDialog)
        lifecycleScope.launch {
            try {
                val calories = fetchCaloriesFromApi(name)
                val itemId = database.child("Restaurants").child(userId).child("Menu").push().key ?: UUID.randomUUID().toString()
                val itemData = mapOf(
                    "Item Name" to name,
                    "Item Recipe" to recipe,
                    "Item Price" to price,
                    "Calories" to calories,
                    "Time Stamp" to System.currentTimeMillis()
                )

                database.child("Restaurants")
                    .child(userId)
                    .child("Menu")
                    .child(itemId)
                    .setValue(itemData)
                    .addOnSuccessListener {
                        arrayList.add(OwnerMenuStructure(itemId, name, recipe, price, calories))
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
                Utils.showToast(requireContext(), "Error: ${e.message}")
                Utils.hideProgress(loadingDialog)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMenu() {
        Utils.showProgress(loadingDialog)

        database.child("Restaurants")
            .child(userId)
            .child("Menu")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    arrayList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val itemId = itemSnapshot.key ?: continue
                        val itemName = itemSnapshot.child("Item Name").getValue(String::class.java) ?: "Unknown"
                        val itemRecipe = itemSnapshot.child("Item Recipe").getValue(String::class.java) ?: "Unknown"
                        val itemPrice = itemSnapshot.child("Item Price").getValue(Double::class.java) ?: 0.0
                        val calories = itemSnapshot.child("Calories").getValue(Double::class.java)?.toFloat() ?: 0f

                        arrayList.add(
                            OwnerMenuStructure(
                                itemId = itemId,
                                itemName = itemName,
                                itemRecipe = itemRecipe,
                                itemPrice = itemPrice,
                                calories = calories
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                    Utils.hideProgress(loadingDialog)
                }

                override fun onCancelled(error: DatabaseError) {
                    Utils.showToast(requireContext(), "Failed to load menu: ${error.message}")
                    Utils.hideProgress(loadingDialog)
                }
            })
    }

    private fun updateEmptyState() {
        binding.RestaurantMenuTvAddClasses.text =
            if (arrayList.isEmpty()) "No items available" else ""
    }

    private suspend fun fetchCaloriesFromApi(foodName: String): Float {
        return try {
            val response = caloryApiClient.getFoodInfo(
                food = foodName,
                results = "0:1",
                appId = "d27f4ffd",
                appKey = "af6d42f114104a0bf3b546b48f98636c"
            )

            if (response.isSuccessful) {
                val calories = response.body()?.hits?.firstOrNull()?.fields?.nf_calories ?: 0f
                Log.d("API_RESPONSE", "Extracted calories: $calories")
                calories
            } else {
                Log.e("API_ERROR", "HTTP ${response.code()} – ${response.errorBody()?.string()}")
                0f
            }
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", e.toString())
            0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
