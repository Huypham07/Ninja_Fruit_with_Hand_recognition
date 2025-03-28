// Module để xử lý camera làm background
(function () {
  var videoElement;
  var switchButton;
  var handTrackingButton;
  var isHandTrackingEnabled = false;

  // Khởi tạo camera
  initCamera = function () {
    // Tạo video element
    videoElement = document.createElement("video");
    videoElement.id = "camera-feed";
    videoElement.autoplay = true;
    videoElement.style.display = "none";
    videoElement.style.position = "absolute";
    videoElement.style.left = "50%";
    videoElement.style.top = "50%";
    videoElement.style.width = gameWidth + "px";
    videoElement.style.height = gameHeight + "px";
    videoElement.style.transform = "translate(-50%, -50%)";
    videoElement.style.zIndex = "-1";
    document.body.appendChild(videoElement);

    createSwitchButton();
    createHandTrackingButton();
    toggleCameraBackground(isUsingCamera);
  };

  createSwitchButton = function () {
    switchButton = document.createElement("button");
    switchButton.id = "switch-background";
    switchButton.textContent = "Use Camera";
    switchButton.style.position = "absolute";
    switchButton.style.bottom = "10px";
    switchButton.style.left = "10px";
    switchButton.style.zIndex = "100";
    switchButton.style.padding = "8px 16px";
    switchButton.style.borderRadius = "4px";
    switchButton.style.border = "none";
    switchButton.style.backgroundColor = "#f6c223";
    switchButton.style.color = "#000";
    switchButton.style.cursor = "pointer";
    switchButton.style.fontWeight = "bold";

    switchButton.addEventListener("click", function () {
      toggleCameraBackground(!isUsingCamera);
    });

    document.body.appendChild(switchButton);
  };

  createHandTrackingButton = function () {
    handTrackingButton = document.createElement("button");
    handTrackingButton.id = "hand-tracking-toggle";
    handTrackingButton.textContent = "Use Hand Tracking";
    handTrackingButton.style.position = "absolute";
    handTrackingButton.style.bottom = "10px";
    handTrackingButton.style.left = "150px";
    handTrackingButton.style.zIndex = "100";
    handTrackingButton.style.padding = "8px 16px";
    handTrackingButton.style.borderRadius = "4px";
    handTrackingButton.style.border = "none";
    handTrackingButton.style.backgroundColor = "#f6c223";
    handTrackingButton.style.color = "#000";
    handTrackingButton.style.cursor = "pointer";
    handTrackingButton.style.fontWeight = "bold";
    handTrackingButton.style.display = "none"; // Hide by default

    handTrackingButton.addEventListener("click", function () {
      toggleHandTracking(!isHandTrackingEnabled);
    });

    document.body.appendChild(handTrackingButton);
  };

  // Toggle hand tracking
  toggleHandTracking = function (enable) {
    isHandTrackingEnabled = enable;

    if (isHandTrackingEnabled) {
      // Initialize MediaPipe hand tracking
      if (!mediaHandTracking) {
        mediaHandTracking = new MediaPipeHandTracking(topCanvas.width, topCanvas.height);

        // Set the video source for MediaPipe
        if (videoElement && videoElement.srcObject) {
          mediaHandTracking.videoElement = videoElement;
          mediaHandTracking.startProcessing();
        }

        mediaHandTracking.addEventListener("handmove", handmove);
      }

      // Update button text
      handTrackingButton.textContent = "Use Mouse";
      isDragging = false; // Disable mouse dragging
    } else {
      handTrackingButton.textContent = "Use Hand Tracking";
    }
  };

  // Bật/tắt camera và cập nhật background
  toggleCameraBackground = function (useCamera) {
    isUsingCamera = useCamera;

    if (isUsingCamera) {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices
          .getUserMedia({
            video: {
              facingMode: "user",
              width: { ideal: gameWidth },
              height: { ideal: gameHeight },
            },
          })
          .then(function (stream) {
            videoElement.srcObject = stream;
            videoElement.style.display = "block"; // Make sure it's visible for MediaPipe
            bottomCanvas.style.backgroundImage = "none";
            switchButton.textContent = "Use Image";

            // Show hand tracking toggle button
            handTrackingButton.style.display = "block";

            // If MediaPipe hand tracking is enabled, initialize it with the new stream
            if (isHandTrackingEnabled && mediaHandTracking) {
              mediaHandTracking.videoElement = videoElement;
              mediaHandTracking.startProcessing();
            }
          })
          .catch(function (error) {
            console.error("Không thể truy cập camera: ", error);
            isUsingCamera = false;
            switchButton.textContent = "Camera Error";
            setTimeout(() => (switchButton.textContent = "Use Camera"), 2000);
            handTrackingButton.style.display = "none";
          });
      }
    } else {
      if (videoElement.srcObject) {
        videoElement.srcObject.getTracks().forEach((track) => track.stop());
        videoElement.srcObject = null;
      }
      videoElement.style.display = "none";
      bottomCanvas.style.backgroundImage = "url(assets/bg.jpg)";
      switchButton.textContent = "Use Camera";

      // Hide hand tracking toggle button and disable hand tracking
      handTrackingButton.style.display = "none";
      if (isHandTrackingEnabled) {
        toggleHandTracking(false);
      }
    }
  };

  // Render camera frame vào background nếu đang sử dụng camera
  renderCameraBackground = function () {
    if (isUsingCamera && videoElement.srcObject) {
      bottomContext.scale(-1, 1);
      bottomContext.drawImage(videoElement, -gameWidth, 0, gameWidth, gameHeight);
    }
  };
})();
