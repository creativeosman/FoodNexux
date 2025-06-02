package com.example.foodnexus.ViewModels

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.WaiterCartAdapter
import com.example.foodnexus.Models.WaiterCartStructure
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentWaiterCartBinding
import com.google.firebase.database.*
import java.util.*

class WaiterCartFragment : Fragment() {
    private var _binding: FragmentWaiterCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var waiterAdapter: WaiterCartAdapter
    private val arrayList = mutableListOf<WaiterCartStructure>()
    private lateinit var preferences: SharedPreferences
    private lateinit var ownerId: String
    private lateinit var userId: String
    private val rtdb = FirebaseDatabase.getInstance().reference
    private var orderListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWaiterCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        userId = preferences.getString("userId", "") ?: ""
        ownerId = preferences.getString("ownerId", "") ?: ""

        setupRecyclerView()
        setupListeners()
        fetchCartItemsFromRTDB()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchCartItemsFromRTDB() {
        val cartRef = rtdb.child("Restaurants").child(ownerId).child("Staff").child(userId).child("Carts")

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                arrayList.clear()
                for (child in snapshot.children) {
                    val item = child.getValue(WaiterCartStructure::class.java)
                    item?.let { arrayList.add(it) }
                }
                waiterAdapter.notifyDataSetChanged()
                updatePlaceOrderText()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView() {
        waiterAdapter = WaiterCartAdapter(
            items = arrayList,
            onIncreaseClicked = { pos ->
                val item = arrayList[pos]
                item.quantity++
                updateCartItemInRTDB(item)
                waiterAdapter.notifyItemChanged(pos)
                updatePlaceOrderText()
            },
            onDecreaseClicked = { pos ->
                val item = arrayList[pos]
                if (item.quantity > 1) {
                    item.quantity--
                    updateCartItemInRTDB(item)
                    waiterAdapter.notifyItemChanged(pos)
                    updatePlaceOrderText()
                }
            }
        )

        binding.WaiterCartRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = waiterAdapter
        }
    }

    private fun updateCartItemInRTDB(item: WaiterCartStructure) {
        val itemRef = rtdb.child("Restaurants").child(ownerId).child("Staff").child(userId)
            .child("Carts").child(item.itemId)

        val updatedItem = item.copy(itemPrice = item.itemPrice / (item.quantity - 1) * item.quantity)
        itemRef.setValue(updatedItem).addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to update cart item", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.WaiterCartPlaceOrderButton.setOnClickListener { placeOrder() }

        binding.WaiterCartMenu.setOnClickListener { view ->
            PopupMenu(requireContext(), view).apply {
                menuInflater.inflate(R.menu.waiter_cart_menu, menu)
                setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.clearCart) {
                        clearCart()
                        true
                    } else false
                }
                show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlaceOrderText() {
        val total = arrayList.sumOf { it.itemPrice }
        binding.WaiterCartPlaceOrderButton.text = "Proceed with: %.2f".format(total)
    }

    private fun placeOrder() {
        if (arrayList.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val total = arrayList.sumOf { it.itemPrice }
        val orderId = rtdb.push().key ?: UUID.randomUUID().toString()
        val orderData = mapOf(
            "Waiter Id" to userId,
            "Items" to arrayList.associateBy { it.itemId },
            "Total Price" to "%.2f".format(total),
            "Status" to "pending",
            "Time stamp" to Date().toString()
        )

        val orderRef = rtdb.child("Restaurants").child(ownerId).child("Pending Orders").child(orderId)

        orderRef.setValue(orderData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Please Wait Order sent to chef", Toast.LENGTH_SHORT).show()
                listenOrderStatus(orderRef)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to place order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenOrderStatus(orderRef: DatabaseReference) {
        orderListener = orderRef.child("Status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: return
                when (status) {
                    "accepted" -> {
                        orderRef.child("Status").setValue("preparing")
                        clearCart()
                        Toast.makeText(requireContext(), "Order accepted, preparing now", Toast.LENGTH_SHORT).show()
                        orderRef.child("Status").removeEventListener(this)
                    }

                    "declined" -> {
                        Toast.makeText(requireContext(), "Order declined, you can modify and resend", Toast.LENGTH_SHORT).show()
                        orderRef.child("Status").removeEventListener(this)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearCart() {
        val cartRef = rtdb.child("Restaurants").child(ownerId).child("Staff").child(userId).child("Carts")

        cartRef.removeValue().addOnSuccessListener {
            arrayList.clear()
            waiterAdapter.notifyDataSetChanged()
            updatePlaceOrderText()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to clear cart", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orderListener?.let { listener ->
            rtdb.removeEventListener(listener)
        }
        _binding = null
    }
}
