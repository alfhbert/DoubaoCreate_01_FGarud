package com.safeguard.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.safeguard.app.databinding.ActivityPairBinding
import com.safeguard.app.util.Prefs
import java.security.SecureRandom
import java.util.Locale

class PairActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairBinding
    private var generatedCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.pair_title)

        if (Prefs.isPaired) {
            binding.textPairCode.text = Prefs.pairCode
            binding.textPairHint.text = getString(R.string.pair_current_hint, Prefs.pairCode)
            binding.btnUnpairPair.visibility = View.VISIBLE
        }

        binding.btnCreate.setOnClickListener {
            generatedCode = String.format(Locale.US, "%06d", SecureRandom().nextInt(1000000))
            binding.textPairCode.text = generatedCode
            binding.textPairHint.text = getString(R.string.pair_created_hint)
            binding.btnStartPair.visibility = View.VISIBLE
        }

        binding.btnStartPair.setOnClickListener {
            val code = generatedCode
            if (code.isNullOrBlank()) return@setOnClickListener
            Prefs.pairCode = code
            StatusHub.remoteStatus = null
            SafeguardService.start(this)
            finish()
        }

        binding.btnJoin.setOnClickListener {
            val code = binding.editCode.text?.toString()?.trim().orEmpty()
            if (!Regex("^\\d{6}$").matches(code)) {
                Toast.makeText(this, R.string.pair_code_bad, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Prefs.pairCode = code
            StatusHub.remoteStatus = null
            SafeguardService.start(this)
            finish()
        }

        binding.btnUnpairPair.setOnClickListener {
            Prefs.pairCode = ""
            StatusHub.remoteStatus = null
            SafeguardService.stop(this)
            Toast.makeText(this, R.string.pair_unpaired_hint, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
