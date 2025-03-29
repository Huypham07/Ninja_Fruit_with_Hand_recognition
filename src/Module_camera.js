// Module để xử lý camera làm background
(function () {
  var videoElement;

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

    console.log("Camera module initialized, video element created with id:", videoElement.id);
    toggleCameraBackground(isUsingCamera);
  };

  toggleHandTrackingMediaPipe = function (enable) {
    console.log("toggleHandTrackingMediaPipe called with enable:", enable);
    isHandTrackingEnabled = enable;

    if (isHandTrackingEnabled) {
      console.log("Attempting to initialize MediaPipe hand tracking");
      // Initialize MediaPipe hand tracking
      if (!mediaHandTracking) {
        console.log("Creating new MediaPipeHandTracking instance");
        mediaHandTracking = new MediaPipeHandTracking(topCanvas.width, topCanvas.height);

        // Set the video source for MediaPipe
        if (videoElement && videoElement.srcObject) {
          console.log("Video element has stream, starting hand tracking");
          mediaHandTracking.videoElement = videoElement;
          // Use setTimeout to allow the UI to update before starting processing
          setTimeout(() => {
            mediaHandTracking.startProcessing();
            console.log("Adding handmove event listener");
            mediaHandTracking.addEventListener("handmove", handmove);
          }, 100);
        } else {
          console.error("Video element has no stream, hand tracking cannot start");
          return;
        }
      }
      // Set this after all initialization to avoid affecting game state
      isDragging = false; // Disable mouse dragging
    } else {
      console.log("Disabling MediaPipe hand tracking");
      // Stop MediaPipe hand tracking
      if (mediaHandTracking) {
        mediaHandTracking.removeEventListener("handmove", handmove);
        mediaHandTracking = null;
      }
      isDragging = true; // Enable mouse dragging
    }
  };

  toggleCameraBackground = function (useCamera) {
    console.log("toggleCameraBackground called with useCamera:", useCamera);
    isUsingCamera = useCamera;

    if (isUsingCamera) {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        console.log("Requesting camera access");
        navigator.mediaDevices
          .getUserMedia({
            video: {
              facingMode: "user",
              width: { ideal: gameWidth },
              height: { ideal: gameHeight },
            },
          })
          .then(function (stream) {
            console.log("Camera access granted, setting up video stream");
            videoElement.srcObject = stream;
            videoElement.style.display = "block"; // Make sure it's visible for MediaPipe
            bottomCanvas.style.backgroundImage = "none";

            if (isHandTrackingEnabled && mediaHandTracking) {
              mediaHandTracking.videoElement = videoElement;
              mediaHandTracking.startProcessing();
            }
          })
          .catch(function (error) {
            console.error("Không thể truy cập camera: ", error);
            isUsingCamera = false;

            // If camera fails, disable hand tracking as well
            if (typeof window.Module_setting !== 'undefined' && window.Module_setting.getSettings().handTrackingEnabled) {
              window.Module_setting.setHandTrackingEnabled(false);
            }
          });
      }
    } else {
      console.log("Turning off camera");
      if (videoElement.srcObject) {
        videoElement.srcObject.getTracks().forEach((track) => track.stop());
        videoElement.srcObject = null;
      }
      videoElement.style.display = "none";
      bottomCanvas.style.backgroundImage = "url(assets/bg.jpg)";

      // Make sure hand tracking is disabled when camera is turned off
      if (typeof window.Module_setting !== 'undefined' && window.Module_setting.getSettings().handTrackingEnabled) {
        window.Module_setting.setHandTrackingEnabled(false);
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
