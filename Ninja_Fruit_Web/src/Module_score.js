const Module_score = (function() {
    let scoreBoard;
    let overlay;
    let gameState;
    let bestScore = parseInt(localStorage.getItem('bestScore')) || 0;

    function init() {
        scoreBoard = document.getElementById('score-board');
        overlay = document.getElementById('score-overlay');

        // Initialize best score display
        document.getElementById('best-score').textContent = bestScore;

        // Add event listeners
        document.getElementById('home-btn').addEventListener('click', () => {
            hideScoreBoard();
            // Only reload when explicitly going home
            window.location.reload();
        });

        document.getElementById('replay-btn').addEventListener('click', () => {
            hideScoreBoard();
            // Keep the background state but reset game data
            if (typeof resetGameData === 'function') {
                resetGameData();
            }
            if (typeof startGame === 'function') {
                startGame();
            }
        });
    }

    function showScoreBoard(score) {
        gameState = {
            score: score,
            // Store current background state
            backgroundImage: document.body.style.backgroundImage,
            // Store any other game state you want to preserve
        };
        
        // Show score overlay without changing background
        overlay.style.display = 'block';
        overlay.style.backgroundColor = 'rgba(0, 0, 0, 0.7)'; // Semi-transparent overlay
        scoreBoard.style.display = 'block';
        
        document.getElementById('final-score').textContent = score;
        updateBestScore(score);
    }

    function hideScoreBoard() {
        overlay.style.display = 'none';
        scoreBoard.style.display = 'none';
        
        // Restore game state if needed
        if (gameState && gameState.backgroundImage) {
            document.body.style.backgroundImage = gameState.backgroundImage;
        }
    }

    function updateBestScore(currentScore) {
        currentScore = parseInt(currentScore);
        if (currentScore > bestScore) {
            bestScore = currentScore;
            localStorage.setItem('bestScore', bestScore.toString());
            document.getElementById('best-score').textContent = bestScore;
        }
    }

    return {
        init: init,
        show: showScoreBoard,
        hide: hideScoreBoard
    };
})();