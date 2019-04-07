/**
 *  HomeSeer HS-FC200+
 *
 *  Copyright 2018 @Pluckyhd, DarwinsDen.com, HomeSeer, @aruffell 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Author: @pluckyHD, Darwin@DarwinsDen.com, HomeSeer, @aruffell, with fan control button code leveraged from @ChadCK
 *	Date: 2018-18-Oct
 *
 *	Changelog:
 *    4.1 3/29/2019 fixed off/on not working digital, fixed compatibility of slider
 *    4.0 3/29/2019 rules didn't allow double digit buttons so changed numbers accordingly.
 *    3.0 3/29/2019 fixed blink of leds and moves multi push/hold to buttons for better integration in rules.
 *	  2.0 3/26/2019 Initial commit of Hubitat port by @PluckyHD
 *    1.0 Oct 2018 Initial Version based on WD200+
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1     	   pressed
 *   Double-Tap Down   2    	   pressed
 *   Triple-Tap Up     3    	   pressed
 *   Triple-Tap Down   4    	   pressed
 *   Hold Up           5 		   pressed
 *   Hold Down         6 		   pressed
 *   Single-Tap Up     7    	   pressed
 *   Single-Tap Down   8    	   pressed
 *   4 taps up         9    	   pressed
 *   4 taps down       10    	   pressed
 *   5 taps up         11    	   pressed
 *   5 taps down       12    	   pressed
 **/

metadata {
    definition(name: "HomeSeer FC200+ Working", namespace: "spalexander", author: "spalexander") {

		capability "Actuator"
		capability "PushableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "SwitchLevel"
		capability "FanControl"
		
		attribute "speed", "enum", ["low","medium-low","medium","medium-high","high","on","off","auto"]
		
		
		attribute	"ledMode", "string"
		attribute   "numberOfButtons", "number"
		attribute   "pushed", "number"
		attribute   "btnStat", "string"
		attribute 	"speed", "enum", ["low","medium-low","medium","medium-high","high","on","off","auto"]
        
        //command "lowSpeed"
		//command "medSpeed"
		//command "highSpeed"
		command "setLEDModeToNormal"
		command "setLEDModeToStatus"
        command "setStatusLed", ["integer","integer","integer"]
        command "setBlinkDurationMilliseconds" ,["integer"]
		command "push",["integer"]

        fingerprint mfr: "000C", prod: "0203", model: "0001"

    }

    preferences {
		section("Settings"){
			input "doubleTapToFullSpeed", "bool", title: "Double-Tap Up sets to full speed", defaultValue: false, displayDuringSetup: true, required: false
			input "singleTapToFullSpeed", "bool", title: "Single-Tap Up sets to full speed", defaultValue: false, displayDuringSetup: true, required: false
			input "doubleTapDownToDim", "bool", title: "Double-Tap Down sets to 25% speed", defaultValue: false, displayDuringSetup: true, required: false
			input "enable4FanSpeeds", "bool", title: "Enable 4 fan speed mode", defaultValue: false, displayDuringSetup: true, required: false
			input "reverseSwitch", "bool", title: "Reverse Switch", defaultValue: false, displayDuringSetup: true, required: false
			input "bottomled", "bool", title: "Bottom LED ON if Load is OFF", defaultValue: false, displayDuringSetup: true, required: false
			input("color", "enum", title: "Default LED Color", options: ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"], description: "Select Color", required: false)
		}
		
		section("Fan Thresholds") {
			input "lowThreshold", "number", title: "Low Threshold", range: "1..99"
			input "medThreshold", "number", title: "Medium Threshold", range: "1..99"
			input "highThreshold", "number", title: "High Threshold", range: "1..99"
		}		
		
	}

}

def parse(String description) {
    def result = null
    log.debug(description)
    if (description != "updated") {
        def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
        if (cmd) {
            result = zwaveEvent(cmd)
        }
    }
    if (!result) {
        log.debug "Parse returned ${result} for command ${cmd}"
    } else {
        log.debug "Parse returned ${result}"
    }
    return result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd) {
    dimmerEvents(cmd)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
	def lowThresholdvalue = (settings.lowThreshold != null && settings.lowThreshold != "") ? settings.lowThreshold.toInteger() : 33
	def medThresholdvalue = (settings.medThreshold != null && settings.medThreshold != "") ? settings.medThreshold.toInteger() : 66
	def highThresholdvalue = (settings.highThreshold != null && settings.highThreshold != "") ? settings.highThreshold.toInteger() : 99

    def value = (cmd.value ? "on" : "off")
    def result = [createEvent(name: "switch", value: value, isStateChange: true)]
    state.lastLevel = cmd.value
    if (cmd.value && cmd.value <= 100) {
        if (cmd.value > 0 && cmd.value <= lowThresholdvalue) { sendEvent(name: "currentState", value: "LOW" as String) }
        if (cmd.value >= lowThresholdvalue+1 && cmd.value <= medThresholdvalue) { sendEvent(name: "currentState", value: "MED" as String) }
	    if (cmd.value >= medThresholdvalue+1) { sendEvent(name: "currentState", value: "HIGH" as String) }
        result << createEvent(name: "level", value: cmd.value, unit: "%")
    }
    return result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    log.debug "ConfigurationReport $cmd"
    def value = "when off"
    if (cmd.configurationValue[0] == 1) {
        value = "when on"
    }
    if (cmd.configurationValue[0] == 2) {
        value = "never"
    }
    createEvent([name: "indicatorStatus", value: value])
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
    createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}


def on() {
    push("digital",7)
	sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
    delayBetween([
        zwave.basicV1.basicSet(value: 0xFF).format(),
        zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 5000)
}

def offer() {
    push("digital",8)
	sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
    delayBetween([
        zwave.basicV1.basicSet(value: 0x00).format(),
        zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 5000)
}

def off() {
	if (logEnable) log.debug "Turn device OFF"
	def cmds = []
	sendEvent(name: "switch", value: "off", isStateChange: true, descriptionText: "$device.displayName is off")
	if (logDesc) log.info "$device.displayName is off"
	push("digital",8)
    cmds << zwave.basicV1.basicSet(value: 0x00).format()
   	cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
	delayBetween(cmds, 3000)
}

def push(btnNum)
{
	log.debug "Button pushed $btnNum"
	push("Digital",btnNum)
}

def push(String buttonType, btnNum) {}

def updated() {
    log.info "Updating..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def cmds = []

	if (reverseSwitch) {
		cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
    } else {
		cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
    }

    if (bottomled) {
		cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
    } else {
        cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
    }

    //Sets fan type
    if (enable4FanSpeeds) {
        //4 Speeds
		cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 5, size: 1).format()
    } else {
        //3 Speeds
        cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 5, size: 1).format()
    }
	
    if (color) {
        switch (color) {
            case "White":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 14, size: 1).format()
                break
            case "Red":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 14, size: 1).format()
                break
            case "Green":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 14, size: 1).format()
                break
            case "Blue":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [3], parameterNumber: 14, size: 1).format()
                break
            case "Magenta":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [4], parameterNumber: 14, size: 1).format()
                break
            case "Yellow":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [5], parameterNumber: 14, size: 1).format()
                break
            case "Cyan":
                cmds << zwave.configurationV2.configurationSet(configurationValue: [6], parameterNumber: 14, size: 1).format()
                break
        }
    }	
	
    //Enable the following configuration gets to verify configuration in the logs
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 14).format()
	cmds << zwave.configurationV1.configurationGet(parameterNumber: 5).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 4).format()
	cmds << zwave.configurationV1.configurationGet(parameterNumber: 3).format()
	
    delayBetween(cmds, 500)	
}

def setLevel(value) {
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def currval = device.currentValue("switch")
	state.level = level
	
	if (logEnable) log.debug "SetLevel (value) - currval: $currval"
	
	if (level > 0 && currval == "off") {
		sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName is on")
		if (logDesc) log.info "$device.displayName is on"
	} else if (level == 0 && currval == "on") {
		sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName is off")
		if (logDesc) log.info "$device.displayName is off"
	}
	sendEvent(name: "level", value: level, unit: "%", descriptionText: "$device.displayName is " + level + "%")
	
	if (logEnable) log.debug "setLevel >> value: $level"

	delayBetween ([
    	zwave.basicV1.basicSet(value: level).format(),
        zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 3000 )
}


def setSpeed(fanspeed) {
	if (logEnable) log.debug "fanspeed is $fanspeed"
	def value
	def result
	
	//speed - ENUM ["low","medium-low","medium","medium-high","high","on","off","auto"]
    switch (fanspeed) {
        case "low":
			if (logEnable) log.debug "fanspeed low detected"	
			sendEvent([name: "speed", value: "low", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			if (paramLOW==null) {paramLOW = 20}	
			value = paramLOW
            setLevel(value)
			break
		case "medium-low":
			if (logEnable) log.debug "fanspeed medium-low detected"	
			sendEvent([name: "speed", value: "medium-low", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			if (paramMEDLOW==null) {paramMEDLOW = 40}	
			value = paramMEDLOW
			setLevel(value)
            break
		case "medium":
			if (logEnable) log.debug "fanspeed medium detected"	
			sendEvent([name: "speed", value: "medium", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			if (paramMED==null) {paramMED = 60}	
			value = paramMED
			setLevel(value)
            break
		case "medium-high":
			if (logEnable) log.debug "fanspeed medium-high detected"	
			sendEvent([name: "speed", value: "medium-high", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			if (paramMEDHIGH==null) {paramMEDHIGH = 80}	
			value = paramMEDHIGH
			setLevel(value)
            break
		case "high":
			if (logEnable) log.debug "fanspeed high detected"	
			sendEvent([name: "speed", value: "high", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			if (paramHIGH==null) {paramHIGH = 99}	
			value = paramHIGH
			setLevel(value)
            break
		case "off":
			if (logEnable) log.debug "speed off detected"
			sendEvent([name: "speed", value: "off", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			off()
			break
		case "on":
			if (logEnable) log.debug "speed on detected"	
			//sendEvent([name: "speed", value: "on", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			on()
			break
		case "auto":
			//if (logEnable) log.debug "speed auto detected"	
			//sendEvent([name: "speed", value: "on", displayed: true, descriptionText: "fan speed set to $fanspeed"])		
			//on()
			log.warn "Speed AUTO requested. This doesn't do anything in this driver right now."
			break
		default:
            break
    }
}
