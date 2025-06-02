package com.example.foodnexus.ViewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefOrderAdapter
import com.example.foodnexus.Models.ChefOrderStructure
import com.example.foodnexus.databinding.FragmentChefPreparingOrderBinding
import com.google.firebase.database.*

class ChefPreparingOrder : Fragment() {
    private var _binding: FragmentChefPreparingOrderBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: ChefOrderAdapter
    private val orders = mutableListOf<ChefOrderStructure>()
    private lateinit var ownerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefPreparingOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        ownerId = requireContext().getSharedPreferences("Details", 0)
            .getString("ownerId", "").orEmpty()

        adapter = ChefOrderAdapter(
            orders,
            onAccept = { orderId -> changeStatus(orderId, "accepted") },
            onReject = { orderId -> changeStatus(orderId, "declined") }
        )

        binding.rvChefOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefPreparingOrder.adapter
        }
        fetchPendingOrders()
    }

    private fun fetchPendingOrders() {
        val ordersRef = database.child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")

        ordersRef.orderByChild("Status").equalTo("preparing")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()
                    for (orderSnapshot in snapshot.children) {
                        val id = orderSnapshot.key ?: continue
                        val total = orderSnapshot.child("totalPrice").getValue(String::class.java).orEmpty()

                        val itemsList = mutableListOf<ChefOrderStructure.Item>()
                        val itemsSnapshot = orderSnapshot.child("items")

                        for (itemSnapshot in itemsSnapshot.children) {
                            val name = itemSnapshot.child("itemName").getValue(String::class.java) ?: continue
                            val quantity = when (val q = itemSnapshot.child("quantity").value) {
                                is Long -> q.toInt()
                                is Int -> q
                                is Double -> q.toInt()
                                else -> 0
                            }
                            itemsList.add(ChefOrderStructure.Item(name, quantity))
                        }

                        orders.add(ChefOrderStructure(id, total, itemsList))
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun changeStatus(orderId: String, newStatus: String) {
        val orderRef = database.child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")
            .child(orderId)

        orderRef.child("Status").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order $newStatus", Toast.LENGTH_SHORT).show()
                fetchPendingOrders()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update order", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
