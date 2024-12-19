package com.nilhcem.blenamebadge.ui.message

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.nilhcem.blenamebadge.Metronome
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.core.android.ext.hideKeyboard
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.android.viewbinding.bindView
import com.nilhcem.blenamebadge.device.model.DataToSend
import com.nilhcem.blenamebadge.device.model.Message
import com.nilhcem.blenamebadge.device.model.Mode
import com.nilhcem.blenamebadge.device.model.Speed
import java.io.File
import java.lang.Float.max
import java.lang.Float.min
import kotlin.concurrent.thread
import kotlinx.coroutines.*

class MessageActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSION_LOCATION = 1
    }

    private val content: EditText by bindView(R.id.text_to_send)
    private val flash: CheckBox by bindView(R.id.flash)
    private val marquee: CheckBox by bindView(R.id.marquee)
    private val speed: Spinner by bindView(R.id.speed)
    private val mode: Spinner by bindView(R.id.mode)
    private val songTitle: Spinner by bindView(R.id.songtitle)
    private val wait: Spinner by bindView(R.id.waiting)
    private val clear: Button by bindView(R.id.send_button_ALL)
    private val sendBF: Button by bindView(R.id.send_button_BF)
    private val send89: Button by bindView(R.id.send_button_89)
    private val send3B: Button by bindView(R.id.send_button_3B)
    private val sendCD: Button by bindView(R.id.send_button_CD)
    private val prevBT: Button by bindView(R.id.prev_button)
    private val playBT: Button by bindView(R.id.play_button)
    private val pauseBT: Button by bindView(R.id.pause_button)
    private val nextBT: Button by bindView(R.id.next_button)
    private val fadeLBT: Button by bindView(R.id.fade_left)
    private val fadeRBT: Button by bindView(R.id.fade_right)
    private val stopBT: Button by bindView(R.id.stop_button)
    private val resetBT: Button by bindView(R.id.reset_button)
    private val console: TextView by bindView(R.id.console)
    private val rbtBF: RadioButton by bindView(R.id.bpm_BF)
    private val rbt89: RadioButton by bindView(R.id.bpm_89)
    private val rbt3B: RadioButton by bindView(R.id.bpm_3B)
    private val rbtCD: RadioButton by bindView(R.id.bpm_CD)

    private val presenter by lazy { MessagePresenter() }
    private lateinit var clipboardManager : ClipboardManager

    private var lastEdit: Long = java.time.Instant.now().toEpochMilli()
    private val songs: ArrayList<File> = ArrayList()
    private var mediaPlayer: MediaPlayer? = null
    private var index = 0
    private var l:Float = 1.0F
    private var r:Float = 1.0F
    private val addresses = mutableListOf("38:3B:26:EC:64:BF","38:3B:26:EC:64:89","38:3B:26:EC:64:3B","38:3B:26:EC:64:CD")
    private val applicationContext: Activity = this

    private val REQUEST_PERMISSION_CODE = 1001

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
        } else {
            // Permissions already granted
            accessDownloadsFolder()
        }
    }

    private fun accessDownloadsFolder() {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsFolder != null && downloadsFolder.exists()) {
            val files = downloadsFolder.listFiles()
            songs.addAll(files)
        } else {
            // Downloads folder not found
            // Handle accordingly
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_activity)

        val spinnerItem = android.R.layout.simple_spinner_dropdown_item
        mode.adapter = ArrayAdapter<String>(this, spinnerItem, Mode.values().map { getString(it.stringResId) })
        speed.adapter = ArrayAdapter<String>(this, spinnerItem, Speed.values().mapIndexed { index, _ -> (index + 1).toString() })
        speed.setSelection(7)//speed8
        wait.adapter = ArrayAdapter<Long>(this, spinnerItem, arrayOf(0L, 10L, 20L, 30L, 40L, 50L, 100L))
        wait.setSelection(0)//sleep
        content.setText("")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        rbtBF.tag = addresses[0]
        rbt89.tag = addresses[1]
        rbt3B.tag = addresses[2]
        rbtCD.tag = addresses[3]
        sendBF.tag = rbtBF
        send89.tag = rbt89
        send3B.tag = rbt3B
        sendCD.tag = rbtCD

        checkPermissions()
        if (songs.isNotEmpty()) {
            songTitle.setSelection(index)

            songTitle.adapter = ArrayAdapter<String>(this, spinnerItem, songs.map { it.nameWithoutExtension  })
            songTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    index = position
                    songTitle.setSelection(index)
                }
            }
        }

        clear.setOnClickListener {
            sendBF.isEnabled = true
            send89.isEnabled = true
            send3B.isEnabled = true
            sendCD.isEnabled = true
            content.setText("")
            Metronome.stop()
        }

        val commonCLickListener = View.OnClickListener { view ->
            if (content.text.isEmpty()) {
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            var textToSend = content.text.trim().toString()
            val rb = view.tag as RadioButton
            if (!rb.isChecked)
            {//trim bpm
                textToSend = textToSend.split("_").last()
            }
            else
            {//metronome
                Metronome.stop()
                if (textToSend.contains("_"))
                {
                    Metronome.start(this, textToSend.split("_").first().toLong(), clear)
                }
            }
            presenter.sendSingleMessage(this, convertToDeviceDataModel(textToSend), wait.selectedItem as Long, view as Button)
        }

        sendBF.setOnClickListener(commonCLickListener)
        send89.setOnClickListener(commonCLickListener)
        send3B.setOnClickListener(commonCLickListener)
        sendCD.setOnClickListener(commonCLickListener)

        playBT.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, Uri.fromFile(songs[index]))
                setVolume(l, r)
                prepare()
                start()
            }
        }
        pauseBT.setOnClickListener {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying)
                {
                    mediaPlayer!!.pause()
                }
                else
                {
                    mediaPlayer!!.start()
                }
            }
        }
        stopBT.setOnClickListener {
            val mp = mediaPlayer
            if (mp != null) {
                mp.setVolume(l, r)
                while ( r > 0 || l > 0)
                {
                    l -= 0.05F
                    r -= 0.05F
                    l = max(l, 0.0F)
                    r = max(r,0.0F)
                    mp.setVolume(l, r)
                    Thread.sleep(250)
                }
                mp.stop()
                // after stopping the mediaplayer instance
                // it is again need to be prepared
                // for the next instance of playback
                mp.prepare()
                l = 1.0F
                r = 1.0F
                mp.setVolume(l, r)
            }
        }

        prevBT.setOnClickListener {
            if (index > 0)
            {
                index -= 1
            }
            if (songs.isNotEmpty()) {
                songTitle.setSelection(index)
            }
        }
        nextBT.setOnClickListener {
            if (index < songs.count()-1)
            {
                index += 1
            }
            if (songs.isNotEmpty()) {
                songTitle.setSelection(index)
            }
        }

        fadeLBT.setOnClickListener {
            val mp = mediaPlayer
            if (mp != null) {
                mp.setVolume(l, r)
                while ( r > 0 && l <= 1)
                {
                    l += 0.05F
                    r -= 0.05F
                    l = min(l, 1.0F)
                    r = max(r,0.0F)
                    mp.setVolume(l, r)
                    Thread.sleep(250)
                }
            }
        }
        fadeRBT.setOnClickListener {
            val mp = mediaPlayer
            if (mp != null) {

                mp.setVolume(l, r)
                while (l > 0 && r <= 1)
                {
                    l -= 0.05F
                    r += 0.05F
                    l = max(l,0.0F)
                    r = min(r, 1.0F)
                    mp.setVolume(l, r)
                    Thread.sleep(250)
                }
            }
        }

        resetBT.setOnClickListener {
            l = 1.0F
            r = 1.0F
            if (mediaPlayer != null) {
                mediaPlayer!!.setVolume(l, r)
            }
        }

        content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastEdit = java.time.Instant.now().toEpochMilli()
            }
        })

        // launch new coroutine in background and continue
        GlobalScope.launch {
            mainCoroutine()
        }
    }

    private suspend fun mainCoroutine() = coroutineScope {
        withContext(Dispatchers.IO) {
            val delayAmount = 2000L
            val partialDelay = 100L
            while(true){//with a while(true) loop inside
                val epochNow: Long = java.time.Instant.now().toEpochMilli()
                val timeDiff = epochNow - lastEdit
                //if text is not empty and not edited recently
                if (content.text.isNotEmpty() && timeDiff > 1000) {
                    //try to send text do bt device #1 and wait it to end
                    val bt1 = launch {
                        println("enter 1")
                        val dataToSend: DataToSend = if (rbtBF.isChecked) convertToDeviceDataModel(content.text.trim().toString()) else convertToDeviceDataModel(content.text.trim().toString().split("_").last())
                        presenter.sendSingleMessage(applicationContext, dataToSend, wait.selectedItem as Long, sendBF)
                        delay(partialDelay)
                        println("exit 1")
                    }
                    bt1.join()
                    //try to send text do bt device #2 and wait it to end
                    val bt2 = launch {
                        println("enter 2")
                        val dataToSend: DataToSend = if (rbt89.isChecked) convertToDeviceDataModel(content.text.trim().toString()) else convertToDeviceDataModel(content.text.trim().toString().split("_").last())
                        presenter.sendSingleMessage(applicationContext, dataToSend, wait.selectedItem as Long, send89)
                        delay(partialDelay)
                        println("exit 2")
                    }
                    bt2.join()
                    //try to send text do bt device #3 and wait it to end
                    val bt3 = launch {
                        println("enter 3")
                        val dataToSend: DataToSend = if (rbt3B.isChecked) convertToDeviceDataModel(content.text.trim().toString()) else convertToDeviceDataModel(content.text.trim().toString().split("_").last())
                        presenter.sendSingleMessage(applicationContext, dataToSend, wait.selectedItem as Long, send3B)
                        delay(partialDelay)
                        println("exit 3")
                    }
                    bt3.join()
                    //try to send text do bt device #4 and wait it to end
                    val bt4 = launch {
                        println("enter 4")
                        val dataToSend: DataToSend = if (rbtCD.isChecked) convertToDeviceDataModel(content.text.trim().toString()) else convertToDeviceDataModel(content.text.trim().toString().split("_").last())
                        presenter.sendSingleMessage(applicationContext, dataToSend, wait.selectedItem as Long, sendCD)
                        delay(partialDelay)
                        println("exit 4")
                    }
                    bt4.join()
                }
                else
                {
                    println("No text to send")
                }
                //delay
                delay(delayAmount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        prepareForScan()
        content.requestFocus()
        content.hideKeyboard()
        content.setText("")
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
        if (mediaPlayer != null) {
            mediaPlayer!!.pause()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, R.string.enable_bluetooth, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d { "Location permission accepted" }
            } else {
                Toast.makeText(this, R.string.grant_location_permission, Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d { "Code permission accepted" }
                accessDownloadsFolder()
            } else {
                Toast.makeText(this, R.string.grant_code_permission, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun convertToDeviceDataModel(text : String): DataToSend {
        return DataToSend(listOf(Message(text, flash.isChecked, marquee.isChecked, Speed.values()[speed.selectedItemPosition], Mode.values()[mode.selectedItemPosition])))
    }

    private fun prepareForScan() {
        if (isBleSupported()) {
            // Ensures Bluetooth is enabled on the device
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter
            if (btAdapter.isEnabled) {
                // Prompt for runtime permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Timber.i { "Coarse permission granted" }
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSION_LOCATION)
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun isBleSupported(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
}
