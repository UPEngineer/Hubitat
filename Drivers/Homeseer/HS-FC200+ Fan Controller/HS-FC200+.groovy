/*
 *  IMPORT URL: https://raw.githubusercontent.com/Botched1/Hubitat/master/Drivers/GE-Jasco%20Z-Wave%20Plus%20Fan%20Control/GE%20Z-Wave%20Plus%20Fan%20Control.groovy
 *
 *	HomeSeer HS-FC200+ Fan Controller
 *
 *  Original based off of the FC200+ Fan Control port by @pluckyHD 
 *
 *  VERSION HISTORY
 *  1.0.0 (04/12/2019) - Initial Version
 *
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
    definition(name: "HomeSeer HS-FC200+ Fan Controller", namespace: "UPEngineer", author: "Scott Alexander") {

		capability "Actuator"
		capability "PushableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "SwitchLevel"
		capability "FanControl"
		
		attribute 	"speed", "enum", ["low","medium-low","medium","medium-high","high","on","off","auto"]
		
		attribute	"ledMode", "string"
		attribute   "numberOfButtons", "number"
		attribute   "pushed", "number"
		attribute   "btnStat", "string"
		        
        command "setLEDModeToNormal"
		command "setLEDModeToStatus"
        command "setStatusLed", ["string","string","string"]
        command "setBlinkDurationMilliseconds" ,["integer"]
		command "push",["integer"]

        fingerprint mfr: "000C", prod: "0203", model: "0001"

    }

    preferences {
	
		input (
            type: "paragraph",
            element: "paragraph",
            title: "Fan Control General Settings",
            description: ""
        )
	
		input "doubleTapToFullSpeed", "bool", title: "Double-Tap Up sets to full speed", defaultValue: false, displayDuringSetup: true, required: false
		input "singleTapToFullSpeed", "bool", title: "Single-Tap Up sets to full speed", defaultValue: false, displayDuringSetup: true, required: false
		input "doubleTapDownToDim", "bool", title: "Double-Tap Down sets to low speed", defaultValue: false, displayDuringSetup: true, required: false
		input "enable4FanSpeeds", "bool", title: "Enable 4-speed fan mode", defaultValue: false, displayDuringSetup: true, required: false
		input "reverseSwitch", "enum", title: "Reverse Switch Paddle", multiple: false, options: ["0" : "Normal (default)", "1" : "Inverted"], required: false, displayDuringSetup: true
		input "bottomled", "bool", title: "Bottom LED ON if Load is OFF", defaultValue: false, displayDuringSetup: true, required: false
		input("color", "enum", title: "Default LED Color", options: ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"], description: "Select Color", required: false)
		
		input (
            type: "paragraph",
            element: "paragraph",
            title: "Fan Speed Thresholds",
            description: ""
        )
		
		input "lowThreshold", "number", title: "Low Threshold %  (3-speed mode)", defaultValue: "33", range: "1..99"
		input "medThreshold", "number", title: "Medium Threshold %  (3-speed mode)", defaultValue: "66", range: "1..99"
		input "highThreshold", "number", title: "High Threshold %  (3-speed mode)", defaultValue: "99", range: "1..99"
		input "paramLOW", "number", title: "Low Speed Fan %", multiple: false, defaultValue: "20",  range: "1..99", required: false, displayDuringSetup: true
		input "paramMEDLOW", "number", title: "Medium-Low Speed Fan %", multiple: false, defaultValue: "40",  range: "1..99", required: false, displayDuringSetup: true
		input "paramMED", "number", title: "Medium Speed Fan %", multiple: false, defaultValue: "60",  range: "1..99", required: false, displayDuringSetup: true
		input "paramMEDHIGH", "number", title: "Medium-High Speed Fan %", multiple: false, defaultValue: "80",  range: "1..99", required: false, displayDuringSetup: true
		input "paramHIGH", "number", title: "High Speed Fan %", multiple: false, defaultValue: "99",  range: "1..99", required: false, displayDuringSetup: true			
		
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false	
		input name: "logDesc", type: "bool", title: "Enable descriptionText logging", defaultValue: true
		
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Parse
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def parse(String description) {
    def result = null
    if (description != "updated") {
		if (logEnable) log.debug "parse() >> zwave.parse($description)"
        def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
		
		if (logEnable) log.debug "cmd: $cmd"
		
        if (cmd) {
            result = zwaveEvent(cmd)
        }
    }
    if (!result) { if (logEnable) log.debug "Parse returned ${result} for $description" }
    else {if (logEnable) log.debug "Parse returned ${result}"}
    
	return result
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Z-Wave Messages
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
    //dimmerEvents(cmd)
	if (logEnable) log.debug "---SwitchMultilevelReport V3---  ${device.displayName} sent ${cmd}"
	
	def currSpeed = device.currentValue("speed")

	
	if (cmd.value) {
		sendEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "$device.displayName is " + cmd.value + "%")
		if (logDesc) log.info "$device.displayName is " + cmd.value + "%"
		
		if (device.currentValue("switch") == "off") {
			sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
			if (logDesc) log.info "$device.displayName is on"
		}
	} else {
		if (device.currentValue("switch") == "on") {
			sendEvent(name: "switch", value: "off", isStateChange: true, , descriptionText: "$device.displayName is off")
			if (logDesc) log.info "$device.displayName is off"
		}
	}
	
	// Display different speeds 3 Speed vs 4 Speed
	if (cmd.value==0) {sendEvent([name: "speed", value: "off", descriptionText: "fan speed set to off"])}
	
	if(enable4FanSpeeds) {

		if (cmd.value>0 && cmd.value<=paramLOW) {sendEvent([name: "speed", value: "low", displayed: true, descriptionText: "fan speed set to low"])}
		if (cmd.value>paramLOW && cmd.value<=paramMEDLOW) {sendEvent([name: "speed", value: "medium-low", displayed: true, descriptionText: "fan speed set to medium-low"])}
		if (cmd.value>paramMEDLOW && cmd.value<=paramMED) {sendEvent([name: "speed", value: "medium", displayed: true, descriptionText: "fan speed set to medium"])}
		if (cmd.value>paramMED && cmd.value<=paramMEDHIGH) {sendEvent([name: "speed", value: "medium-high", displayed: true, descriptionText: "fan speed set to medium-high"])}
		if (cmd.value>paramMEDHIGH && cmd.value<=99) {sendEvent([name: "speed", value: "high", displayed: true, descriptionText: "fan speed set to high"])}
	
	} else {
	
		def lowThresholdvalue = (settings.lowThreshold != null && settings.lowThreshold != "") ? settings.lowThreshold.toInteger() : 33
		def medThresholdvalue = (settings.medThreshold != null && settings.medThreshold != "") ? settings.medThreshold.toInteger() : 66
		def highThresholdvalue = (settings.highThreshold != null && settings.highThreshold != "") ? settings.highThreshold.toInteger() : 99
		
		if (cmd.value > 0 && cmd.value <= lowThresholdvalue) { sendEvent([name: "speed", value: "low", displayed: true, descriptionText: "fan speed set to low"]) }
		if (cmd.value >= lowThresholdvalue+1 && cmd.value <= medThresholdvalue) { sendEvent([name: "speed", value: "medium", displayed: true, descriptionText: "fan speed set to medium"]) }
		if (cmd.value >= medThresholdvalue+1) { sendEvent([name: "speed", value: "high", displayed: true, descriptionText: "fan speed set to high"]) }

		}
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd) {
    log.debug "Doesn't do anything right now"
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
	if (logEnable) log.debug "---CONFIGURATION REPORT V1--- ${device.displayName} sent ${cmd}"
	def config = cmd.scaledConfigurationValue.toInteger()
    def result = []
	def name = ""
    def value = ""
    def reportValue = cmd.configurationValue[0]
	switch (cmd.parameterNumber) {
		case 3:
			value = reportValue == 0 ? "when off" : "when on"
			break
		default:
			break
	}
	
	result << createEvent([name: "indicatorStatus", value: value])
	return result
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "---MANUFACTURER SPECIFIC REPORT V2--- ${device.displayName} sent ${cmd}"
    log.debug "manufacturerId: 		${cmd.manufacturerId}"
    log.debug "manufacturerName: 	${cmd.manufacturerName}"
    state.manufacturer = cmd.manufacturerName
    log.debug "productId: 			${cmd.productId}"
    log.debug "productTypeId: 		${cmd.productTypeId}"
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
	sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	if (logEnable) log.debug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
    createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}

def zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
    log.debug("received Firmware Report")
    log.debug "checksum: ${cmd.checksum}"
    log.debug "firmwareId: ${cmd.firmwareId}"
    log.debug "manufacturerId: ${cmd.manufacturerId}" [: ]
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
    [createEvent(name: "switch", value: "on"), response(zwave.switchMultilevelV1.switchMultilevelGet().format())]
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    // Handles all Z-Wave commands we aren’t interested in
	if (logEnable) log.warn "${device.displayName} received unhandled command: ${cmd}"
    [:]
}

def on() {
	if (logEnable) log.debug "Turn device ON"
	def cmds = []
    sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
	if (logDesc) log.info "$device.displayName is on"
	push("digital",7)
    cmds << zwave.basicV1.basicSet(value: 0xFF).format()
    cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
    delayBetween(cmds, 5000)
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
	if (logEnable) log.debug "Button pushed $btnNum"
	push("Digital", btnNum)
}

def push(String buttonType, btnNum) {
	def cmds = []
	def needsCmds = false
	def statValue = ""
	if (btnNum > 100)
	{
			def numbers = btnNum.toString()
			def led = numbers.charAt(0).toString().toInteger()
			def color = numbers.charAt(1).toString().toInteger()
			def blink = numbers.charAt(2).toString().toInteger()
			log.debug "stats $led $color $blink"
			cmds += setStatusLed(led, color, blink,true)
			log.debug "cmds to fire = $cmds"
			needsCmds = true
			colorName = getColor(color)
	
		statValue = "LED # $led Color: $colorName Blink: ${blink==0? "Off": "ON"}"
	}
	else
	{
		switch(btnNum){


					case 1:
					 // 2 Times
							statValue = "Tap ▲▲"
							break

					case 2:
					 // 2 Times
							statValue = "Tap ▼▼"
							break
					case 3:
					// 3 times
							statValue = "Tap ▲▲▲"
							break
					case 4:
					// 3 times
							statValue = "Tap ▼▼▼"
							break

					case 5:
					//Hold 
							statValue = "Hold ▲"
							break

					case 6:
					//Hold 
							statValue = "Hold ▼"
							break

					case 7:
							statValue ="Tap ▲"
							break

					case 8:
							statValue ="Tap ▼"
							break		

					case 9:
					// 4 times
							statValue = "Tap ▲▲▲▲"
							break
					case 10:
					// 4 times
							statValue = "Tap ▼▼▼▼"
							break

					case 11: 
					// 5 times
							statValue = "Tap ▲▲▲▲▲"
							break

					case 12: 
					// 5 times
							statValue = "Tap ▼▼▼▼▼ "
							break

					default:
					// unexpected case
						log.debug("unexpected button number: $btnNum")
					break
				 }
	}

    sendEvent(name: "pushed", value: btnNum,data: [buttonNumber: btnNum], descriptionText: "$device.displayName button $btnNum was pushed",  type: "$buttonType", isStateChange: true)
	sendEvent(name: "btnStat", value: statValue, isStateChange: true)
	if(needsCmds){
		delayBetween(cmds,500)}
}

def updated() {
    log.info "Updating..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def cmds = []

	// Set Paddle orientation
	if (reverseSwitch=="1") {
		cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
    } else {
		cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
    }

	// Set bottom LED
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
	
	// Set default color
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

// dummy setLevel command with duration for compatibility with non-Homeseer hubs
def setLevel(value, duration) {
	setLevel(value)
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

	def result = []

    result += response(zwave.basicV1.basicSet(value: level))
    result += response("delay 5000")
    result += response(zwave.switchMultilevelV1.switchMultilevelGet())
    result += response("delay 5000")
    result += response(zwave.switchMultilevelV1.switchMultilevelGet())
	
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

/*
 *  Set dimmer to status mode, then set the color of the individual LED
 *
 *  led = 1-7
 *  color = 0=0ff
 *          1=red
 *          2=green
 *          3=blue
 *          4=magenta
 *          5=yellow
 *          6=cyan
 *          7=white
 */

def setBlinkDurationMilliseconds(newBlinkDuration) {
    def cmds = []
    if (0 < newBlinkDuration && newBlinkDuration < 25500) {
        log.debug "setting blink duration to: ${newBlinkDuration} ms"
        state.blinkDuration = newBlinkDuration.toInteger() / 100
        log.debug "blink duration config parameter 30 is: ${state.blinkDuration}"
        cmds << zwave.configurationV2.configurationSet(configurationValue: [state.blinkDuration.toInteger()], parameterNumber: 30, size: 1).format()
    } else {
        log.debug "commanded blink duration ${newBlinkDuration} is outside range 0 … 25500 ms"
    }
    return cmds
}

def setStatusLed(led, color, blink) {

    def cmds = []
	led = led.toInteger()
	color = color.toInteger()
	blink = blink.toInteger()
	
    if (state.statusled1 == null) {
        state.statusled1 = 0
        state.statusled2 = 0
        state.statusled3 = 0
        state.statusled4 = 0
        state.blinkval = 0
    }

    /* set led # and color */
    switch (led) {
        case 1:
            state.statusled1 = color
            break
        case 2:
            state.statusled2 = color
            break
        case 3:
            state.statusled3 = color
            break
        case 4:
            state.statusled4 = color
            break
        case 0:
        case 5:
            // Special case - all LED's
            state.statusled1 = color
            state.statusled2 = color
            state.statusled3 = color
            state.statusled4 = color
            break
    }	

    if (state.statusled1 == 0 && state.statusled2 == 0 && state.statusled3 == 0 && state.statusled4 == 0 && state.statusled5 == 0 && state.statusled6 == 0 && state.statusled7 == 0) {
        // no LEDS are set, put back to NORMAL mode
        cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()
    } else {
        // at least one LED is set, put to status mode
        cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
    }

    if (led == 5 | led == 0) {
        for (def ledToChange = 1; ledToChange <= 4; ledToChange++) {
            // set color for all LEDs
            cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: ledToChange + 20, size: 1).format()
        }
    } else {
        // set color for specified LED
        cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: led + 20, size: 1).format()
    }

    // check if LED should be blinking
    def blinkval = state.blinkval
	if (logEnable) log.debug "current blinkval $blinkval , blink setting $blink)"

    if (blink) {
        switch (led) {
            case 1:
                blinkval = blinkval | 0x1
                break
            case 2:
                blinkval = blinkval | 0x2
                break
            case 3:
                blinkval = blinkval | 0x4
                break
            case 4:
                blinkval = blinkval | 0x8
                break
            case 0:
            case 5:
                blinkval = 0x7F
                break
        }
        cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
        state.blinkval = blinkval
        // set blink frequency if not already set, 5=500ms
        if (state.blinkDuration == null | state.blinkDuration < 0 | state.blinkDuration > 255) {
            cmds << zwave.configurationV2.configurationSet(configurationValue: [5], parameterNumber: 30, size: 1).format()
        }
    } else {

        switch (led) {
            case 1:
                blinkval = blinkval & 0xFE
                break
            case 2:
                blinkval = blinkval & 0xFD
                break
            case 3:
                blinkval = blinkval & 0xFB
                break
            case 4:
                blinkval = blinkval & 0xF7
                break
            case 0:
            case 5:
                blinkval = 0
                break
        }
        cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
        state.blinkval = blinkval
		if (logEnable) log.debug "New blinkval $blinkval"
    }
    delayBetween(cmds, 150)	

	}

/*
 * Set Dimmer to Status mode (exit normal mode)
 *
 */

def setLEDModeToNormal() {
	if (logEnable) log.debug "LED Mode set to Normal"
    def cmds = []
	sendEvent(name: "ledMode", value: "Normal",isStateChange: true)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()
    delayBetween(cmds, 500)
}

/*
 * Set the color of the LEDS for normal dimming mode, shows the current dim level
 */
def setLEDModeToStatus() {
	if (logEnable) log.debug "LED Mode Set to Status"
    def cmds = []
	sendEvent(name: "ledMode", value: "Status",isStateChange: true)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
    delayBetween(cmds, 500)
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    log.debug("sceneNumber: ${cmd.sceneNumber} keyAttributes: ${cmd.keyAttributes}")
    def result = []

    switch (cmd.sceneNumber) {
        case 1:
            // Up
            switch (cmd.keyAttributes) {
                case 0:
                    // Press Once
                    result += createEvent(push("physical",7))
                    result += createEvent([name: "switch", value: "on", type: "physical"])

                    if (singleTapToFullSpeed) {
                        //result += setLevel(99)
						result += setSpeed("high")
                        result += response("delay 5000")
                        result += response(zwave.switchMultilevelV1.switchMultilevelGet())
                    }
                    break
                case 1:
                    result = createEvent([name: "switch", value: "on", type: "physical"])
                    break
                case 2:
                    // Hold
                    result += createEvent(push("physical",5))
                    result += createEvent([name: "switch", value: "on", type: "physical"])
                    break
                case 3:
                    // 2 Times
                    result += createEvent(push("physical",1))
                    if (doubleTapToFullSpeed) {
						result += setSpeed("high")
                        result += response("delay 5000")
                        result += response(zwave.switchMultilevelV1.switchMultilevelGet())
                    }
                    break
                case 4:
                    // 3 times
                    result += createEvent(push("physical",3))
                    break
                case 5:
                    // 4 times
                    result += createEvent(push("physical",9))
                    break
                case 6:
                    // 5 times
                    result += createEvent(push("physical",11))
                    break
                default:
                    log.debug("unexpected up press keyAttribute: $cmd.keyAttributes")
            }
            break

        case 2:
            // Down
            switch (cmd.keyAttributes) {
                case 0:
                    // Press Once
                    result += createEvent(push("physical",8))
                    result += createEvent([name: "switch", value: "off", type: "physical"])
                    break
                case 1:
                    result = createEvent([name: "switch", value: "off", type: "physical"])
                    break
                case 2:
                    // Hold
                    result += createEvent(push("physical",6))
                    result += createEvent([name: "switch", value: "off", type: "physical"])
                    break
                case 3:
                    // 2 Times
                    createEvent(push("physical",2))
                    if (doubleTapDownToDim) {
                        result += setSpeed("low")
                        result += response("delay 5000")
                        result += response(zwave.switchMultilevelV1.switchMultilevelGet())
                    }
                    break
                case 4:
                    // 3 Times
                    result += createEvent(push("physical",4))
                    break
                case 5:
                    // 4 Times
                    result += createEvent(push("physical",10))
                    break
                case 6:
                    // 5 Times
                    result += createEvent(push("physical",12))
                    break
                default:
                    log.debug("unexpected down press keyAttribute: $cmd.keyAttributes")
            }
            break

        default:
            // unexpected case
            log.debug("unexpected scene: $cmd.sceneNumber")

    }
    return result
}

def refresh() {
	log.info "refresh() was called"
	
	def cmds = []
	cmds << zwave.switchBinaryV1.switchBinaryGet().format()
	cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	cmds << zwave.versionV1.versionGet().format()
	delayBetween(cmds,1000)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
