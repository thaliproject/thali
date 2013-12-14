/**
 * Created by yarong on 12/13/13.
 */

var successCount = 0;

var successPing = function(responseString) {
    var responseObject = JSON.parse(responseString);
    if (responseObject != "Pong") {
        throw "Expecting Pong! But got " + responseObject;
    }
    ++successCount;
}

var failCallBack = function(responseString) {
    throw "Was not supposed to be called, got: " + responseString;
}

window.ThaliBridgeCallOnce("Test","Ping", successPing, failCallBack);
window.ThaliBridgeCallOnce("Test","Ping"+successCount, failCallBack, successPing);
// Eventually we'll introduce one way update calls but for now, not.
window.ThaliBridgeCallOnce("Test","Ping"+successCount, failCallBack, failCallBack);