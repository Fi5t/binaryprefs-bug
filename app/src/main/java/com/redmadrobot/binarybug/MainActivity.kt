package com.redmadrobot.binarybug

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.crypto.tink.aead.AeadFactory
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.ironz.binaryprefs.BinaryPreferencesBuilder
import java.security.GeneralSecurityException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val KEYSET_NAME = "master_keyset"
        private const val PREFERENCE_FILE = "master_key_preference"
        private const val MASTER_KEY_URI = "android-keystore://master_key"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            TinkConfig.register()
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(this, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        val aead = AeadFactory.getPrimitive(keysetHandle)

        val bpInstance1 = BinaryPreferencesBuilder(this)
            .name("main_file")
            .valueEncryption(TinkValueEncryption(aead, "good_data".toByteArray()))
            .build()

        val bpInstance2 = BinaryPreferencesBuilder(this)
            .name("main_file")
            .valueEncryption(TinkValueEncryption(aead, "bad_data".toByteArray()))
            .build()

        bpInstance1.edit {
            putString("my_key", "bug")
        }

        val result = bpInstance2.getString("my_key", "no bug")

        Log.e("[REPRODUCE]", result)

    }
}
