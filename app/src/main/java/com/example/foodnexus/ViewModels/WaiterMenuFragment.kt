package com.example.foodnexus.ViewModels

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.WaiterMenuAdapter
import com.example.foodnexus.Models.WaiterMenuStructure
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentWaiterMenuBinding
import com.google.firebase.database.*

class WaiterMenuFragment : Fragment() {

    private var _binding: FragmentWaiterMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: SharedPreferences
    private lateinit var loadingDialog: Dialog
    private lateinit var adapter: WaiterMenuAdapter
    private val menuItems = ArrayList<WaiterMenuStructure>()

    private lateinit var rtdb: DatabaseReference

    private var userId: String = ""
    private var name: String = ""
    private var ownerId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWaiterMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeComponents()
        if (validateUserData()) {
            setupUI()
            loadMenuData()
        }
        setupClickListeners()
    }

    private fun initializeComponents() {
        rtdb = FirebaseDatabase.getInstance().reference
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        loadingDialog = createLoadingDialog()
    }

    private fun createLoadingDialog(): Dialog {
        return Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }
    }

    private fun validateUserData(): Boolean {
        userId = preferences.getString("userId", null).orEmpty()
        ownerId = preferences.getString("ownerId", null).orEmpty()

        if (userId.isEmpty() || ownerId.isEmpty()) {
            handleInvalidUser()
            return false
        }

        name = preferences.getString("name", "Name").toString()
        binding.RestaurantMenuTvName.text = name
        return true
    }

    private fun handleInvalidUser() {
        Utils.showToast(requireContext(), "Authentication required. Please login again.")
        findNavController().navigate(R.id.action_waiterMenuFragment_to_loginFragment)
    }

    private fun setupUI() {
        adapter = WaiterMenuAdapter(menuItems, requireContext(), userId, ownerId).apply {
            setHasStableIds(true)
        }

        binding.RestaurantMenuRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WaiterMenuFragment.adapter
            itemAnimator = null
        }
    }

    private fun loadMenuData() {
        if (!isAdded) return

        Utils.showProgress(loadingDialog)

        val menuRef = rtdb.child("Restaurants").child(ownerId).child("Menu")
        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                Utils.hideProgress(loadingDialog)
                processMenuData(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return

                Utils.hideProgress(loadingDialog)
                handleDataError(error.toException())
            }
        })
    }

    private fun processMenuData(snapshot: DataSnapshot) {
        menuItems.clear()
        for (menuItemSnapshot in snapshot.children) {
            val id = menuItemSnapshot.key ?: continue
            val name = menuItemSnapshot.child("Item Name").getValue(String::class.java) ?: "Unnamed Item"
            val recipe = menuItemSnapshot.child("Item Recipe").getValue(String::class.java) ?: "No description"
            val price = menuItemSnapshot.child("Item Price").getValue(Double::class.java) ?: 0.0

            menuItems.add(WaiterMenuStructure(id, name, recipe, price))
        }
        updateUIState()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUIState() {
        val isEmpty = menuItems.isEmpty()
        binding.RestaurantMenuRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.RestaurantMenuTvAddClasses.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isAdded) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun handleDataError(exception: Exception?) {
        val errorMessage = exception?.localizedMessage ?: "Unknown error occurred"
        Utils.showToast(requireContext(), "Menu loading failed: $errorMessage")
        updateUIState()
    }

    private fun setupClickListeners() {
        binding.RestaurantMenuImgBtnMenu.setOnClickListener {
            findNavController().navigate(R.id.action_waiterMenuFragment_to_waiterCartFragment)
        }
        binding.RestaurantMenuImgBtnReady.setOnClickListener{
            findNavController().navigate(R.id.action_waiterMenuFragment_to_waiterReadyOrder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
