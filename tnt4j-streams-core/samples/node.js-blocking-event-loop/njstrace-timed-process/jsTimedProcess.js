/*
 * Copyright 2014-2022 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var idgen = require ('idgen');

/**
 * Default constructor. Timeout passed as argument
 */
function JsTimedProcess(args)  {

    // setTimeout() callback
	    function timeoutCallback () {
        if (this.ended === undefined) this.end ();
				if (this.isTimedOut()) {
					if (__njsTraceExit__ === undefined) {
                console.log ("NJS trace undefined");
            }
					__njsTraceExit__({entryData: this.entryData, exception : "Function timed out", line : this.line, returnValue : (this.elapsed() + " of " + JsTimedProcess.tmout)});
            //console.log("Function timeout reatched: " + this.id + " started: " + this.start + " Elapsed: " + this.elapsed() + "Line " + this.line );
        }
    };
    // end of setTimeout() callback

		if (args.entryData === undefined) {
        console.log ("entry data undefined");
    }
    this.entryData = args.entryData;
    this.line = args.line;
    this.exitData = undefined;
		this.id = (idgen());	
		this.start = (new Date().getTime());
		if (JsTimedProcess.tmout === undefined) {
        this.fTimeoutValue = 300; //default timeout value in ms
		} else {
        this.fTimeoutValue = JsTimedProcess.tmout;
    }
    this.ended = undefined;
    this.timeout = setTimeout (timeoutCallback.bind (this), JsTimedProcess.tmout);
		JsTimedProcess.timeouts[this.id] = (this.timeout);
}

JsTimedProcess.timeouts = [];
JsTimedProcess.tmout = undefined;

JsTimedProcess.prototype.elapsed = function() {
			if (this.start === undefined) {
        console.log ("Start undefined");
    }
			if (this.ended === undefined) {
        console.log ("End undefined");
    }
    return this.ended - this.start;
};

JsTimedProcess.prototype.isTimedOut = function() {
			var result = (this.elapsed()) >= this.fTimeoutValue;
    //console.log(result + "end: " + this.ended + " start: " + this.start + " timeout: " + this.fTimeoutValue);
    return result;
};

JsTimedProcess.prototype.end = function() {
	this.ended = (new Date().getTime());
    var timeout = JsTimedProcess.timeouts[this.id];
	if (timeout === undefined || this.start === undefined || this.id === undefined) {
        console.log ("error");
    }
    // Ensure timeout cleared before it processed in node.js event loop
	process.nextTick(function(timeout2, jsProcess) {
		if (!jsProcess.isTimedOut()) {
                              timeout2.unref ();
                              clearTimeout (timeout);
                              delete JsTimedProcess.timeouts[jsProcess.id];
                          }
                      }, timeout, this);
};

module.exports = function() { 
		if (!(arguments === undefined)) {
        JsTimedProcess.tmout = arguments['0'];
    }
    return JsTimedProcess;
}
