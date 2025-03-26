/**
 * Module_setting - Handles game settings and their UI controls
 */
const Module_setting = (function() {
    // Private variables
    let settingsToggle;
    let settingsContent;
    let soundToggle;
    let cameraToggle;
    let pauseToggle;
    let countdownElement;
    
    // Game settings state
    const gameSettings = {
        soundEnabled: true,
        cameraEnabled: true,
        gamePaused: false
    };

    // Private methods
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
    }
    
    // Animation frame ID for pause functionality
    let originalRequestAnimationFrame;
    let originalGameLoop;
    
    // Create or get countdown element
    function getCountdownElement() {
        if (!countdownElement) {
            countdownElement = document.createElement('div');
            countdownElement.id = 'game-countdown';
            countdownElement.style.position = 'absolute';
            countdownElement.style.left = '50%';
            countdownElement.style.top = '50%';
            countdownElement.style.transform = 'translate(-50%, -50%)';
            countdownElement.style.fontSize = '100px';
            countdownElement.style.fontWeight = 'bold';
            countdownElement.style.color = '#fff';
            countdownElement.style.textShadow = '0 0 10px #000';
            countdownElement.style.zIndex = '1000';
            countdownElement.style.display = 'none';
            document.body.appendChild(countdownElement);
        }
        return countdownElement;
    }
    
    // Show countdown before resuming the game
    function showCountdown(callback) {
        const countdown = getCountdownElement();
        countdown.style.display = 'block';
        
        let count = 3;
        countdown.textContent = count;
        
        const interval = setInterval(() => {
            count--;
            if (count > 0) {
                countdown.textContent = count;
            } else {
                clearInterval(interval);
                countdown.style.display = 'none';
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
        // Method 1: Use game object if available
        if (window.game && typeof window.game.pause === 'function') {
            window.game.pause();
            return; // Successfully handled by game object
        }
        
        // Method 2: Override requestAnimationFrame for a universal pause solution
        if (!originalRequestAnimationFrame) {
            // Save the original function on first pause
            originalRequestAnimationFrame = window.requestAnimationFrame;
            
            // Override requestAnimationFrame to stop all animations
            window.requestAnimationFrame = function(callback) {
                originalGameLoop = callback;
                return 0; // Return a dummy ID
            };
            
            // Dispatch a custom event for other modules
            document.dispatchEvent(new CustomEvent('gamePaused'));
            console.log('Animation loop paused');
        }
    }
    
    function resumeGame() {
        // Method 1: Use game object if available
        if (window.game && typeof window.game.resume === 'function') {
            window.game.resume();
            return; // Successfully handled by game object
        }
        
        // Method 2: Restore original requestAnimationFrame
        if (originalRequestAnimationFrame) {
            // Restore the original function
            window.requestAnimationFrame = originalRequestAnimationFrame;
            originalRequestAnimationFrame = null;
            
            // Resume the game loop if we have it
            if (originalGameLoop) {
                window.requestAnimationFrame(originalGameLoop);
                console.log('Animation loop resumed');
            }
            
            // Dispatch a custom event for other modules
            document.dispatchEvent(new CustomEvent('gameResumed'));
        }
    }

    // Public interface
    return {
        init: function() {
            // Get DOM elements
            settingsToggle = document.getElementById('settings-toggle');
            settingsContent = document.getElementById('settings-content');
            soundToggle = document.getElementById('sound-toggle');
            cameraToggle = document.getElementById('camera-toggle');
            pauseToggle = document.getElementById('pause-toggle');
            
            // Create countdown element
            getCountdownElement();
            
            // Set up event handlers
            settingsToggle.addEventListener('click', function() {
                settingsContent.style.display = settingsContent.style.display === 'block' ? 'none' : 'block';
            });
            
            soundToggle.addEventListener('change', function() {
                toggleSound(this.checked);
            });
            
            cameraToggle.addEventListener('change', function() {
                toggleCamera(this.checked);
            });
            
            pauseToggle.addEventListener('change', function() {
                togglePause(this.checked);
            });

            // Initialize camera if Module_camera.js is loaded
            if (typeof initCamera === 'function') {
                // Make sure camera module is initialized
                initCamera();
                
                // Set initial camera state based on setting
                toggleCamera(gameSettings.cameraEnabled);
            }

            // Add keyboard shortcut for pause (Escape key)
            document.addEventListener('keydown', function(event) {
                if (event.key === 'Escape') {
                    pauseToggle.checked = !pauseToggle.checked;
                    togglePause(pauseToggle.checked);
                }
            });

            // Expose settings to global scope for other modules
            window.gameSettings = gameSettings;
            
            console.log('Settings module initialized');
        },
        
        // Public methods to control settings programmatically
        setSoundEnabled: function(enabled) {
            soundToggle.checked = enabled;
            toggleSound(enabled);
        },
        
        setCameraEnabled: function(enabled) {
            cameraToggle.checked = enabled;
            toggleCamera(enabled);
        },
        
        setPaused: function(paused) {
            pauseToggle.checked = paused;
            togglePause(paused);
        },
        
        // Get current settings
        getSettings: function() {
            return {...gameSettings};
        },
        
        // Check if game is paused
        isPaused: function() {
            return gameSettings.gamePaused;
        }
    };
})();

// Initialize when document is ready
document.addEventListener('DOMContentLoaded', function() {
    Module_setting.init();
});