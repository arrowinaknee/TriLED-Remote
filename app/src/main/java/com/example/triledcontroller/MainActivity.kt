package com.example.triledcontroller

import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var addressInput : EditText
    lateinit var statusText : TextView
    lateinit var hue1Setting : SliderSetting
    lateinit var hue2Setting: SliderSetting
    lateinit var brightnessSetting: SliderSetting

    public var address: String = ""

    var lampPing : Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addressInput = findViewById(R.id.addressInput)
        statusText = findViewById(R.id.connectionStatus)

        hue1Setting = SliderSetting(this, findViewById(R.id.hue1Picker), findViewById(R.id.hue1Value), "hue1")
        hue2Setting = SliderSetting(this, findViewById(R.id.hue2Picker), findViewById(R.id.hue2Value), "hue2")
        brightnessSetting = SliderSetting(this, findViewById(R.id.brightnessPicker), findViewById(R.id.brightnessValue), "brightness", format = "%d%%")

        addressInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                address = s.toString()
                if(lampPing?.isAlive == true)
                    lampPing?.interrupt()

                statusText.text = getString(R.string.status_connecting)

                lampPing = Thread {
                    Looper.prepare()
                    try {
                        val url = URL("http://$s")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 1000 * 10
                        conn.connect()

                        // new address is passed, current one doesn't matter anymore
                        if(Thread.interrupted())
                            return@Thread

                        Toast.makeText(
                            baseContext,
                            "Lamp responded ${conn.responseCode}",
                            Toast.LENGTH_SHORT
                        ).show()
                        statusText.text = getString(R.string.status_connected)
                    } catch (e: MalformedURLException) {
                        statusText.text = getString(R.string.status_invalid_address)
                    } catch (e: IOException) {
                        // new address is passed, current one doesn't matter anymore
                        if(Thread.interrupted())
                            return@Thread

                        Toast.makeText(
                            baseContext,
                            "Lamp responded $e",
                            Toast.LENGTH_LONG
                        ).show()
                        statusText.text = getString(R.string.status_not_connected)
                    }
                }
                lampPing!!.start()
            }
        })
    }

    class SliderSetting(var activity: MainActivity, var slider: SeekBar, var display: TextView, var path: String, var scale : Int = 10, var format: String = "%d") : OnSeekBarChangeListener {
        init {
            slider.setOnSeekBarChangeListener(this)
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val realProgress = progress * scale
            display.text = String.format(format, realProgress)
            Thread {
                Looper.prepare()
                try {
                    val url = URL("http://${activity.address}/$path?val=$realProgress")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 1000 * 10
                    conn.connect()

                    Log.d("INFO", "Lamp responded ${conn.responseCode}")

                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }
}