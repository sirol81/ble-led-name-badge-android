package com.nilhcem.blenamebadge.ui.message

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.*
import android.media.MediaPlayer
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.core.android.ext.hideKeyboard
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.android.viewbinding.bindView
import com.nilhcem.blenamebadge.device.model.DataToSend
import com.nilhcem.blenamebadge.device.model.Message
import com.nilhcem.blenamebadge.device.model.Mode
import com.nilhcem.blenamebadge.device.model.Speed

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
    private val send: Button by bindView(R.id.send_button_ALL)
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
    val add_bt: Button by bindView(R.id.add_button)
    val reset_bt: Button by bindView(R.id.reset_button)

    private val presenter by lazy { MessagePresenter() }
    lateinit var clipboardManager : ClipboardManager

    val mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_activity)

        val spinnerItem = android.R.layout.simple_spinner_dropdown_item
        speed.adapter = ArrayAdapter<String>(this, spinnerItem, Speed.values().mapIndexed { index, _ -> (index + 1).toString() })
        mode.adapter = ArrayAdapter<String>(this, spinnerItem, Mode.values().map { getString(it.stringResId) })
        wait.adapter = ArrayAdapter<Long>(this, spinnerItem, arrayOf(500L, 550L, 600L, 650L, 700L, 750L, 800L, 850L, 900L, 950L))
        timeout.adapter = ArrayAdapter<Long>(this, spinnerItem, arrayOf(5_000L, 10_000L, 15_000L, 20_000L, 25_000L, 30_000L))
        speed.setSelection(7)//speed8
        wait.setSelection(0)//sleep500
        timeout.setSelection(1)//timeout10000

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        send.setOnClickListener {
            send_BF.isEnabled = true
            send_89.isEnabled = true
            send_3B.isEnabled = true
            send_CD.isEnabled = true
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            presenter.sendMessage(this, convertToDeviceDataModel(content.text.trim().toString()), wait.selectedItem as Long, timeout.selectedItem as Long, send_BF, send_89, send_3B, send_CD)
        }

        send_BF.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            presenter.sendMessage(this, convertToDeviceDataModel(content.text.trim().toString()), wait.selectedItem as Long, timeout.selectedItem as Long, send_BF)
        }
        send_89.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            presenter.sendMessage(this, convertToDeviceDataModel(content.text.trim().toString()), wait.selectedItem as Long, timeout.selectedItem as Long, send_89)
        }
        send_3B.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            presenter.sendMessage(this, convertToDeviceDataModel(content.text.trim().toString()), wait.selectedItem as Long, timeout.selectedItem as Long, send_3B)
        }
        send_CD.setOnClickListener {
            if (content.text.isEmpty()) {
                //presenter.sendBitmap(this, BitmapFactory.decodeResource(resources, R.drawable.mix2))
                content.setText(clipboardManager.primaryClip?.getItemAt(0)?.text)
            }
            presenter.sendMessage(this, convertToDeviceDataModel(content.text.trim().toString()), wait.selectedItem as Long, timeout.selectedItem as Long, send_CD)
        }

        prev_bt.setOnClickListener {
            Toast.makeText(this, "PREV" , Toast.LENGTH_SHORT).show()
        }
        play_bt.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer!!.start()
            }
        }
        pause_bt.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer!!.pause()
            }
        }
        next_bt.setOnClickListener {
            Toast.makeText(this, "NEXT" , Toast.LENGTH_SHORT).show()
        }
        fadeL_bt.setOnClickListener {
            Toast.makeText(this, "L Fade" , Toast.LENGTH_SHORT).show()
        }
        fadeR_bt.setOnClickListener {
            Toast.makeText(this, "R Fade" , Toast.LENGTH_SHORT).show()
        }
        add_bt.setOnClickListener {
            Toast.makeText(this, "ADD" , Toast.LENGTH_SHORT).show()
        }
        reset_bt.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
                // after stopping the mediaplayer instance
                // it is again need to be prepared
                // for the next instance of playback
                mediaPlayer!!.prepare()
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
        } else {
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
