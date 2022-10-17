package it.alkona.rutoken.ui.web

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import it.alkona.rutoken.Constants
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.DialogFragmentPinBinding
import it.alkona.rutoken.databinding.FragmentRutokenWaitBinding
import it.alkona.rutoken.ui.logger

class RutokenWaitFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentRutokenWaitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRutokenWaitBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        logger("Убираем окно ожидания")
    }
}