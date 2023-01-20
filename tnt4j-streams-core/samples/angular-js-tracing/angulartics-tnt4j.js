/*
 * Copyright 2014-2023 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

'use strict';

var plugin = angular.module ('angulartics.tnt4j', ['angulartics']);

plugin.factory ('Tnt4jStreamsService', function ($http, $document) {
    return {
        sendPath: function (path) {
            this.sendEventToStream (this.event (path, null, null));
        },

        sendAction: function (action, properties) {
            this.sendEventToStream (this.event (null, action, properties));
        },

        event: function (path, action, properties) {
            var ret = {
                browser: navigator.userAgent,
                url: "path",
                eventName: "pageVisit",
                properties: "",
                rid: ((document.getElementById ('corrid') || {}).value) || "",
                sid: ((document.getElementById ('rcorrid') || {}).value) || "",
                pageLoad: (window.performance.timing.domComplete - window.performance.timing.fetchStart),
                timestamp: Date.now ()
            };
            ret.url = path || "";
            ret.eventName = action || "";
            ret.properties = properties || "";
            return ret;
        },

        sendEventToStream: function (dataM) {
            console.log (dataM);
            console.log (JSON.stringify (dataM));
            return $http ({
                              url: "http://localhost:9595",
                              data: JSON.stringify (dataM),
                              method: "POST",
                              headers: {"Content-Type": "text/plain"}
                          });
        }
    }
});

plugin.config (['$analyticsProvider', 'Tnt4jStreamsServiceProvider', function ($analyticsProvider, Tnt4jStreamsServiceProvider) {
        var Tnt4jStreamsService = Tnt4jStreamsServiceProvider.$get ();
        $analyticsProvider.registerPageTrack (function (path) {
            Tnt4jStreamsService.sendPath (path);
        });

        $analyticsProvider.registerEventTrack (function (action, properties) {
            Tnt4jStreamsService.sendAction (action, properties);
        });
    }]);

 
