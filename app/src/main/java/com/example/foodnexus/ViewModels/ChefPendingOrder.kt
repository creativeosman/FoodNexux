package com.example.foodnexus.ViewModels

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefOrderAdapter
import com.example.foodnexus.Models.ChefOrderStructure
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentChefPendingOrderBinding
import com.google.firebase.database.*

class ChefPendingOrder : Fragment() {

    private var _binding: FragmentChefPendingOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences
    private lateinit var ownerId: String
    private val rtdb = FirebaseDatabase.getInstance().reference

    private val ordersList = mutableListOf<ChefOrderStructure>()
    private lateinit var adapter: ChefOrderAdapter

    private var pendingOrdersListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefPendingOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) Load ownerId from SharedPreferences (written at login)
        prefs = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        ownerId = prefs.getString("ownerId", "") ?: ""

        // 2) Set up RecyclerView + adapter
        adapter = ChefOrderAdapter(
            orders = ordersList,
            onAccept = { orderId -> updateOrderStatus(orderId, "accepted") },
            onReject = { orderId -> updateOrderStatus(orderId, "declined") }
        )

        binding.rvChefOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefPendingOrder.adapter
        }

        // 3) Fetch “Pending Orders” from RTDB
        loadPendingOrders()
    }

    private fun loadPendingOrders() {
        val pendingRef = rtdb
            .child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")

        // Remove any previous listener
        pendingOrdersListener?.let { pendingRef.removeEventListener(it) }

        // Listen once for all children under /Pending Orders
        pendingOrdersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                for (orderSnap in snapshot.children) {
                    // Each order node key is the orderId
                    val orderId = orderSnap.key ?: continue

                    // Only show “pending” orders
                    val status = orderSnap.child("Status").getValue(String::class.java) ?: ""
                    if (status != "pending") {
                        continue
                    }

                    // Total Price is stored as String ("500.00", etc.)
                    val total = orderSnap.child("Total Price").getValue(String::class.java) ?: "0.00"

                    // “Items” is a map: itemId -> WaiterCartStructure
                    // We need to convert to List<ChefOrderStructure.Item>
                    val itemsList = mutableListOf<ChefOrderStructure.Item>()
                    val itemsSnap = orderSnap.child("Items")
                    for (itemEntry in itemsSnap.children) {
                        // itemEntry.key is the itemId
                        val waiterItem = itemEntry.getValue( // read as a data‐class–like map
                            com.example.foodnexus.Models.WaiterCartStructure::class.java
                        )

                        // If the conversion succeeded, pull out name & quantity
                        waiterItem?.let {
                            itemsList.add(
                                ChefOrderStructure.Item(
                                    name = it.itemName,
                                    quantity = it.quantity
                                )
                            )
                        }
                    }

                    // Build ChefOrderStructure and add to list
                    ordersList.add(
                        ChefOrderStructure(
                            id = orderId,
                            total = total,
                            items = itemsList
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        pendingRef.addValueEventListener(pendingOrdersListener as ValueEventListener)
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        val statusRef = rtdb
            .child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")
            .child(orderId)
            .child("Status")

        statusRef.setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order $orderId marked '$newStatus'.", Toast.LENGTH_SHORT).show()
                // Immediately refresh the list so “non-pending” orders disappear
                loadPendingOrders()
            }
            .addOnFailureListener { err ->
                Toast.makeText(requireContext(), "Could not update: ${err.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detach the listener
        val pendingRef = rtdb.child("Restaurants").child(ownerId).child("Pending Orders")
        pendingOrdersListener?.let { pendingRef.removeEventListener(it) }
        _binding = null
    }
}
