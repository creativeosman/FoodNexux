// ChefPreparingOrder.kt
package com.example.foodnexus.ViewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefPreparingOrderAdapter
import com.example.foodnexus.Adapters.WaiterReadyAdapter
import com.example.foodnexus.Models.ChefOrderPreparingStructure
import com.example.foodnexus.Models.WaiterReadyStructure
import com.example.foodnexus.databinding.FragmentChefPreparingOrderBinding
import com.example.foodnexus.databinding.FragmentWaiterReadyOrderBinding
import com.google.firebase.database.*

class WaiterReadyOrder : Fragment() {
    private var _binding: FragmentWaiterReadyOrderBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: WaiterReadyAdapter
    private val orders = mutableListOf<WaiterReadyStructure>()
    private lateinit var ownerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterReadyOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ownerId = requireContext()
            .getSharedPreferences("Details", 0)
            .getString("ownerId", "")
            .orEmpty()

        adapter = WaiterReadyAdapter(
            orders,
            onAccept = { orderId -> changeStatus(orderId, "Completed") }
        )

        binding.rvWaiterReady.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WaiterReadyOrder.adapter
        }

        fetchPreparingOrders()
    }

    private fun fetchPreparingOrders() {
        val ordersRef = database
            .child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")

        // Only pull orders whose Status == "preparing"
        ordersRef.orderByChild("Status").equalTo("prepared")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()

                    for (orderSnapshot in snapshot.children) {
                        val id = orderSnapshot.key ?: continue

                        // ← use the exact key "Total Price" (capital T, space, capital P)
                        val total = orderSnapshot
                            .child("Total Price")
                            .getValue(String::class.java)
                            .orEmpty()

                        orders.add(WaiterReadyStructure(id, total))
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun changeStatus(orderId: String, newStatus: String) {
        val orderRef = database
            .child("Restaurants")
            .child(ownerId)
            .child("Pending Orders")
            .child(orderId)

        orderRef.child("Status").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order marked $newStatus", Toast.LENGTH_SHORT)
                    .show()
                fetchPreparingOrders()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update order", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
