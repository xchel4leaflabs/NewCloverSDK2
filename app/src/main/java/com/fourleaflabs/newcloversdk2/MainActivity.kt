package com.fourleaflabs.newcloversdk2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clover.sdk.gosdk.GoSdk
import com.clover.sdk.gosdk.GoSdkConfiguration
import com.clover.sdk.gosdk.GoSdkCreator
import com.clover.sdk.gosdk.model.PayRequest
import com.clover.sdk.gosdk.payment.domain.model.CardReaderStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val mTag = "XchelTag"
    private val permissionTag = "XchelTag"

    private val goSdk: GoSdk by lazy { GoSDKCreator.get(this) }

    /*
    private var requiredPermissions: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        else -> {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
            )
        }
    }
    */

    private val requiredPermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        else -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // clover code
        ActivityCompat.requestPermissions(
            this,
            requiredPermissions,
            REQUEST_PERMISSION_CODE
        )
        lifecycleScope.launch {
            //scanReaders()
            //receiveCardReaderStatus()
            //sendPayRequest()
        }

        // Own kiosk code
        //val permissionList = checkPermissions()
        //val permission = permissionList.toTypedArray()
        //if (!hasPermissions(this, permission)) {
        //    showDialog(permission)
        //}

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val permissionsList = ArrayList<String>()
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty()) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionsList.add(permissions[i])
                }
            }

            if (permissionsList.isEmpty()) {
                permissionsGranted()
            } else {
                var showRationale = false

                for (permission in permissionsList) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        showRationale = true
                        break
                    }
                }

                if (showRationale) {
                    showAlertDialog(
                        { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                permissionsList.toTypedArray(),
                                REQUEST_PERMISSION_CODE
                            )
                        },
                        { _, _ ->
                            permissionsDenied()
                        }
                    )
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun permissionsDenied() {
        Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show()
    }

    private fun permissionsGranted() {
        Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            goSdk.scanForReaders().collectLatest { reader ->
                if (reader.bluetoothName.equals("Clover_Go_630094")) { // this is my clover go name
                    Log.d(mTag, reader.toString())
                    goSdk.connect(reader)
                }
            }
        }

        lifecycleScope.launch {
            goSdk.observeCardReaderStatus().collectLatest {
                println(it)
                Log.d(mTag, it.javaClass.name)
                CardReaderStatus.Connecting

                if (it is CardReaderStatus.Ready) {
                    val request = PayRequest(
                        final = true, //true for Sales, false for Auth or PreAuth Transactions
                        capture = true, //true for Sales, true for Auth, false for PreAuth Transactions
                        amount = 100L,
                        taxAmount = 0L,
                        tipAmount = 0L,
                        externalPaymentId = null
                    )
                    goSdk.chargeCardReader(request).collectLatest { chargeState ->
                        println(chargeState)
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 100
    }


    /// kiosk code for request permissions
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun showDialog(permission: Array<String>) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Permission Required")
            .setMessage("The following permissions must be accepted for the correct operation of the App. \n\n•Nearby Devices \n•Location")
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                ActivityCompat.requestPermissions(this@MainActivity, permission, 12)
            }
            .setOnDismissListener {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    permission,
                    REQUEST_PERMISSION_CODE
                )
            }
            .create()
            .show()
    }

    @SuppressLint("LogNotTimber")
    private fun checkPermissions(): MutableList<String> {
        val permissionList = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        Log.d(permissionTag, permissionList.joinToString(", "))
        return permissionList
    }

    private suspend fun sendPayRequest() {
        val request = PayRequest(
            final = true, //true for Sales, false for Auth or PreAuth Transactions
            // capture = null, //null for Sales, true for Auth, false for PreAuth Transactions
            amount = 1000L,
            taxAmount = 0L,
            tipAmount = 100L,
            externalPaymentId = null
        )

        goSdk.chargeCardReader(request).collectLatest { status ->
            //We suggest logging or updating the UI with the transaction status so you follow the transaction steps
            //and to know if the device is waiting for a user action (i.e. if the device is waiting for a card to be inserted or tapped)
            Timber.d("PayRequest: $status")
            Log.d(mTag, "PayRequest: $status")
        }
    }

    private suspend fun receiveCardReaderStatus() {
        goSdk.observeCardReaderStatus().collectLatest { status ->
            Timber.d("CardReaderStatus: $status")
            Log.d(mTag, "CardReaderStatus: $status")
        }

    }

    private suspend fun scanReaders() {
        goSdk.scanForReaders().collectLatest { reader ->
            Timber.d("Readers: $reader")
            Log.d(mTag, "Readers: $reader")
        }
    }

    private fun showAlertDialog(
        okListener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setMessage("Some permissions are not granted. Application may not work as expected. Do you want to grant them?")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", cancelListener)
            .create()
            .show()
    }

    /// kiosk code for request permissions
}

object GoSDKCreator {

    private val env = if (BuildConfig.DEBUG) GoSdkConfiguration.Environment.SANDBOX else GoSdkConfiguration.Environment.PROD

    private fun buildConfig(context: Context) = GoSdkConfiguration.Builder(
        context = context,
        appId = BuildConfig.APP_ID,
        appVersion = BuildConfig.VERSION_NAME,
        apiKey = BuildConfig.API_KEY,
        apiSecret = BuildConfig.API_SECRET,
        oAuthFlowAppSecret = BuildConfig.OAUTH,
        //oAuthFlowRedirectURI = "https://sandbox.dev.clover.com/oauth/authorize?client_id=${BuildConfig.APP_ID}&response_type=code",
        oAuthFlowRedirectURI = "https://sandbox.dev.clover.com",
        oAuthFlowAppID = BuildConfig.APP_ID,
        environment = env,
        reconnectLastConnectedReader = true
    )
        .enableLogging(true)
        .allowDuplicates(false)
        .build()

    private lateinit var goSdkInstance: GoSdk

    @JvmStatic
    fun get(context: Context): GoSdk {
        if (!GoSDKCreator::goSdkInstance.isInitialized) {
            goSdkInstance = GoSdkCreator.create(buildConfig(context))
        }
        return goSdkInstance
    }
}