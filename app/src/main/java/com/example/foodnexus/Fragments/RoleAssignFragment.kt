package com.example.foodnexus.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentRoleAssignBinding

class RoleAssignFragment : Fragment() {
    private lateinit var binding: FragmentRoleAssignBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentRoleAssignBinding.inflate(layoutInflater)

        binding.RoleAssignFragmentBtnOwner.setOnClickListener {
            findNavController().navigate(R.id.action_roleAssignFragment_to_ownerSignUpFragment)
        }
        binding.RoleAssignFragmentBtnStaff.setOnClickListener {
            findNavController().navigate(R.id.action_roleAssignFragment_to_staffSignUpFragment)
        }

        return binding.root
    }


}