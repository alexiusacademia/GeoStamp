package com.asa.geostamp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.asa.geostamp.databinding.ActivityMainBinding
import java.io.File
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.support.v4.content.FileProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.location.Location
import android.media.ThumbnailUtils
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val CAMERA_START = 0

    val REQUEST_IMAGE_CAPTURE = 1

    private val PERMISSION_REQUEST_CODE = 101

    private var mCurrentPhotoPath: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                if (it != null) {
                    Log.d("Latitude", it.latitude.toString())
                    Log.d("Longitude", it.longitude.toString())
                } else {
                    Log.d("Location message: ", "Cannot access location!")
                }
            }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.btnCapture.setOnClickListener {
            if (checkPermission()) {
                takePicture()
            } else {
                requestPermission()
            }
        }
    }

    /**
     * Checks if permissions has been granted.
     */
    private fun checkPermission() : Boolean {
        return (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Request from the user for permission.
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
            PERMISSION_REQUEST_CODE)
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createFile()

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.asa.geostamp.fileprovider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            // val auxFile = File(mCurrentPhotoPath)
            val bitmap: Bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)

            // Rotate image
            var matrix = Matrix()
            matrix.postRotate(90.0f)

            var rotatedBitmap: Bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true)

            var newBitmap = drawText(this,
                rotatedBitmap,
                "Hello world!")

            // Display image
            // binding.imageView.setImageBitmap(newBitmap)
            // Create thumbnail
            var thumb = ThumbnailUtils.extractThumbnail(newBitmap,
                binding.imageView.width,
                binding.imageView.height)
            binding.imageView.setImageBitmap(thumb)
            Log.d("Full size: ", (newBitmap.allocationByteCount / 1_000_000).toString())
            Log.d("Thumb size: ", (thumb.allocationByteCount / 1_000_000).toString())

        } else {
            Toast.makeText(this, "Not good!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show()
                }
            }

            else -> {

            }
        }
    }

    @Throws(IOException::class)
    private fun createFile() : File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* Prefix */
            ".jpg", /* Suffix/Extension */
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEWS intents
            mCurrentPhotoPath = absolutePath
        }
    }

    private fun drawText(context: Context, bitmap: Bitmap, text1: String, textSize: Int = 24) : Bitmap {
        val resources = context.resources
        val scale = resources.displayMetrics.density

        var bitmapConfig = bitmap.config;
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        var newBitmap = bitmap.copy(bitmapConfig, true)

        val canvas = Canvas(newBitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.style = Paint.Style.FILL

        textPaint.color = Color.rgb(255, 255, 255)
        bgPaint.color = Color.rgb(0, 0, 0)

        // text size in pixels
        textPaint.textSize = (textSize * scale).roundToInt().toFloat()

        // text shadow
        textPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // Save the canvas state, draw text then restore
        canvas.save()

        canvas.rotate(90.0f,
            //canvas.width / 2.0f,
            0f,
            //canvas.height / 2.0f
            0f)

        canvas.drawText(text1,
            50.0f,
            -150.0f,
            textPaint)

        canvas.restore()

        // Replace the image saved by the phone camera with the
        // stamped image
        try {
            val file = File(this.mCurrentPhotoPath)
            val fOut = FileOutputStream(file)
            newBitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(null, "Save file error!")
        }


        return newBitmap
    }
}
