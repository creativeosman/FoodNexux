package com.example.foodnexus.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)

        checkLoginStatus()

        return binding.root
    }

    private fun checkLoginStatus() {
        val isLogin = preferences.getBoolean("isLogin", false)
        val role = preferences.getString("role", "")

        if (isLogin) {
            navigateToRoleBasedScreen(role)
        } else {
            showAuthOptions()
        }
    }

    private fun navigateToRoleBasedScreen(role: String?) {
        Handler(Looper.getMainLooper()).postDelayed({
            when (role) {
                "Owner" -> findNavController().navigate(R.id.action_splashFragment_to_restaurantMenuFragment)
                "Waiter" -> findNavController().navigate(R.id.action_splashFragment_to_waiterMenuFragment)
                "Chef" -> findNavController().navigate(R.id.action_splashFragment_to_chefOrderReceivingFragment)
                else -> showAuthOptions() // Fallback if role is not recognized
            }
        }, 500)
    }

    private fun showAuthOptions() {
        binding.SplashFragmentBtnLogin.visibility = View.VISIBLE
        binding.SplashFragmentBtnSignUp.visibility = View.VISIBLE
        binding.SplashFragmentTvOr.visibility = View.VISIBLE

        binding.SplashFragmentBtnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }

        binding.SplashFragmentBtnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_roleAssignFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
