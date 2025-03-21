// Module để xử lý camera làm background
(function () {
  var videoElement;
  var switchButton;

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

  // Bật/tắt camera và cập nhật background
  toggleCameraBackground = function (useCamera) {
    isUsingCamera = useCamera;

    if (isUsingCamera) {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices
          .getUserMedia({
            video: {
              facingMode: "user", // Nếu bị mirror, đổi thành "environment"
              width: { ideal: gameWidth },
              height: { ideal: gameHeight },
            },
          })
          .then(function (stream) {
            videoElement.srcObject = stream;
            videoElement.style.display = "none";
            bottomCanvas.style.backgroundImage = "none";
              switchButton.textContent = "Use Image";
          })
          .catch(function (error) {
            console.error("Không thể truy cập camera: ", error);
            isUsingCamera = false;
            switchButton.textContent = "Camera Error";
            setTimeout(() => (switchButton.textContent = "Use Camera"), 2000);
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
