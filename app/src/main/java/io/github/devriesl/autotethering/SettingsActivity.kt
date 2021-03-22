package io.github.devriesl.autotethering

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val sharedPrefs = this.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val keywordInput = findViewById<EditText>(R.id.keyword_text_input)
        keywordInput.hint = sharedPrefs.getString(KEYWORD_TEXT, getString(R.string.ethernet_tether_checkbox_text))
        keywordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                sharedPrefs.edit().putString(KEYWORD_TEXT, s.toString()).apply();
            }
        })
    }

    companion object {
        const val SHARED_PREFS_NAME = "auto_tethering_settings"
        const val KEYWORD_TEXT = "keyword_text"
    }
}