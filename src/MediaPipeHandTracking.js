class MediaPipeHandTracking {
    constructor(canvas_width, canvas_height) {
        this.canvas_width = canvas_width;
        this.canvas_height = canvas_height;
        this.listeners = {};
        this.videoElement = document.getElementById('camera-feed');

        this.hands = null;
        this.camera = null;
        this.initialized = false;
        this.isProcessing = false;

        this.initialize();
    }

    async initialize() {
        try {
            if (!window.Hands) {
                console.error("MediaPipe Hands is not loaded");
                return;
            }

            this.hands = new Hands({
                locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`
            });

            this.hands.setOptions({
                maxNumHands: 1,
                modelComplexity: 0,  // Giảm độ phức tạp để tăng hiệu năng
                minDetectionConfidence: 0.5,
                minTrackingConfidence: 0.5
            });

            this.hands.onResults(this.processHandResults.bind(this));

            this.initialized = true;
            console.log("MediaPipe Hands initialized successfully");
        } catch (error) {
            console.error("Error initializing MediaPipe Hands:", error);
        }
    }

    startProcessing() {
        if (!this.hands || !this.videoElement || !this.videoElement.srcObject || !this.initialized) {
            console.error("Cannot start MediaPipe processing - missing components");
            return;
        }

        if (this.camera) {
            this.camera.stop();
        }

        try {
            this.camera = new Camera(this.videoElement, {
                onFrame: this.processFrame.bind(this),
                width: this.canvas_width,
                height: this.canvas_height
            });

            this.camera.start();
        } catch (error) {
            console.error("Error starting MediaPipe Camera:", error);
        }
    }

    async processFrame() {
        // Ngăn chặn việc xử lý song song
        if (this.isProcessing) return;
        
        this.isProcessing = true;
        try {
            if (this.hands) {
                await this.hands.send({ image: this.videoElement });
            }
        } catch (error) {
            console.error("Frame processing error:", error);
        } finally {
            this.isProcessing = false;
        }
    }

    processHandResults(results) {
        if (!results.multiHandLandmarks || results.multiHandLandmarks.length === 0) return;

        const landmarks = results.multiHandLandmarks[0];
        const wrist = landmarks[0];
        const middleFingerBase = landmarks[9];

        const palmCenterX = (wrist.x + middleFingerBase.x) / 2;
        const palmCenterY = (wrist.y + middleFingerBase.y) / 2;

        const x = palmCenterX * this.canvas_width;
        const y = palmCenterY * this.canvas_height;
        const mirroredX = this.canvas_width - x;

        requestAnimationFrame(() => {
            this.dispatchEvent('handmove', { x: mirroredX, y: y });
        });
    }

    addEventListener(type, listener) {
        (this.listeners[type] = this.listeners[type] || []).push(listener);
    }

    removeEventListener(type, listener) {
        if (!this.listeners[type]) return;
        this.listeners[type] = this.listeners[type].filter(l => l !== listener);
    }

    dispatchEvent(type, event) {
        const listenerArray = this.listeners[type];
        if (!listenerArray) return;

        event.target = this;
        listenerArray.forEach(listener => listener.call(this, event));
    }
}