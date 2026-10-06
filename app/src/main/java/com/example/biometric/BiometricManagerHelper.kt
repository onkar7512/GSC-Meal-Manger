package com.example.biometric

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BiometricManagerHelper {
    private const val TAG = "BiometricHelper"

    /**
     * Checks if the device has biometric or fingerprint hardware available.
     */
    fun hasBiometricHardware(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                packageManager.hasSystemFeature("android.hardware.biometrics")
    }

    /**
     * Checks if biometrics can be authenticated on this device.
     */
    fun canAuthenticate(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            if (biometricManager != null) {
                val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                return canAuth == BiometricManager.BIOMETRIC_SUCCESS
            }
        }
        return hasBiometricHardware(context)
    }

    /**
     * Generates a standardized Fingerprint Identifier / Token for student enrollment.
     */
    fun generateFingerprintId(rollNo: String): String {
        val cleanRoll = rollNo.filter { it.isLetterOrDigit() }.ifBlank { "00" }
        return "FP-$cleanRoll"
    }

    /**
     * Formats current timestamp for fingerprint enrollment log.
     */
    fun getCurrentTimestampString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Triggers a subtle tactile haptic vibration for successful fingerprint match.
     */
    fun triggerSuccessVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration feedback error: ${e.message}")
        }
    }

    /**
     * Triggers a double-pulse vibration for errors or duplicate scans.
     */
    fun triggerErrorVibration(context: Context) {
        try {
            val pattern = longArrayOf(0, 70, 70, 70)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error vibration error: ${e.message}")
        }
    }

    /**
     * Initiates device system BiometricPrompt if API 28+ and hardware is ready.
     * Returns a CancellationSignal that can be used to cancel prompt.
     */
    fun authenticateWithSystemBiometrics(
        context: Context,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ): CancellationSignal? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric authentication requires Android 9+")
            return null
        }

        val cancellationSignal = CancellationSignal()
        try {
            val executor = context.mainExecutor
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButton("Manual Check-In", executor) { _, _ ->
                    onCancel()
                }
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        triggerSuccessVibration(context)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            onCancel()
                        } else {
                            onError(errString?.toString() ?: "Authentication error ($errorCode)")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        triggerErrorVibration(context)
                        onError("Fingerprint not recognized. Please try again.")
                    }
                }
            )
            return cancellationSignal
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch BiometricPrompt", e)
            onError(e.localizedMessage ?: "Failed to start biometric prompt")
            return null
        }
    }
}
