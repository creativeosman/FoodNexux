package com.example.foodnexus.ViewModels

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentWaiterHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class WaiterHomeFragment : Fragment() {
    private var _binding: FragmentWaiterHomeBinding? = null
    private val binding get() = _binding!! // Non-null getter, safe after onCreateView
    private var activeFragment: Fragment? = null
    private val fragmentMap = mutableMapOf<String, Fragment>()
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        implementBottomNav()
    }

    private fun implementBottomNav() {
        bottomNav = binding.bottomNavigationView
        bottomNav.selectedItemId = R.id.ChefOrdersPending
        showFragment("MENU")
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.WaiterCart -> showFragment("CART")
                R.id.WaiterMenu -> showFragment("MENU")
                R.id.WaiterReady -> showFragment("READY")
                else -> false
            }
        }
    }

    private fun showFragment(tag: String): Boolean {
        val transaction = childFragmentManager.beginTransaction()
        activeFragment?.let { transaction.hide(it) }

        val fragment = fragmentMap.getOrPut(tag) {
            when (tag) {
                "CART" -> WaiterCartFragment()
                "MENU" -> WaiterMenuFragment()
                "READY" -> WaiterReadyOrder()
                else -> Fragment()
            }.also {
                transaction.add(R.id.child_fragment_container, it, tag)
            }
        }

        transaction.show(fragment).commit()
        activeFragment = fragment
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}