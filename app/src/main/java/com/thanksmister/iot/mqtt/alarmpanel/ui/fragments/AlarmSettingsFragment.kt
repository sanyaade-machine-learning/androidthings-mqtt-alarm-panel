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

package com.thanksmister.iot.mqtt.alarmpanel.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.thanksmister.iot.mqtt.alarmpanel.BaseActivity
import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_ALARM_TOPIC
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_BROKER
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_CLIENT_ID
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_COMMAND_TOPIC
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_PASSWORD
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_PORT
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_TLS_CONNECTION
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions.Companion.PREF_USERNAME
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.ui.views.AlarmCodeView
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AlarmSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var dialogUtils: DialogUtils

    private var brokerPreference: EditTextPreference? = null
    private var clientPreference: EditTextPreference? = null
    private var portPreference: EditTextPreference? = null
    private var commandTopicPreference: EditTextPreference? = null
    private var stateTopicPreference: EditTextPreference? = null
    private var userNamePreference: EditTextPreference? = null
    private var passwordPreference: EditTextPreference? = null
    private var pendingPreference: EditTextPreference? = null
    private var sslPreference: CheckBoxPreference? = null

    private var mqttOptions: MQTTOptions? = null
    private var defaultCode: Int = 0
    private var tempCode: Int = 0
    private var confirmCode = false

    override fun onAttach(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Perform injection here for M (API 23) due to deprecation of onAttach(Activity).
            AndroidSupportInjection.inject(this)
        }
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey : String?) {
        addPreferencesFromResource(R.xml.preferences)
        lifecycle.addObserver(dialogUtils)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val buttonPreference = findPreference(Configuration.PREF_ALARM_CODE)
        buttonPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showAlarmCodeDialog()
            true
        }

        brokerPreference = findPreference(PREF_BROKER) as EditTextPreference
        clientPreference = findPreference(PREF_CLIENT_ID) as EditTextPreference
        portPreference = findPreference(PREF_PORT) as EditTextPreference
        commandTopicPreference = findPreference(PREF_COMMAND_TOPIC) as EditTextPreference
        stateTopicPreference = findPreference(PREF_ALARM_TOPIC) as EditTextPreference
        userNamePreference = findPreference(PREF_USERNAME) as EditTextPreference
        passwordPreference = findPreference(PREF_PASSWORD) as EditTextPreference
        pendingPreference = findPreference(Configuration.PREF_PENDING_TIME) as EditTextPreference
        sslPreference = findPreference(PREF_TLS_CONNECTION) as CheckBoxPreference

        if (isAdded && activity != null) {
            mqttOptions = (activity as BaseActivity).readMqttOptions()
        }

        brokerPreference!!.text = mqttOptions!!.getBroker()
        clientPreference!!.text = mqttOptions!!.getClientId().toString()
        portPreference!!.text = mqttOptions!!.getPort().toString()
        commandTopicPreference!!.text = mqttOptions!!.getCommandTopic()
        stateTopicPreference!!.text = mqttOptions!!.getAlarmTopic()
        userNamePreference!!.text = mqttOptions!!.getUsername()
        passwordPreference!!.text = mqttOptions!!.getPassword()
        pendingPreference!!.text = configuration.pendingTime.toString()
        sslPreference!!.isChecked = mqttOptions!!.getTlsConnection()

        if (!TextUtils.isEmpty(mqttOptions!!.getBroker())) {
            brokerPreference!!.summary = mqttOptions!!.getBroker()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getClientId())) {
            clientPreference!!.summary = mqttOptions!!.getClientId()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getPort().toString())) {
            portPreference!!.summary = mqttOptions!!.getPort().toString()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getCommandTopic())) {
            commandTopicPreference!!.summary = mqttOptions!!.getCommandTopic()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getAlarmTopic())) {
            stateTopicPreference!!.summary = mqttOptions!!.getAlarmTopic()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getUsername())) {
            userNamePreference!!.summary = mqttOptions!!.getUsername()
        }
        if (!TextUtils.isEmpty(mqttOptions!!.getPassword())) {
            passwordPreference!!.summary = toStars(mqttOptions!!.getPassword())
        }
        pendingPreference!!.summary = getString(R.string.preference_summary_pending_time, configuration.pendingTime.toString())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val value: String
        when (key) {
            PREF_BROKER -> {
                value = brokerPreference!!.text
                if (!TextUtils.isEmpty(value)) {
                    mqttOptions!!.setBroker(value)
                    brokerPreference!!.summary = value
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_blank_entry, Toast.LENGTH_LONG).show()
                }
            }
            PREF_CLIENT_ID -> {
                value = clientPreference!!.text
                if (!TextUtils.isEmpty(value)) {
                    mqttOptions!!.setClientId(value)
                    clientPreference!!.summary = value
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_blank_entry, Toast.LENGTH_LONG).show()
                    clientPreference!!.text = mqttOptions!!.getClientId()
                }
            }
            PREF_PORT -> {
                value = portPreference!!.text
                if (value.matches("[0-9]+".toRegex()) && !TextUtils.isEmpty(value)) {
                    mqttOptions!!.setPort(Integer.valueOf(value)!!)
                    portPreference!!.summary = value.toString()
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_only_numbers, Toast.LENGTH_LONG).show()
                    portPreference!!.text = mqttOptions!!.getPort().toString()
                }
            }
            PREF_COMMAND_TOPIC -> {
                value = commandTopicPreference!!.text
                if (!TextUtils.isEmpty(value)) {
                    mqttOptions!!.setCommandTopic(value)
                    commandTopicPreference!!.summary = value
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_blank_entry, Toast.LENGTH_LONG).show()
                    commandTopicPreference!!.text = mqttOptions!!.getCommandTopic()
                }
            }
            PREF_ALARM_TOPIC -> {
                value = stateTopicPreference!!.text
                if (!TextUtils.isEmpty(value)) {
                    mqttOptions!!.setAlarmTopic(value)
                    stateTopicPreference!!.summary = value
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_blank_entry, Toast.LENGTH_LONG).show()
                    stateTopicPreference!!.text = mqttOptions!!.getAlarmTopic()
                }
            }
            PREF_USERNAME -> {
                value = userNamePreference!!.text
                mqttOptions!!.setUsername(value)
                userNamePreference!!.summary = value
            }
            PREF_PASSWORD -> {
                value = passwordPreference!!.text
                mqttOptions!!.setPassword(value)
                passwordPreference!!.summary = toStars(value)
            }
            Configuration.PREF_PENDING_TIME -> {
                value = pendingPreference!!.text
                if (value.matches("[0-9]+".toRegex()) && !TextUtils.isEmpty(value)) {
                    val pendingTime = Integer.parseInt(value)
                    if (pendingTime < 10) {
                        if (isAdded) {
                            dialogUtils.showAlertDialog(activity as BaseActivity, getString(R.string.text_error_pending_time_low))
                        }
                    }
                    configuration.pendingTime = pendingTime
                    pendingPreference!!.text = pendingTime.toString()
                    pendingPreference!!.summary = getString(R.string.preference_summary_pending_time, pendingTime.toString())
                } else if (isAdded) {
                    Toast.makeText(activity, R.string.text_error_only_numbers, Toast.LENGTH_LONG).show()
                    pendingPreference!!.text = configuration.pendingTime.toString()
                }
            }
            PREF_TLS_CONNECTION -> {
                val checked = sslPreference!!.isChecked
                mqttOptions!!.setTlsConnection(checked)
            }
        }
    }

    private fun toStars(textToStars: String?): String {
        var text = textToStars
        val sb = StringBuilder()
        for (i in 0 until text!!.length) {
            sb.append('*')
        }
        text = sb.toString()
        return text
    }

    private fun showAlarmCodeDialog() {
        // store the default alarm code
        defaultCode = configuration.alarmCode
        if (isAdded) {
            dialogUtils.showCodeDialog(activity as BaseActivity, confirmCode, object : AlarmCodeView.ViewListener {
                override fun onComplete(code: Int) {
                    if (code == defaultCode) {
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_match, Toast.LENGTH_LONG).show()
                    } else if (!confirmCode) {
                        tempCode = code
                        confirmCode = true
                        dialogUtils.clearDialogs()
                        showAlarmCodeDialog()
                    } else if (code == tempCode) {
                        configuration.alarmCode = tempCode
                        tempCode = 0
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_changed, Toast.LENGTH_LONG).show()
                    } else {
                        tempCode = 0
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_not_match, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onError() {}
                override fun onCancel() {
                    confirmCode = false
                    dialogUtils.clearDialogs()
                    Toast.makeText(activity, R.string.toast_code_unchanged, Toast.LENGTH_SHORT).show()
                }
            }, DialogInterface.OnCancelListener {
                confirmCode = false
                Toast.makeText(activity, R.string.toast_code_unchanged, Toast.LENGTH_SHORT).show()
            })
        }
    }
}