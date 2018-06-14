/*
 * <!--
 *   ~ Copyright (c) 2017. ThanksMister LLC
 *   ~
 *   ~ Licensed under the Apache License, Version 2.0 (the "License");
 *   ~ you may not use this file except in compliance with the License. 
 *   ~ You may obtain a copy of the License at
 *   ~
 *   ~ http://www.apache.org/licenses/LICENSE-2.0
 *   ~
 *   ~ Unless required by applicable law or agreed to in writing, software distributed 
 *   ~ under the License is distributed on an "AS IS" BASIS, 
 *   ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   ~ See the License for the specific language governing permissions and 
 *   ~ limitations under the License.
 *   -->
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui.modules

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import com.thanksmister.iot.mqtt.alarmpanel.BuildConfig
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import timber.log.Timber
import java.util.*
import android.speech.tts.UtteranceProgressListener

/**
 * Module to use Google Text-to-Speech to speak the payload of MQTT messages.
 */
class TextToSpeechModule( base: Context?, private val configuration: Configuration) : ContextWrapper(base),
        TextToSpeech.OnInitListener, LifecycleObserver {

    private var textToSpeech: TextToSpeech? = null
    private val UTTERANCE_ID = BuildConfig.APPLICATION_ID + ".UTTERANCE_ID"
    private var isInitialized = false

    init {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() {
        if(configuration.hasTssModule() && textToSpeech != null) {
            textToSpeech = TextToSpeech(baseContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech!!.language = Locale.getDefault()
            textToSpeech!!.voice = textToSpeech!!.defaultVoice
            textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onError(p0: String?) {
                    Timber.e( "error: " + p0)
                }
                override fun onStart(utteranceId: String) {
                    Timber.i("onStart")
                }
                override fun onDone(utteranceId: String) {
                    Timber.i( "onDone")
                }
                override fun onError(utteranceId: String, errorCode: Int) {
                    Timber.i( "onError ($utteranceId). Error code: $errorCode")
                }
            })

            isInitialized = true
            Timber.i( "TTS initialized successfully")
        } else {
            Timber.e("Error initializing text to speech: " + status)
        }
    }

    fun speakText(message: String) {
        if(configuration.hasTssModule()) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            if (textToSpeech != null && isInitialized) {
                Timber.d("Speak this: " + message)
                textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        if(textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        if(textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }
}