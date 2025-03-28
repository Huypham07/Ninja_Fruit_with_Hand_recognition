window.onload = loadAssets;

window.game = {
  isGameStarted: false
};

function loadAssets() {
  assetsManager = new AssetsManager();
  assetsManager.addEventListener("complete", init);
  assetsManager.start();
}
function init() {
  //canvas
  topCanvas = document.getElementById("top");
  topCanvas.style.display = "block";
  topCanvas.width = gameWidth;
  topCanvas.height = gameHeight;
  topContext = topCanvas.getContext("2d");
  topContext.globalCompositeOperation = "lighter";

  middleCanvas = document.getElementById("middle");
  middleCanvas.style.display = "block";
  middleCanvas.width = gameWidth;
  middleCanvas.height = gameHeight;
  middleContext = middleCanvas.getContext("2d");

  bottomCanvas = document.getElementById("bottom");
  bottomCanvas.style.display = "block";
  bottomCanvas.width = gameWidth;
  bottomCanvas.height = gameHeight;
  bottomContext = bottomCanvas.getContext("2d");
  bottomContext.fillStyle = "#f6c223";
  bottomContext.textAlign = "left";
  bottomContext.textBaseline = "top";

  // Khởi tạo hệ thống camera
  initCamera();

  //particle system
  particleSystem = new SPP.ParticleSystem();
  particleSystem.start();
  bladeSystem = new SPP.ParticleSystem();
  bladeSystem.start();
  fruitSystem = new SPP.ParticleSystem();
  fruitSystem.start();
  bombSystem = new SPP.ParticleSystem();
  bombSystem.start();
  gravity = new SPP.Gravity(0.15);

  //data
  if (typeof chrome !== "undefined" && typeof chrome.storage != "undefined") storage = chrome.storage.local;
  else storage = window.localStorage;
  if (!storage.highScore) storage.highScore = 0;
  gameState = GAME_READY;
  score = 0;
  gameLife = 3;
  ui_gamelifeTexture = assetsManager["gamelife-3"];
  gameLevel = 0.1;

  // Use hand tracking or mouse to control

  topCanvas.addEventListener("mousedown", (e) => {
  isDragging = true;
  updateMousePosition(e);
});

topCanvas.addEventListener("mousemove", (e) => {
  if (isDragging) updateMousePosition(e);
});

topCanvas.addEventListener("mouseup", () => {
  isDragging = false;
});

// Sự kiện cảm ứng (Mobile)
topCanvas.addEventListener("touchstart", (e) => {
  isDragging = true;
  updateMousePosition(e.touches[0]); // Lấy vị trí ngón tay đầu tiên
});

topCanvas.addEventListener("touchmove", (e) => {
  if (isDragging) updateMousePosition(e.touches[0]);
});

topCanvas.addEventListener("touchend", () => {
  isDragging = false;
});
  //   handtracking = new HandTracking(topCanvas.width, topCanvas.height);
  //   handtracking.tracker.params.simple = true;
  //   handtracking.addEventListener("handmove", handmove);

  render();
  enterGame();
}
function enterGame() {
  showStartGameUI();
}

function resetGameData() {
  gameState = GAME_READY;
  score = 0;
  gameLife = 3;
  ui_gamelifeTexture = assetsManager["gamelife-3"];
  gameLevel = 0.1;
  window.game.isGameStarted = false;
}
function startGame(e) {
  hideStartGameUI();

  resetGameData();
  showScoreUI();
  gameState = GAME_PLAYING;
  window.game.isGameStarted = true;
}
function renderTimer() {
  if (gameState != GAME_PLAYING) return;
  timer += SPP.frameTime;
  if (timer >= interval) {
    timer = 0;
    throwObject();
  }
}
function throwObject() {
  var n = ((Math.random() * 4) >> 0) + 1;
  for (var i = 0; i < n; i++) {
    if (isThrowBomb()) throwBomb();
    else throwFruit();
  }
  createjs.Sound.play("throwFruit");
}
function isThrowBomb() {
  var n = Math.random() * 2;
  if (n < gameLevel) return true;
  return false;
}
function levelUpdate() {
  gameLevel += levelStep;
  if (gameLevel > 1) {
    gameLevel = 0.1;
  }
}

function gameOver() {
  if (gameState == GAME_OVER) return;
  var l = fruitSystem.getParticles().length;
  while (l-- > 0) {
    fruitSystem.getParticles()[l].removeEventListener("dead", missHandler);
  }
  gameState = GAME_OVER;
  gameLife = 0;
  ui_gamelifeTexture = assetsManager["gamelife-" + gameLife];
  ui_gameLife.texture = ui_gamelifeTexture;
  if (score > parseInt(storage["highScore"])) storage.highScore = score;
  showGameoverUI();
}
function gameOverComplete() {
  replay();
}

function replay(e) {
  hideGameoverUI();
}

//mouse event
function updateMousePosition(e) {
  let rect = topCanvas.getBoundingClientRect(); // Lấy tọa độ canvas trên màn hình
  mouse.x = e.clientX - rect.left;
  mouse.y = e.clientY - rect.top;
  buildBladeParticle(mouse.x, mouse.y);
}

//hand tracking event
function handmove(e) {
  console.log(e);
  buildBladeParticle(e.x, e.y);
}
//render canvas
function render() {
  requestAnimationFrame(render);

  topContext.clearRect(0, 0, gameWidth, gameHeight);
  middleContext.clearRect(0, 0, gameWidth, gameHeight);
  bottomContext.clearRect(0, 0, gameWidth, gameHeight);
  //   handtracking.tick();

  // Render camera background nếu đang sử dụng
  renderCameraBackground();

  fruitSystem.render();
  bombSystem.render();
  particleSystem.render();
  showScoreTextUI();
  bladeSystem.render();

  buildColorBlade(bladeColor, bladeWidth);
  collideTest();
  levelUpdate();
  renderTimer();
}
