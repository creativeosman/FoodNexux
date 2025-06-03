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
import com.example.foodnexus.Adapters.ChefReadyOrderAdapter
import com.example.foodnexus.Models.ChefOrderPreparingStructure
import com.example.foodnexus.Models.ChefOrderReadyStructure
import com.example.foodnexus.databinding.FragmentChefPreparingOrderBinding
import com.example.foodnexus.databinding.FragmentChefReadyOrderBinding
import com.google.firebase.database.*

class ChefReadyOrder : Fragment() {
    private var _binding: FragmentChefReadyOrderBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: ChefReadyOrderAdapter
    private val orders = mutableListOf<ChefOrderReadyStructure>()
    private lateinit var ownerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefReadyOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ownerId = requireContext()
            .getSharedPreferences("Details", 0)
            .getString("ownerId", "")
            .orEmpty()

        adapter = ChefReadyOrderAdapter(
            orders
        )

        binding.rvChefOrderPreparing.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefReadyOrder.adapter
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

                        orders.add(ChefOrderReadyStructure(id, total))
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
