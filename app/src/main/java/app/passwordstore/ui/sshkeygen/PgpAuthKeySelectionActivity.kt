/*
 * Copyright © 2014-2026 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.passwordstore.ui.sshkeygen

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import app.passwordstore.R
import app.passwordstore.util.extensions.snackbar
import app.passwordstore.util.git.sshj.SshKey
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import logcat.LogPriority.ERROR
import logcat.asLog
import logcat.logcat
import javax.inject.Inject
import app.passwordstore.crypto.PGPKeyManager
import app.passwordstore.crypto.KeyUtils
import app.passwordstore.crypto.PGPIdentifier

class PgpAuthKeySelectionActivity : AppCompatActivity() {

   @Inject lateinit var pgpKeyManager: PGPKeyManager
 
  private val pgpKeySelectionAction =
    registerForActivityResult(StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        val data = result.data ?: return@registerForActivityResult
        val keyId = 
          data.getStringExtra(PGPKeyListActivity.EXTRA_SELECTED_KEY)?.let {
		    PGPIdentifier.fromString(it)
		  } ?: return@registerForActivityResult

            
      }

      if (uri == null) {
        finish()
        return@registerForActivityResult
      }
      runCatching {
          SshKey.import(uri)
          Toast.makeText(
              this,
              resources.getString(R.string.ssh_key_success_dialog_title),
              Toast.LENGTH_LONG,
            )
            .show()
          setResult(RESULT_OK)
          finish()
        }
        .onFailure { e ->
          MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.ssh_key_error_dialog_title))
            .setMessage(e.message)
            .setPositiveButton(resources.getString(R.string.dialog_ok)) { _, _ -> finish() }
            .show()
        }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (SshKey.exists) {
      MaterialAlertDialogBuilder(this).run {
        setTitle(R.string.ssh_keygen_existing_title)
        setMessage(R.string.ssh_keygen_replace_with_pgp_key_message)
        setPositiveButton(R.string.ssh_keygen_replace_with_pgp_key) { _, _ -> selectPgpKey() }
        setNegativeButton(R.string.ssh_keygen_existing_keep) { _, _ -> finish() }
        setOnCancelListener { finish() }
        show()
      }
    } else {
      selectPgpKey()
    }
  }

  private fun selectPgpKey() {
    runCatching { pgpKeySelectionAction.launch(PGPKeyListActivity.newIntent(this, keySelection = true, singleSelection = true)) }
      .onFailure { e ->
        logcat(ERROR) { e.asLog() }
        e.message?.let { message -> snackbar(message = message) }
      }
  }
}
