/**
 * Created by 教兵 on 14-3-14.
 */
define([
    "dojo/_base/declare"
], function(declare) {
    return declare("com.tpsoft.notification-push.Login", null, {
        redis: null,
        constructor: function(args) {
            declare.safeMixin(this, args);
        },
        checkAppId: function(appId, password) {

        }
    });
});