/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package fusion.ai.billing

import android.util.Base64
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device. For the sake of simplicity and
 * clarity of this example, this code is included here and is executed on the device. If you must
 * verify the purchases on the phone, you should obfuscate this code to make it harder for an
 * attacker to replace the code with stubs that treat all purchases as verified.
 */
object Security {

    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Verifies that the data was signed with the given signature
     *
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */
    fun verifyPurchase(
        base64PublicKey: String?,
        signedData: String?,
        signature: String?
    ) = if (signedData.isNullOrBlank() ||
        base64PublicKey.isNullOrBlank() ||
        signature.isNullOrBlank()
    ) {
        BuildConfig.DEBUG
        // Line modified (https://stackoverflow.com/questions/14600664/android-in-app-purchase-signature-verification-failed). Was: return false.
    } else {
        verify(generatePublicKey(base64PublicKey), signedData, signature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     * @throws NoSuchAlgorithmException if encoding algorithm is not supported or key specification is invalid
     */
    @Throws(IllegalArgumentException::class, NoSuchAlgorithmException::class)
    fun generatePublicKey(encodedPublicKey: String?): PublicKey = try {
        KeyFactory.getInstance(KEY_FACTORY_ALGORITHM).generatePublic(
            X509EncodedKeySpec(Base64.decode(encodedPublicKey, Base64.DEFAULT))
        )
    } catch (e: InvalidKeySpecException) {
        throw IllegalArgumentException(e)
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     *
     * @return true if the data and signature match
     */
    private fun verify(
        publicKey: PublicKey?,
        signedData: String,
        signature: String?
    ): Boolean {
        val signatureBytes = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            return false
        }

        try {
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(publicKey)
                update(signedData.toByteArray())

                if (!verify(signatureBytes)) {
                    return false
                }
            }

            return true
        } catch (_: Exception) {
        }

        return false
    }
}
