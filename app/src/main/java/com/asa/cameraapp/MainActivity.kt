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


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val CAMERA_START = 0

    val REQUEST_IMAGE_CAPTURE = 1

    private val PERMISSION_REQUEST_CODE = 101

    private var mCurrentPhotoPath: String? = null

    private var bitmapPreview: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.btnCapture.setOnClickListener {
            if (checkPermission()) {
                takePicture()
            } else {
                requestPermission()
            }
        }

        if (savedInstanceState != null) {
            binding.imageView.setImageBitmap(savedInstanceState?.getParcelable("bitmapPreview"))
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
            var matrix: Matrix = Matrix()
            matrix.postRotate(90.0f)

            var rotatedBitmap: Bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true)

            var newBitmap = drawText(this,
                rotatedBitmap,
                "Hello world!")

            // Display image
            // binding.imageView.setImageBitmap(bitmap)
            binding.imageView.setImageBitmap(newBitmap)

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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(255, 255, 255)

        // text size in pixels
        paint.textSize = (textSize * scale).roundToInt().toFloat()

        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // draw text to the Canvas center
        val bounds = Rect()

        //draw the first text
        paint.getTextBounds(text1, 0, text1.length, bounds)
        var x = (bitmap.width - bounds.width()) / 2f - 470
        var y = (bitmap.height + bounds.height()) / 2f - 140

        canvas.save()

        canvas.rotate(90.0f,
            //canvas.width / 2.0f,
            0f,
            //canvas.height / 2.0f
            0f)

        canvas.drawText(text1,
            50.0f,
            50.0f,
            paint)

        canvas.restore()
        this.bitmapPreview = newBitmap
        return newBitmap
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putParcelable("bitmapPreview", this.bitmapPreview)
    }
}
