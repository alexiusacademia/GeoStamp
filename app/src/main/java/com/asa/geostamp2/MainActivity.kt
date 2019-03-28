package com.asa.geostamp2

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
import com.asa.geostamp2.databinding.ActivityMainBinding
import java.io.File
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
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
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.util.Log
import android.view.View
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.FileOutputStream
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val CAMERA_START = 0

    val REQUEST_IMAGE_CAPTURE = 1

    private val PERMISSION_REQUEST_CODE = 101

    private var mCurrentPhotoPath: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mLat: Double = 0.0
    private var mLong: Double = 0.0
    private var mTime: String = ""
    private var mTimeExif: String = ""

    private var mFinalImage: Bitmap? = null

    private var mLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.textWaiting.text = "Waiting for GPS data..."

        if (checkPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener {
                    if (it != null) {
                        this.mLat = it.latitude
                        this.mLong = it.longitude
                        mLocation = it
                    } else {
                        Log.d("Location message: ", "Cannot access location!")
                    }
                }

            binding.textWaiting.visibility = View.GONE
            binding.btnCapture.visibility = View.VISIBLE

        } else {
            requestPermission()
            if (checkPermission()) {
                binding.textWaiting.visibility = View.GONE
            }
        }

        binding.btnCapture.setOnClickListener {
            if (checkPermission()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener {
                        if (it != null) {
                            this.mLat = it.latitude
                            this.mLong = it.longitude
                            mLocation = it
                        } else {
                            Log.d("Location message: ", "Cannot access location!")
                        }
                    }

                takePicture()
            } else {
                requestPermission()
            }
        }

        binding.btnSave.setOnClickListener {
            if (checkPermission()) {
                saveFile()
            }
        }
    }

    /**
     * Checks if permissions has been granted.
     */
    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Request from the user for permission.
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(READ_EXTERNAL_STORAGE, CAMERA, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createFile()

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.asa.geostamp2.fileprovider",
            file
        )

        val date = Date()

        // Get the current time the photo was taken
        val sdf = SimpleDateFormat("MM/dd/yyyy - hh:mm:ss")
        val currDate = sdf.format(date)
        mTime = currDate.toString()

        val sdf2 = SimpleDateFormat("yyyy:MM:dd hh:mm:ss")
        val currDateExif = sdf2.format(date)
        mTimeExif = currDateExif.toString()

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            val bitmap: Bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)

            // Rotate image
            var matrix = Matrix()
            matrix.postRotate(90.0f)

            var rotatedBitmap: Bitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true
            )

            var newBitmap = drawText(
                this,
                rotatedBitmap
            )

            // Delete the original file saved by the camera
            val f = File(mCurrentPhotoPath)
            if (f.exists()) {
                f.delete()
            }

            // Display image
            // binding.imageView.setImageBitmap(newBitmap)
            // Create thumbnail
            var thumb = ThumbnailUtils.extractThumbnail(
                newBitmap,
                binding.imageView.width,
                binding.imageView.height
            )

            binding.imageView.setImageBitmap(thumb)

            if (binding.textWaiting.visibility == View.VISIBLE) {
                binding.textWaiting.visibility = View.GONE
            }
            if (binding.btnSave.visibility != View.VISIBLE) {
                binding.btnSave.visibility = View.VISIBLE
            }

        } else {
            Toast.makeText(this, "Capture cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    // takePicture()
                    binding.textWaiting.visibility = View.GONE
                    binding.btnCapture.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show()
                }
            }

            else -> {

            }
        }
    }

    @Throws(IOException::class)
    private fun createFile(): File {
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

    private fun drawText(context: Context, bitmap: Bitmap, textSize: Int = 28): Bitmap {
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

        val textLat = "Latitude: " + this.mLat.toString()
        val textLong = "Longitude: " + this.mLong.toString()
        val textTime = mTime

        textPaint.color = Color.rgb(255, 255, 255)
        bgPaint.color = Color.argb(50, 0, 0, 0)
        bgPaint.style = Paint.Style.FILL

        // text size in pixels
        textPaint.textSize = (textSize * scale).roundToInt().toFloat()

        // text shadow
        textPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        canvas.drawRect(0f,
            0f,
            canvas.width / 4f,
            canvas.height / 4.5f,
            bgPaint)

        // Save the canvas state, draw text then restore
        canvas.save()

        canvas.rotate(
            90.0f,
            0f,
            0f
        )

        // Draw the texts to be displayed
        canvas.drawText(
            textLat,
            50.0f,
            -150.0f,
            textPaint
        )

        canvas.drawText(
            textLong,
            50.0f,
            -250.0f,
            textPaint
        )

        canvas.drawText(
            textTime,
            50.0f,
            -350.0f,
            textPaint
        )

        canvas.restore()

        mFinalImage = newBitmap

        return newBitmap
    }

    private fun saveFile() {
        // Replace the image saved by the phone camera with the
        // stamped image

        if (mFinalImage != null) {
            try {
                // task is run on a thread
                Thread(Runnable {
                    // Display the indefinite progressbar
                    this@MainActivity.runOnUiThread {
                        binding.progressSaving.visibility = View.VISIBLE
                    }

                    // Operation
                    try {
                        val file = File(this.mCurrentPhotoPath)
                        val fOut = FileOutputStream(file)
                        mFinalImage?.compress(Bitmap.CompressFormat.JPEG, 100, fOut)

                        fOut.flush()
                        fOut.close()

                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    // Hide the progress bar after saving the file
                    this@MainActivity.runOnUiThread {
                        binding.progressSaving.visibility = View.GONE
                        Toast.makeText(this, "Image has been saved!", Toast.LENGTH_SHORT).show()
                        galleryAddPic()

                        // Tag the photo
                        tagImage()
                    }
                }).start()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(null, "Save file error!")
            }
        } else {
            Toast.makeText(this,
                "There is no available image to tag.\nPlease take another picture.",
                Toast.LENGTH_SHORT).show()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun decToDMS(coord: Double) : String {
        var c = coord
        if (c < 0) {
            c *= -1
        }

        var tempI = c.toInt()           // Hour
        var tempD = (c - tempI) * 60

        var sOut: String = tempI.toString() + "/1,"
        tempI = tempD.toInt()           // Minutes
        tempD -= tempI

        sOut += tempI.toString() + "/1,"

        tempI = (tempD * 60_000).toInt()

        sOut += tempI.toString() + "/1000"

        return sOut
    }

    // TODO: Edit exif

    // Make the photo available in gallery
    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(mCurrentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    private fun tagImage() {
        saveExifData(mCurrentPhotoPath.toString(),
            mLat,
            mLong,
            "syncster31\nalexius.academia@gmail.com",
            mTimeExif)
    }

    /**
     * Converts a latitude or longitude to the appropriate
     * string value that the GPS Exif will accept.
     */
    private fun gpsToDMS(data: Double): String {
        val degMinSec = Location.convert(data, Location.FORMAT_SECONDS).split(":")

        val degrees = degMinSec[0].toInt().absoluteValue
        val seconds = (degMinSec[2].toDouble() * 10000).roundToInt()
        return "$degrees/1,${degMinSec[1]}/1,$seconds/10000"
    }

    /**
     * Save the exif data to the image file
     */
    private fun saveExifData(mediaFilePath: String,
                             lat: Double,
                             lon: Double,
                             copyWrite: String,
                             timeExif: String) {
        val exif = ExifInterface(mediaFilePath)

        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, gpsToDMS(lat))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (lat < 0) "S" else "N")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, gpsToDMS(lon))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lon < 0) "W" else "E")
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, copyWrite)
        exif.setAttribute(ExifInterface.TAG_DATETIME, timeExif)

        exif.saveAttributes()
    }
}
