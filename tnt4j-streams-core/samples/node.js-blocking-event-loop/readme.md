# Introduction

This sample demonstrates how his Fibonacci number calculation written in `node.js` has abysmal performance.

## Prerequisites

`node.js` needs to be installed in your system.

# Installing and running

To install:

1) This command get's all dependencies needed
```cmd
npm install
```

2) To run provided sample run TNT4J-steams with provided parser configuration tnt-data-source.xml.
O simply start run.bat (run.sh in Linux)

3) run sample 
```cmd
npm start
```

# Apply to other `node.js` software

You need to inject 3 objects: JsTimedProcess, Tnt4jStreamsFormatter and `njsTrace`.

JsTimedProcess comes with timeout option (in this case 10);
If you want to send only error traces to [meshIQ](https://www.meshiq.com) use option "true" in Tnt4jStreamsFormatter;
`njsTrace` comes with filter witch files to instrument, and if function arguments should be inspected;

```js
JsTimedProcess = require('njstrace-timed-process')(10); // Timeout value
var Tnt4jStreamsFormatter = require('njstrace-tnt4j-formatter').Tnt4jStreamsFormatter;							// ################# Tnt4JFormatter
var njstrace = require('njstrace').inject({	formatter: new Tnt4jStreamsFormatter("true"), inspectArgs: false, files: ['**/*.js', '*.js', '!**/node_modules/**'],});
```

# Note

This sample uses customized `njsTrace` lib.