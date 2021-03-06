package com.alexiusacademia.geotagstamp

import android.Manifest.permission.*
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.alexiusacademia.geotagstamp.databinding.ActivityMainBinding
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.support.v4.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.media.ExifInterface
import android.media.ThumbnailUtils
import android.nfc.Tag
import android.opengl.Visibility
import android.os.Build
import android.os.Environment.getExternalStoragePublicDirectory
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.io.*
import java.text.DateFormat.getDateTimeInstance
import kotlin.math.absoluteValue


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding

    val REQUEST_IMAGE_CAPTURE = 1

    private val PERMISSION_REQUEST_CODE = 101

    private var mCurrentPhotoPath: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mLat: Double = 0.0
    private var mLong: Double = 0.0
    private var mTime: String = ""
    private var mTimeExif: String = ""

    private var mFinalImage: Bitmap? = null

    private var mDisplayTime: Boolean = false
    private var mStampLocation: String = ""
    private var mStampLocationsArray = mutableListOf<String>()
    private var mCustomText = mutableListOf<String>()
    private var mTextSize = 28
    private var mCustomTextSize = 40
    private var mEnableCustomContent: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the default preferences
        PreferenceManager.setDefaultValues(this,
            R.xml.preferences_layout, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Define the fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener {
                    if (it != null) {
                        mLat = it.latitude
                        mLong = it.longitude
                        binding.btnCapture.visibility = View.VISIBLE
                        binding.textWaiting.visibility = View.INVISIBLE
                    } else {
                        Log.d("Location Error", "Location null")
                        binding.btnCapture.visibility = View.INVISIBLE
                        binding.textWaiting.visibility = View.VISIBLE
                    }
                }
        } else {
            requestPermission()
        }

        // Create data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Sets the text waiting message while the phone
        // is waiting for the GPS data
        binding.textWaiting.text = "Waiting for GPS data..."

        binding.btnCapture.setOnClickListener {
            if (checkPermission()) {
                /*fusedLocationClient.lastLocation
                    .addOnSuccessListener {
                        if (it != null) {
                            this.mLat = it.latitude
                            this.mLong = it.longitude
                        } else {
                            Log.d("Location message: ", "Cannot access location!")
                        }
                    }
                */
                binding.imageView.setImageBitmap(null)

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

        binding.btnSettings.setOnClickListener {
            openSettingsActivity()
        }

        // Retrieve the string array resource
        val stampLocationsArray = resources.getStringArray(R.array.stamp_locations)
        for (i in stampLocationsArray.indices) {
            mStampLocationsArray.add(stampLocationsArray[i])
        }
    }

    override fun onLocationChanged(location: Location?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResume() {
        super.onResume()

        // Retrieve the preferences values
        mDisplayTime = sharedPreferences.getBoolean("pref_datetime", false)
        mStampLocation = sharedPreferences.getString("pref_stamp_location", "Lower Left").toString()

        val customText = sharedPreferences.getString("pref_custom_text", "")

        // Clear the array first
        mCustomText.clear()

        for (line in customText.split("\n")) {
            mCustomText.add(line)
        }

        mCustomTextSize = sharedPreferences.getString("pref_custom_text_size", "40").toInt()

        mTextSize = sharedPreferences.getString("pref_text_size", "28").toInt()

        mEnableCustomContent = sharedPreferences.getBoolean("pref_enable_custom_text", false)
    }

    /**
     * Checks if permissions has been granted.
     */
    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, CAMERA)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                )
    }

    /**
     * Request from the user for permission.
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(READ_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE,
                CAMERA,
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createFile()

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.alexiusacademia.geotagstamp.fileprovider",
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

        // Before starting the camera, make sure to empty the mFinalImage
        mFinalImage = null

        // Remove the content of the image view
        binding.imageView.setImageBitmap(null)

        // Save the image to the specified path
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

        // Start the default camera
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            val bitmap: Bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)

            // Rotate image
            val matrix = Matrix()
            matrix.postRotate(90.0f)

            val rotatedBitmap: Bitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true
            )

            val newBitmap = drawText(
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
            val thumb = ThumbnailUtils.extractThumbnail(
                newBitmap,
                binding.imageView.width,
                binding.imageView.height
            )

            binding.imageView.setImageBitmap(thumb)
            binding.imageView.scaleType = ImageView.ScaleType.FIT_XY

            if (binding.textWaiting.visibility == View.VISIBLE) {
                binding.textWaiting.visibility = View.GONE
            }
            if (binding.btnSave.visibility != View.VISIBLE) {
                binding.btnSave.visibility = View.VISIBLE
            }

            if (checkPermission()) {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    mLat = it.latitude
                    mLong = it.longitude
                }
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

    private fun drawText(context: Context, bitmap: Bitmap): Bitmap {
        val resources = context.resources
        val scale = resources.displayMetrics.density

        var bitmapConfig = bitmap.config

        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888
        }

        // Resource bitmaps are immutable,
        // so we need to copy to a mutable one.
        var newBitmap = bitmap.copy(bitmapConfig, true)

        // Creates new canvas
        val canvas = Canvas(newBitmap)

        // Create paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaintCustom = Paint(Paint.ANTI_ALIAS_FLAG)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.style = Paint.Style.FILL

        val textLat = "Latitude: " + this.mLat.toString()
        val textLong = "Longitude: " + this.mLong.toString()
        val textTime = mTime

        // Vertical padding of texts
        val padding = 25f

        // Rectangular height required by the location and time stamp
        var rectHeight = (mTextSize * scale * 2) + (padding * 2) + (padding * 2)

        // Rectangular height required by the custom text
        var rectHeightCustomText = 2 * padding
        for (line in mCustomText) {
            rectHeightCustomText += mCustomTextSize * scale + padding
        }

        // Text array to be displayed in aligned with the location stamp
        val textsArray = mutableListOf<String>()
        textsArray.add(textLat)
        textsArray.add(textLong)

        if (mDisplayTime) {
            textsArray.add(textTime)

            // Adjust rectangle height
            rectHeight += mTextSize * scale + padding
        }

        // Colors
        textPaint.color = Color.rgb(255, 255, 255)
        textPaintCustom.color = Color.rgb(255, 255, 255)
        bgPaint.color = Color.argb(50, 0, 0, 0)
        bgPaint.style = Paint.Style.FILL

        // text size in pixels
        textPaint.textSize = (mTextSize * scale).roundToInt().toFloat()
        textPaintCustom.textSize = (mCustomTextSize * scale).roundToInt().toFloat()

        // Font family
        val typeFaceCustom = Typeface.create("Times New Roman", Typeface.ITALIC)
        textPaintCustom.typeface = typeFaceCustom

        // text shadow
        textPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // Responsible for placing the text and rectangle
        // based on the selected location
        var stampVerticalLocationFactor = 0f
        var stampHorizontalLocationFactor = 0f

        val textLineHeight = mTextSize * scale + padding

        when (mStampLocation) {
            mStampLocationsArray[0] ->
                // Lower Left
                {
                    stampVerticalLocationFactor = 0f
                    stampHorizontalLocationFactor = 0f
                }
            mStampLocationsArray[1] ->
                // Upper Left
                {
                    stampVerticalLocationFactor = canvas.width - (textsArray.size * textLineHeight + 2 * padding)
                    stampHorizontalLocationFactor = 0f
                }
        }

        // Use the larger of the two rectheight
        val bgHeight: Float
        bgHeight = if (rectHeight > rectHeightCustomText) {
            rectHeight
        } else {
            rectHeightCustomText
        }

        canvas.drawRect(0f + stampVerticalLocationFactor,
            0f,
            bgHeight + stampVerticalLocationFactor,
            canvas.height.toFloat(),
            bgPaint)

        // Save the canvas state, draw text then restore
        canvas.save()

        canvas.rotate(
            90.0f,
            0f,
            0f
        )

        for (i in textsArray.indices) {
            if (i == 0) {
                canvas.drawText(
                    textsArray[i],
                    50f + stampHorizontalLocationFactor,
                    -1 * padding - stampVerticalLocationFactor,
                    textPaint
                )
            } else {
                canvas.drawText(
                    textsArray[i],
                    50f + stampHorizontalLocationFactor,
                    -1 * i * (textLineHeight + padding) - stampVerticalLocationFactor,
                    textPaint
                )
            }
        }

        if (mEnableCustomContent) {
            // Draw the custom text
            var j = 0
            for (i in mCustomText.size-1 downTo 0) {
                if (j == 0) {
                    canvas.drawText(
                        mCustomText[i],
                        canvas.height / 3f,
                        -1 * padding - stampVerticalLocationFactor,
                        textPaintCustom
                    )
                } else {
                    canvas.drawText(
                        mCustomText[i],
                        canvas.height / 3f,
                        -1 * j * (textPaintCustom.textSize + padding) - stampVerticalLocationFactor,
                        textPaintCustom
                    )
                }

                j += 1
            }
        }

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
                        binding.progressSaving.visibility = View.INVISIBLE
                        Toast.makeText(this,
                            "Image has been saved to the Gallery.\nDCIM/GeoTagStamp",
                            Toast.LENGTH_LONG).show()

                        val f = File(mCurrentPhotoPath)
                        if (f.exists()) {
                            moveImageFile()
                        } else {
                            Log.d("File Not Found", mCurrentPhotoPath + " not found!")
                        }
                    }
                }).start()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(null, "Save file error!")
            }

        } else {
            Toast.makeText(this,
                "There is no available image to tag yet.\nPlease take a picture.",
                Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Move the file from private directory to DCIM in the phone storage.
     */
    private fun moveImageFile() {
        val inputStream = FileInputStream(mCurrentPhotoPath)

        val filename = mCurrentPhotoPath!!
            .split("/")[mCurrentPhotoPath?.split("/")!!.size - 1]

        val outputPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/GeoTagStamp/"


        val dir = File(outputPath)
        if (!dir.exists()) {
            if (dir.mkdirs()) {

            } else {
                Log.d("Create Dir", "Error creating directory!")
            }
        }

        val buffer =  ByteArray(1024)
        var read : Int

        try {
            val outputStream = FileOutputStream(outputPath + filename)

            while (true) {
                read = inputStream.read(buffer)

                if (read <= 0) {
                    break
                }

                outputStream.write(buffer, 0, read)
            }

            outputStream.close()

            // Delete the original file
            val originalFile = File(mCurrentPhotoPath)
            originalFile.delete()

            // Tag the photo
            // Might show a few seconds of delay when the photo taken is opened
            // immediately
            tagImage(outputPath + filename)

        } catch (e: SecurityException) {
            Log.d("Error Me", e.message)
        } catch (e: FileNotFoundException) {
            Log.d("Error Me", e.message)
        } catch (e: java.lang.Exception) {
            Log.d("Error Me", e.message)
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI() else showSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    // Make the photo available in gallery
    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(mCurrentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    /**
     * Calling the exif tagging
     */
    private fun tagImage(filename: String) {
        saveExifData(filename,
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
     * Check the availability of the external storage
     * for writing.
     */
    private fun isExternalStorageWritable() : Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, copyWrite)
        }
        exif.setAttribute(ExifInterface.TAG_DATETIME, timeExif)

        exif.saveAttributes()
    }

    /**
     * Open the settings screen.
     */
    private fun openSettingsActivity() {
        // Clear generated image bitmap
        mFinalImage = null
        binding.imageView.setImageBitmap(null)

        // Open the settings screen
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    /*
    TODO Resolve the DialogRedirect: Failed to start resolution intent when running on lower api devices
     */
}
