/**
 * Module_setting - Handles game settings and their UI controls
 */

const Module_setting = (function () {
    // Private variables
    let settingsToggle;
    let settingsContent;
    let soundToggle;
    let backgroundToggle;
    let cameraToggle;
    let handTrackingToggle;
    let countdownElement;
    let overlayElement;
    let backgroundModal;
    let closeBgModal;

    // Game settings state
    const gameSettings = {
        soundEnabled: true,
        cameraEnabled: false,
        handTrackingEnabled: false,
        gamePaused: false,
        backgroundModalEnabled: false,
    };

    // Private methods
    function createOverlay() {
        overlayElement = document.createElement('div');
        overlayElement.id = 'settings-overlay';
        overlayElement.style.position = 'fixed';
        overlayElement.style.top = '0';
        overlayElement.style.left = '0';
        overlayElement.style.width = '100%';
        overlayElement.style.height = '100%';
        overlayElement.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
        overlayElement.style.zIndex = '900';  // Below settings panel but above game
        overlayElement.style.display = 'none';
        document.body.appendChild(overlayElement);
    }

    let countdownInterval = null;

    function toggleSettings(show) {
        settingsContent.style.display = show ? 'block' : 'none';
        overlayElement.style.display = show ? 'block' : 'none';

        if (show) {
            if (countdownInterval) {
                clearInterval(countdownInterval);
                countdownInterval = null;

                const countdown = document.getElementById('countdown');
                if (countdown) {
                    countdown.style.display = 'none';
                }
            }

            gameSettings.gamePaused = true;
            pauseGame();
        } else {
            // Close background modal if it's open
            if (backgroundModal && backgroundModal.style.display === 'block') {
                backgroundModal.style.display = 'none';
            }
            
            // Only show countdown if game is in progress
            if (window.game && window.game.isGameStarted) {
                showCountdown(() => {
                    gameSettings.gamePaused = false;
                    resumeGame();
                });
            } else {
                // If game hasn't started yet, just resume without countdown
                gameSettings.gamePaused = false;
                resumeGame();
            }
        }
    }

    function toggleSound(enabled) {
        gameSettings.soundEnabled = enabled;

        // Interact with SoundJS implementation
        if (window.createjs && createjs.Sound) {
            createjs.Sound.muted = !enabled;
        }
        console.log('Sound ' + (enabled ? 'enabled' : 'disabled'));
    }

    function toggleCamera(enabled) {
        gameSettings.cameraEnabled = enabled;

        console.log('Camera ' + (enabled ? 'enabled' : 'disabled'));

        // Call the toggleCameraBackground function from Module_camera.js
        if (typeof toggleCameraBackground === 'function') {
            // This calls the function from Module_camera.js
            toggleCameraBackground(enabled);
        } else {
            console.error('Camera module function not found');
        }

        // Update handtracking button state based on camera state
        updateHandTrackingButtonState(enabled);

        // If camera is disabled, also disable hand tracking
        if (!enabled && gameSettings.handTrackingEnabled) {
            handTrackingToggle.checked = false;
            toggleHandTracking(false);
        }
    }

    function toggleHandTracking(enabled) {
        // Only allow enabling hand tracking if camera is enabled
        if (enabled && !gameSettings.cameraEnabled) {
            console.warn('Cannot enable hand tracking without camera');
            handTrackingToggle.checked = false;
            return;
        }

        gameSettings.handTrackingEnabled = enabled;
        console.log('Hand tracking ' + (enabled ? 'enabled' : 'disabled'));

        // Call the function to toggle hand tracking in the MediaPipe module
        if (typeof toggleHandTrackingMediaPipe === 'function') {
            toggleHandTrackingMediaPipe(enabled);
        } else {
            console.error('Hand tracking function not found: toggleHandTrackingMediaPipe');
        }
    }

    function updateHandTrackingButtonState(cameraEnabled) {
        // Enable or disable the hand tracking toggle based on camera state
        if (handTrackingToggle) {
            if (cameraEnabled) {
                handTrackingToggle.disabled = false;
                handTrackingToggle.parentElement.parentElement.classList.remove('disabled');
                // Remove blur effect from label text
                const handTrackingLabel = handTrackingToggle.parentElement.querySelector('label');
                if (handTrackingLabel) {
                    handTrackingLabel.style.opacity = '1';
                    handTrackingLabel.style.filter = 'none';
                }
            } else {
                handTrackingToggle.disabled = true;
                handTrackingToggle.parentElement.parentElement.classList.add('disabled');
                // Add blur effect to label text
                const handTrackingLabel = handTrackingToggle.parentElement.querySelector('label');
                if (handTrackingLabel) {
                    handTrackingLabel.style.opacity = '0.5';
                    handTrackingLabel.style.filter = 'blur(0.5px)';
                }
            }
        }
    }

    // Animation frame ID for pause functionality
    let originalRequestAnimationFrame;
    let originalGameLoop;

    // Create or get countdown element
    function getCountdownElement() {
        if (!countdownElement) {
            countdownElement = document.createElement('div');
            countdownElement.id = 'game-countdown';
            countdownElement.style.position = 'fixed';
            countdownElement.style.left = '50%';
            countdownElement.style.top = '50%';
            countdownElement.style.transform = 'translate(-50%, -50%)';
            countdownElement.style.fontSize = '120px';
            countdownElement.style.fontWeight = 'bold';
            countdownElement.style.color = '#fff';
            countdownElement.style.textShadow = '2px 2px 10px rgba(0, 0, 0, 0.8)';
            countdownElement.style.zIndex = '1000';
            countdownElement.style.display = 'none';
            countdownElement.style.fontFamily = 'Arial, sans-serif';
            countdownElement.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
            countdownElement.style.padding = '20px 40px';
            countdownElement.style.borderRadius = '20px';
            document.body.appendChild(countdownElement);
        }
        return countdownElement;
    }

    // Show countdown before resuming the game
    function showCountdown(callback) {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }

        const countdown = document.getElementById('countdown');
        if (!countdown) {
            console.error('Countdown element not found');
            if (callback) callback();
            return;
        }

        countdown.style.display = 'block';
        let count = 3;
        countdown.textContent = count;

        countdownInterval = setInterval(() => {
            count--;
            if (count >= 0) {
                countdown.textContent = count > 0 ? count : 'GO!';
                countdown.style.fontSize = count > 0 ? '120px' : '150px';
                // Play sound for each number if sound is enabled
                if (gameSettings.soundEnabled && window.createjs && createjs.Sound) {
                    createjs.Sound.play('countdown');
                }
            } else {
                clearInterval(countdownInterval);
                countdownInterval = null;
                countdown.style.display = 'none';
                if (gameSettings.soundEnabled && window.createjs && createjs.Sound) {
                    createjs.Sound.play('start');
                }
                if (callback) callback();
            }
        }, 1000);
    }

    function togglePause(paused) {
        gameSettings.gamePaused = paused;
        console.log('Game ' + (paused ? 'paused' : 'resumed'));

        if (paused) {
            // Immediately pause the game
            pauseGame();
        } else {
            // Show countdown before resuming
            showCountdown(() => {
                resumeGame();
            });
        }
    }

    function pauseGame() {
        if (window.game) {
            window.game.isPaused = true;
            document.dispatchEvent(new CustomEvent('gamePaused'));
            console.log('Top and middle canvas animations paused');
        }
    }

    function resumeGame() {
        if (window.game) {
            window.game.isPaused = false;
            document.dispatchEvent(new CustomEvent('gameResumed'));
            console.log('Top and middle canvas animations resumed');
        }
    }

    function openBackgroundModal() {
        backgroundModal.style.display = 'block';
        backgroundModal.style.zIndex = '1001'; // Ensure it's above other elements
    }
    
    function closeBackgroundModal() {
        backgroundModal.style.display = 'none';
    }    

    function selectBackground(e) {
        const selectedImg = e.target;
        if (selectedImg && selectedImg.src) {
            console.log('Changing background to:', selectedImg.src);
            
            // Change the bottom canvas background image
            const bottomCanvas = document.getElementById('bottom');
            if (bottomCanvas) {
                bottomCanvas.style.backgroundImage = `url('${selectedImg.src}')`;
            }
            
            // Also apply background to body for full coverage
            document.body.style.backgroundImage = `url('${selectedImg.src}')`;
            document.body.style.backgroundSize = 'contain';
            document.body.style.backgroundPosition = 'center';
            document.body.style.backgroundRepeat = 'repeat';
            document.body.style.backgroundAttachment = 'fixed';
            
            // Don't close the modal when selecting background
            console.log('Background changed successfully');
        }
    }

    // Public interface
    return {
        init: function () {
            // Get DOM elements
            settingsToggle = document.getElementById('settings-toggle');
            settingsContent = document.getElementById('settings-content');
            soundToggle = document.getElementById('sound-toggle');
            cameraToggle = document.getElementById('camera-toggle');
            handTrackingToggle = document.getElementById('hand-tracking-toggle');
            backgroundToggle = document.getElementById('background-toggle');
            backgroundModal = document.getElementById('background-modal');
            closeBgModal = document.getElementById('close-bg-modal');

            // Create overlay and countdown elements
            createOverlay();
            getCountdownElement();

            // Set up event handlers
            settingsToggle.addEventListener('click', function () {
                const isShowing = settingsContent.style.display === 'block';
                toggleSettings(!isShowing);
            });
            
            soundToggle.addEventListener('change', function () {
                toggleSound(this.checked);
            });
            
            cameraToggle.addEventListener('change', function () {
                toggleCamera(this.checked);
            });
            
            handTrackingToggle.addEventListener('change', function () {
                toggleHandTracking(this.checked);
            });

            // Background toggle handler
            backgroundToggle.addEventListener('change', function () {
                if (this.checked) {
                    console.log('Opening background modal');
                    openBackgroundModal();
                } else {
                    console.log('Resetting background');
                    closeBackgroundModal();
                    
                    // Reset to default background
                    const bottomCanvas = document.getElementById('bottom');
                    if (bottomCanvas) {
                        bottomCanvas.style.backgroundImage = 'url(assets/bg.jpg)';
                    }
                    document.body.style.backgroundImage = '';
                }
            });

            // Close modal button handler
            closeBgModal.addEventListener('click', function () {
                closeBackgroundModal();
                // Always set toggle to false when closing
                backgroundToggle.checked = false;
                // Reset background if no background was selected
                if (!document.body.style.backgroundImage) {
                    const bottomCanvas = document.getElementById('bottom');
                    if (bottomCanvas) {
                        bottomCanvas.style.backgroundImage = 'url(assets/bg.jpg)';
                    }
                    document.body.style.backgroundImage = '';
                }
            });
            
            // Setup background selection click handlers
            const bgOptions = document.querySelectorAll('.bg-option');
            console.log('Found background options:', bgOptions.length);
            
            bgOptions.forEach(function (img) {
                // Ensure the image has loaded correctly
                if (img.complete && img.naturalHeight === 0) {
                    console.warn('Background image failed to load:', img.src);
                }
                
                // Add click event listener
                img.addEventListener('click', selectBackground);
            });

            // Initial state setup
            updateHandTrackingButtonState(gameSettings.cameraEnabled);

            // Expose settings to global scope for other modules
            window.gameSettings = gameSettings;

            console.log('Settings module initialized');
        },

        // Public methods
        setSoundEnabled: function (enabled) {
            soundToggle.checked = enabled;
            toggleSound(enabled);
        },

        setCameraEnabled: function (enabled) {
            cameraToggle.checked = enabled;
            toggleCamera(enabled);
        },

        setHandTrackingEnabled: function (enabled) {
            handTrackingToggle.checked = enabled && gameSettings.cameraEnabled;
            toggleHandTracking(handTrackingToggle.checked);
        },

        getSettings: function () {
            return { ...gameSettings };
        },

        isPaused: function () {
            return gameSettings.gamePaused;
        }
    };
})();

// Initialize when document is ready
document.addEventListener('DOMContentLoaded', function () {
    Module_setting.init();
});