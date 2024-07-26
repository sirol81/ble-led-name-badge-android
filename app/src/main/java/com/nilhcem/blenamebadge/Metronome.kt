package com.nilhcem.blenamebadge

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import com.nilhcem.blenamebadge.ui.message.MessageActivity
import java.util.*
import kotlin.concurrent.timerTask

typealias BPM = Long

enum class MetronomeState {
    OFF,
    ON
}

object Metronome {

    // Thresholds to be used for logic outside of the Metronome class
    const val BPM_LOWER_THRESHOLD: BPM = 40L
    const val BPM_UPPER_THRESHOLD: BPM = 210L

    private const val MILLISECONDS_IN_SECOND: Int = 1000
    private const val SECONDS_IN_MINUTE: Int = 60

    private var metronomeState: MetronomeState
    private var metronome: Timer

    init {
        metronome = Timer("metronome", true)
        metronomeState = MetronomeState.OFF
    }

    private fun createNewTimer() {
        if (this.isOff()) {
            this.metronome = Timer("metronome", true)
        }
    }

    private fun calculateSleepDuration(bpm: BPM): Long {
        return (MILLISECONDS_IN_SECOND * (SECONDS_IN_MINUTE / bpm.toDouble())).toLong()
    }

    fun start(context: Context, bpm: BPM, beats: Button): Boolean {
        val activity : MessageActivity = context as MessageActivity

        if (this.isOn()) {
            return false
        }

        this.metronomeState = MetronomeState.ON
        this.metronome.schedule(
            timerTask {
                activity.runOnUiThread{
                    beats.setTextColor(Color.RED)
                }
            },
            0L,
            calculateSleepDuration(bpm)
        )

        this.metronome.schedule(
                timerTask {
                    activity.runOnUiThread{
                        beats.setTextColor(Color.BLACK)
                    }
                },
                100L,
                calculateSleepDuration(bpm)
        )

        return true
    }

    fun stop(): Boolean {
        if (this.isOff()) {
            return false
        }

        this.metronomeState = MetronomeState.OFF
        this.metronome.cancel()
        createNewTimer()

        return true
    }

    fun isOn(): Boolean {
        return this.metronomeState == MetronomeState.ON
    }

    fun isOff(): Boolean {
        return this.metronomeState == MetronomeState.OFF
    }
}