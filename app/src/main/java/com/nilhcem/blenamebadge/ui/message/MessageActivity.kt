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
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
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
    private val timeout: Spinner by bindView(R.id.timeout)
    private val wait: Spinner by bindView(R.id.waiting)
    private val clear: Button by bindView(R.id.send_button_ALL)
    val send_BF: Button by bindView(R.id.send_button_BF)
    val send_89: Button by bindView(R.id.send_button_89)
    val send_3B: Button by bindView(R.id.send_button_3B)
    val send_CD: Button by bindView(R.id.send_button_CD)
    val prev_bt: Button by bindView(R.id.prev_button)
    val play_bt: Button by bindView(R.id.play_button)
    val pause_bt: Button by bindView(R.id.pause_button)
    val next_bt: Button by bindView(R.id.next_button)
    val fadeL_bt: Button by bindView(R.id.fade_left)
    val fadeR_bt: Button by bindView(R.id.fade_right)
    val stop_bt: Button by bindView(R.id.stop_button)
    val reset_bt: Button by bindView(R.id.reset_button)
    val console: TextView by bindView(R.id.console)
    val bmp_BF: RadioButton by bindView(R.id.bpm_BF)
    val bmp_89: RadioButton by bindView(R.id.bpm_89)
    val bmp_3B: RadioButton by bindView(R.id.bpm_3B)
    val bmp_CD: RadioButton by bindView(R.id.bpm_CD)

    private val presenter by lazy { MessagePresenter() }
    lateinit var clipboardManager : ClipboardManager

    val loop: ArrayList<File> = ArrayList<File>()
    var mediaPlayer: MediaPlayer? = null
    var index = 0
    var l:Float = 1.0F
    var r:Float = 1.0F
    val addresses = mutableListOf("38:3B:26:EC:64:BF","38:3B:26:EC:64:89","38:3B:26:EC:64:3B","38:3B:26:EC:64:CD")

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
            loop.addAll(files)
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
        wait.setSelection(1)//sleep


        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        send_BF.setTag(addresses[0])
        send_89.setTag(addresses[1])
        send_3B.setTag(addresses[2])
        send_CD.setTag(addresses[3])

        checkPermissions()
        if (loop.count() > 0) {
            timeout.setSelection(index)

            timeout.adapter = ArrayAdapter<String>(this, spinnerItem, loop.map { it.nameWithoutExtension  })
            timeout?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    index = position
                    timeout.setSelection(index)
                }
            }
        }

        clear.setOnClickListener {
            send_BF.isEnabled = true
            send_89.isEnabled = true
            send_3B.isEnabled = true
            send_CD.isEnabled = true
            content.setText("")
            Metronome.stop()
        }

        send_BF.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            var textToSend = content.text.trim().toString()
            if (!bmp_BF.isChecked())
            {//trim bpm
                textToSend = textToSend.split("_").last()
            }
            else
            {//metronome
                Metronome.stop()
                Metronome.start(this, textToSend.split("_").first().toLong(), clear)
            }
            presenter.sendSingleMessage(this, convertToDeviceDataModel(textToSend), wait.selectedItem as Long, send_BF, console)
        }
        send_89.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            var textToSend = content.text.trim().toString()
            if (!bmp_89.isChecked())
            {//trim bpm
                textToSend = textToSend.split("_").last()
            }
            else
            {//metronome
                Metronome.stop()
                Metronome.start(this, textToSend.split("_").first().toLong(), clear)
            }
            presenter.sendSingleMessage(this, convertToDeviceDataModel(textToSend), wait.selectedItem as Long, send_89, console)
        }
        send_3B.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            var textToSend = content.text.trim().toString()
            if (!bmp_3B.isChecked())
            {//trim bpm
                textToSend = textToSend.split("_").last()
            }
            else
            {//metronome
                Metronome.stop()
                Metronome.start(this, textToSend.split("_").first().toLong(), clear)
            }
            presenter.sendSingleMessage(this, convertToDeviceDataModel(textToSend), wait.selectedItem as Long, send_3B, console)
        }
        send_CD.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            var textToSend = content.text.trim().toString()
            if (!bmp_CD.isChecked())
            {//trim bpm
                textToSend = textToSend.split("_").last()
            }
            else
            {//metronome
                Metronome.stop()
                Metronome.start(this, textToSend.split("_").first().toLong(), clear)
            }
            presenter.sendSingleMessage(this, convertToDeviceDataModel(textToSend), wait.selectedItem as Long, send_CD, console)
        }

        play_bt.setOnClickListener {
            val applicationContext: Activity = this
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
                setDataSource(applicationContext, Uri.fromFile(loop[index]))
                setVolume(l, r)
                prepare()
                start()
            }
        }
        pause_bt.setOnClickListener {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying())
                {
                    mediaPlayer!!.pause()
                }
                else
                {
                    mediaPlayer!!.start()
                }
            }
        }
        stop_bt.setOnClickListener {
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
                mp!!.stop()
                // after stopping the mediaplayer instance
                // it is again need to be prepared
                // for the next instance of playback
                mp!!.prepare()
                l = 1.0F
                r = 1.0F
                mp!!.setVolume(l, r)
            }
        }

        prev_bt.setOnClickListener {
            if (index > 0)
            {
                index -= 1
            }
            if (loop.count() > 0) {
                timeout.setSelection(index)
            }
        }
        next_bt.setOnClickListener {
            if (index < loop.count()-1)
            {
                index += 1
            }
            if (loop.count() > 0) {
                timeout.setSelection(index)
            }
        }

        fadeL_bt.setOnClickListener {
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
        fadeR_bt.setOnClickListener {
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


        reset_bt.setOnClickListener {
            l = 1.0F
            r = 1.0F
            if (mediaPlayer != null) {
                mediaPlayer!!.setVolume(l, r)
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
        super.onActivityResult(requestCode, resultCode, data);
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
