package com.bardino.dozi.core.ui.screens.login

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.bardino.dozi.R
import com.bardino.dozi.core.data.repository.UserRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repo = UserRepository()
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnSuccessListener {
                scope.launch {
                    repo.createUserIfNotExists()
                    onLoginSuccess()
                }
            }
        }
    }

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.width(220.dp).height(50.dp)
        ) {
            Text("Google ile Giriş Yap")
        }
    }
}
