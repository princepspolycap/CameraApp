package com.example.stevenprincepscodingsesh

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

    //show image in full quality
// include camera permissions
// handling the user permanently denying the camera and or storage permission
// should show permissions rational if it was true/false afterwards, take the user to the app setting to
//steven@squiresolutions.com
    private var deniedList: List<String> = emptyList()
    private var arePermissionsAccepted: Boolean = false

    private lateinit var currentImageUri: Uri
    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AppCompatButton?>(R.id.checkPermissionsButtonView).apply {
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23) {
                    this.rootView.findViewById<ImageView?>(R.id.showCapturedImageView).isClickable = false
                    checkUserPermissions()
                }
            }
        }

        val startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    //  you will get result here in result.data
                    Log.v(
                        "get picture taken in ${MainActivity::javaClass}",
                        result.data.toString()
                    )

                    val pictureTakenBitmap = BitmapFactory.decodeStream(
                        applicationContext?.contentResolver?.openInputStream(
                            currentImageUri
                        )
                    )
                    findViewById<ImageView?>(R.id.showCapturedImageView).setImageBitmap(
                        pictureTakenBitmap
                    )
                }

            }



        findViewById<ImageView?>(R.id.showCapturedImageView).setOnClickListener {

            if (arePermissionsAccepted) {
                takePictureUsingCamera(startForResult)
            } else {
                findViewById<ImageView?>(R.id.showCapturedImageView).run {
                    Snackbar.make(
                        this.rootView,
                        "Accept Permissions to Continue",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun takePictureUsingCamera(startForResult: ActivityResultLauncher<Intent>) {

        val photoURI: Uri = createImageFile().let { imageFile ->
            FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.provider",
                imageFile
            )
        }
        photoURI.let {
            currentImageUri = it
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, it)
            startForResult.runCatching { launch(cameraIntent)}.apply {
                if(this.isFailure){
                    notifyUserPermissionsNecessity()
                }
            }
        }
    }


    private fun askUserPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissionsNeeded: MutableList<String> = ArrayList()
            val permissionsList: MutableList<String> = ArrayList()


            if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissionsNeeded.add("Read Storage")
            }

            if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionsNeeded.add("Write Storage")
            }

            if (!addPermission(permissionsList, Manifest.permission.CAMERA)) {
                permissionsNeeded.add("Camera")
            }

            if (permissionsList.size > 0) {
                requestPermissions(
                    permissionsList.toTypedArray(),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                )
                return
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: MutableMap<String, Boolean> ->
            deniedList = result.filter { !it.value }.map { it.key }
        }
    }
    private fun addPermission(permissionsList: MutableList<String>, permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= 23) if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission)
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission)) return false
        }
        return true
    }

    private fun checkUserPermissions() {
        askUserPermissions()

        if (Build.VERSION.SDK_INT >= 23) {
            when {
                deniedList.isNotEmpty() -> {
                    val permissionDenied = null
                    val sendUserToSettings = null
                    val map = deniedList.groupBy { permission ->
                        if (shouldShowRequestPermissionRationale(permission)) permissionDenied else sendUserToSettings
                    }

                    map[permissionDenied]?.let {
                        // request denied , request again
                        askUserPermissions()
                    }
                    map[sendUserToSettings]?.let {
                        //request denied ,send to settings
                        notifyUserPermissionsNecessity()
                    }

                    findViewById<ImageView?>(R.id.showCapturedImageView).isClickable = false
                    arePermissionsAccepted = false

                }
                deniedList.isEmpty() -> {
                    //All request are permitted
                    Snackbar.make(
                        this.window.decorView.rootView,
                        this.getString(R.string.thank_you),
                        Snackbar.LENGTH_LONG
                    ).show()

                    findViewById<AppCompatButton?>(R.id.checkPermissionsButtonView).visibility =
                        View.GONE
                    arePermissionsAccepted = true
                    findViewById<ImageView?>(R.id.showCapturedImageView).isClickable = true
                }
            }
        }
    }
    private fun notifyUserPermissionsNecessity() {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        builder.setTitle(getString(R.string.neccessary_persmissions))
            .setMessage(getString(R.string.need_permissions_desc))
            .setPositiveButton("YES") { dialog, _ ->
                dialog.dismiss()
                openAppSystemSettings()
            }
            .setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }
            .setIcon(R.drawable.ic_launcher_foreground)
            .show()
    }

    private fun openAppSystemSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File? =
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_TEMP_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentImageUri = Uri.parse(absoluteFile.absolutePath)
        }
    }
}