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
import com.example.foodnexus.R
import com.example.foodnexus.Models.WaiterCartStructure
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentWaiterCartBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class WaiterCartFragment : Fragment() {
    private var _binding: FragmentWaiterCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var waiterAdapter: WaiterCartAdapter
    private val arrayList = mutableListOf<WaiterCartStructure>()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var preferences: SharedPreferences

    private lateinit var ownerId: String
    private lateinit var userId: String
    private var orderListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        userId = preferences.getString("userId", "") ?: ""
        ownerId = preferences.getString("ownerId", "") ?: ""
        setupRecyclerView()
        setupListeners()
        fetchCartItemsFromFirestore()
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_bar)
        setCancelable(false)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchCartItemsFromFirestore() {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Staff")
            .document(userId)
            .collection("Carts")
            .get()
            .addOnSuccessListener { documents ->
                arrayList.clear()
                for (doc in documents) {
                    val id = doc.getString("Item Id").orEmpty()
                    val name = doc.getString("Item Name").orEmpty()
                    val price = doc.getDouble("Item Price")?:0.0
                    val quantity = doc.getLong("Quantity")?.toInt() ?: 1
                    val recipe = doc.getString("Customize Recipe").orEmpty()
                    arrayList.add(WaiterCartStructure(id, name, price, quantity,recipe))
                }
                Utils.showToast(requireContext(),"done")
                waiterAdapter.notifyDataSetChanged()
                updatePlaceOrderText()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        waiterAdapter = WaiterCartAdapter(
            items = arrayList,
            onIncreaseClicked = { pos ->
                val item = arrayList[pos]
                item.quantity++
                updateCartItemInFirestore(item)
                waiterAdapter.notifyItemChanged(pos)
                updatePlaceOrderText()
            },
            onDecreaseClicked = { pos ->
                val item = arrayList[pos]
                if (item.quantity > 1) {
                    item.quantity--
                    updateCartItemInFirestore(item)
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

    private fun updateCartItemInFirestore(item: WaiterCartStructure) {
        val unitPrice = item.itemPrice
        val newTotal = unitPrice * item.quantity

        val map = mapOf(
            "Item Id" to item.itemId,
            "Item Name" to item.itemName,
            "Item Price" to newTotal,
            "Quantity" to item.quantity,
            "Customize Recipe" to item.itemCustomizeRecipe
        )

        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Staff")
            .document(userId)
            .collection("Carts")
            .document(item.itemId)
            .set(map)
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update cart item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.WaiterCartPlaceOrderButton.setOnClickListener {
            placeOrder()
        }

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
        val orderData = mapOf(
            "Waiter Id" to userId,
            "Items" to arrayList.map { item ->
                mapOf(
                    "Item Id" to item.itemId,
                    "Item Name" to item.itemName,
                    "Quantity" to item.quantity,
                    "Item Price" to item.itemPrice,
                    "Customize Recipe" to item.itemCustomizeRecipe
                )
            },
            "Total Price" to "%.2f".format(total),
            "Status" to "pending",
            "Time stamp" to Date()
        )

        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .add(orderData)
            .addOnSuccessListener { docRef ->
                Toast.makeText(requireContext(), "Please Wait Order sent to chef", Toast.LENGTH_SHORT).show()
                listenOrderStatus(docRef.id)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to place order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenOrderStatus(orderId: String) {
        val orderDoc = firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .document(orderId)

        orderListener = orderDoc.addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@EventListener
            val status = snapshot.getString("Status").orEmpty()
            when (status) {
                "accepted" -> {
                    // move to preparing and clear cart
                    orderDoc.update("Status", "preparing")
                    clearCart()
                    orderListener?.remove()
                    Toast.makeText(requireContext(), "Order accepted, preparing now", Toast.LENGTH_SHORT).show()
                }
                "declined" -> {
                    orderListener?.remove()
                    Toast.makeText(requireContext(), "Order declined, you can modify and resend", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearCart() {
        // clear cart subcollection
        val cartRef = firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Staff")
            .document(userId)
            .collection("Carts")

        cartRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) doc.reference.delete()
            arrayList.clear()
            waiterAdapter.notifyDataSetChanged()
            updatePlaceOrderText()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to clear cart", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orderListener?.remove()
        _binding = null
    }
}


