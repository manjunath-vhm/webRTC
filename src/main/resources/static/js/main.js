var signalling = function () {

    var serverConnection;

    var init = function (onMessageCallback) {
        var serverUri = 'ws://' + window.location.host + '/signal';
        serverConnection = new WebSocket(serverUri);

        serverConnection.onopen = function (evt) {
            console.log('Connected to signalling server: ' + JSON.stringify(evt));
        };
        serverConnection.onclose = function (evt) {
            console.log('Connection with signalling server closed: ' + JSON.stringify(evt));
        };
        serverConnection.onmessage = function (evt) {
            // console.log('Message from signalling server: ' + evt.data);
            var payload = JSON.parse(evt.data);
            onMessageCallback(payload);
        };
        serverConnection.onerror = function (evt) {
            console.log('Signalling error: ' + JSON.stringify(evt));
        }
    };

    var send = function (payload) {
        var msg = JSON.stringify(payload);
        console.log("Sending message: " + msg);
        serverConnection.send(msg)
    };

    return {
        connect: init,
        send: send
    };

}();


var firstInMeeting = false;
var setupDone = false;


// OnMessage handler
function handleMessage(payload) {
    // console.log("Received signalling message: " + JSON.stringify(payload));
    switch (payload.type) {
        case 'created':
            firstInMeeting = true;
            gotoLobby();
            break;
        case 'remotePeerJoining':
            if (!setupDone) {
                startMeeting();
                setupWebRTC();
                setupDone = true;
            }
            break;
        case 'offer':
            handleOffer(payload);
            break;
        case 'answer':
            handleAnswer(payload);
            break;
        defaut:
            break;
    }

}

function initialize() {
    // Connect to signalling server
    signalling.connect(handleMessage);
}

var meetingID;
var userName;

function signIn() {
    // Get user name and meeting ID from the form
    userName = document.getElementById("inputUsername").value;
    meetingID = document.getElementById("inputMeetingId").value;

    // Sign in to the meeting
    var signIn = {
        type: "join",
        name: userName,
        meeting: meetingID
    };
    signalling.send(signIn);

}


var webRTC = function () {
    function hasUserMedia() {
        navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia || navigator.msGetUserMedia;
        return !!navigator.getUserMedia;
    }

    function hasRTCPeerConnection() {
        window.RTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
        return !!window.RTCPeerConnection;
    }

    return {
        supported: function () {
            return hasUserMedia() && hasRTCPeerConnection();
        }
    }
}();


var rtcPeerConnection;
var myLocalStream;

function setupWebRTC() {
    console.log("Setting up WebRTC...");
    if (webRTC.supported()) {

        // var configuration = null; // { "iceServers": [{ "url": "stun:127.0.0.1:9876" }] };
        var configuration = { "iceServers": [{ urls: 'stun:stun.l.google.com:19302' }] };  // Google's public STUN server
        rtcPeerConnection = new RTCPeerConnection(configuration);


        rtcPeerConnection.onicecandidate = function(iceEvent) {
            if (!iceEvent || ! iceEvent.candidate) return;
            console.log("On ice candidate: " + JSON.stringify(iceEvent));
            rtcPeerConnection.addIceCandidate(iceEvent.candidate);
        };

        // if firstInMeeting, then this client will start to createOffer process
        if (firstInMeeting) {
            rtcPeerConnection.onnegotiationneeded = function() {
                createOffer();
            }
            addTrack();
        }

        rtcPeerConnection.ontrack = function(e) {
            if (!e) return;

            console.log("ONTRACK CALLED");
            var theirVideo = document.getElementById("their-video");
            if (theirVideo.srcObject !== e.streams[0]) {
                theirVideo.srcObject = e.streams[0];
                console.log('pc2 received remote stream');
            }
        };




    } else {
        alert("Sorry, your browser does not support WebRTC.");
    }
}

function addTrack() {
    navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
    }).then(function(stream) {
        myLocalStream = stream;
        console.log("About to stream local video");
        var myVideo = document.getElementById("my-video");
        myVideo.srcObject = stream;
        myVideo.onloadedmetadata = function () {
            myVideo.play();
        };
        // rtcPeerConnection.addStream(stream);
        stream.getTracks().forEach(function (track) {
            rtcPeerConnection.addTrack(track, stream);
        });
    }, function(error) {
        console.log(error);
    });
}


function createOffer() {
    // The caller calls RTCPeerConnection.createOffer() to create an offer.
    rtcPeerConnection.createOffer()
        .then(function (offer) {
            // The caller calls RTCPeerConnection.setLocalDescription() to set that offer as the local description (that is, the description of the local end of the connection).
            return rtcPeerConnection.setLocalDescription(offer);
        })
        .then(function () {
            // The caller uses the signaling server to transmit the offer to the intended receiver of the call.
            signalling.send({
                type: "offer",
                name: userName,
                meeting: meetingID,
                data: rtcPeerConnection.localDescription
            });
        })
        .catch(function (reason) {
            console.log("An error occurred sending the offer: " + reason);
        });
}

function handleOffer(payload) {
    // The recipient receives the offer and calls RTCPeerConnection.setRemoteDescription() to record it as the remote description (the description of the other end of the connection).
    console.log("Received offer: " + JSON.stringify(payload));
    rtcPeerConnection.setRemoteDescription(payload.data).then(function () {
        // TODO The recipient does any setup it needs to do for its end of the call, including adding outgoing streams to the connection.
        //...
        addTrack();

        console.log("About to create answer...");
        // The recipient then creates an answer by calling RTCPeerConnection.createAnswer().
        rtcPeerConnection.createAnswer()
            .then(function (answer) {
                console.log("Created answer: " + answer);
                // The recipient calls RTCPeerConnection.setLocalDescription() to set the answer as its local description. The recipient now knows the configuration of both ends of the connection.
                rtcPeerConnection.setLocalDescription(answer);
            })
            .then(function () {
                // The recipient uses the signaling server to send the answer to the caller.
                console.log("About to send answer...");
                signalling.send({
                    type: "answer",
                    name: userName,
                    meeting: meetingID,
                    data: rtcPeerConnection.localDescription
                });
            })
            .catch(function (reason) {
                console.log("An error occurred sending the answer: " + reason);
            })

    }).catch(function (reason) {
        console.log("An error occurred setting the remote description: " + reason);
    });
}

function handleAnswer(payload) {
    // The caller receives the answer.
    console.log("Received answer: " + JSON.stringify(payload));

    // The caller calls RTCPeerConnection.setRemoteDescription() to set the answer as the remote description for its end of the call. It now knows the configuration of both peers. Media begins to flow as configured.
    rtcPeerConnection.setRemoteDescription(payload.data)
        .then(function(){
            //
        })
        .catch(function(reason){
            console.log("An error occurred setting the remote description (handleAsnwer(): " + reason);
        });
}

