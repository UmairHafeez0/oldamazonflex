package com.example.oldflex

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.oldflex.databinding.FragmentFilterBinding

class FilterFragment : Fragment() {
    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val PREF_NAME = "custom_block_prefs"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSavedData()

        // Toggle active/inactive
        binding.btnActive.setOnClickListener {
            binding.activeOptionsLayout.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Active Custom Block selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnInactive.setOnClickListener {
            binding.activeOptionsLayout.visibility = View.GONE
            clearActiveInputs()
            saveData("mode", "inactive")
            Toast.makeText(requireContext(), "Inactive Custom Block selected", Toast.LENGTH_SHORT).show()
        }

        // Save button
        binding.applyButton.setOnClickListener {
            saveInputs()
        }

        // Close
        binding.closeButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun saveInputs() {
        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString("mode", "active")
        editor.putString("station_code", binding.inputStationCode.text.toString())
        editor.putString("time", binding.inputTime.text.toString())
        editor.putString("hours", binding.inputHours.text.toString())
        editor.putString("price", binding.inputPrice.text.toString())
        editor.putString("date", binding.inputDate.text.toString())

        editor.apply()

        Toast.makeText(requireContext(), "Filters saved successfully!", Toast.LENGTH_SHORT).show()
    }
    private fun saveData(key: String, value: String) {
        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun loadSavedData() {
        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString("mode", "inactive")

        if (mode == "active") {
            binding.activeOptionsLayout.visibility = View.VISIBLE
            binding.inputStationCode.setText(prefs.getString("station_code", ""))
            binding.inputTime.setText(prefs.getString("time", ""))
            binding.inputHours.setText(prefs.getString("hours", ""))
            binding.inputPrice.setText(prefs.getString("price", ""))
            binding.inputDate.setText(prefs.getString("date", ""))
        } else {
            binding.activeOptionsLayout.visibility = View.GONE
        }
    }

    private fun clearActiveInputs() {
        binding.inputStationCode.setText("")
        binding.inputTime.setText("")
        binding.inputHours.setText("")
        binding.inputPrice.setText("")
        binding.inputDate.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
