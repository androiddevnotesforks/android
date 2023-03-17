package fusion.ai.features.signin

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import fusion.ai.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    var googleSignInClient: GoogleSignInClient

    private var googleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.server_client_id))
            .requestEmail()
            .build()

    init {
        googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
    }

    fun authWithFirebase(account: GoogleSignInAccount, onUpdate: (success: Boolean, id: String?, error: String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        Timber.d(idToken)
                        onUpdate(true, idToken, null)
                    }
                } else {
                    task.exception?.let {
                        Timber.e(it)
                        onUpdate(false, null, null)
                    }
                }
            }
            .addOnFailureListener {
                Timber.e(it)
                onUpdate(false, null, "Something went wrong! ${it.message}")
            }
    }
}
