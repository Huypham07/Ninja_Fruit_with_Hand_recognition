var Game = {
  assets: {
    fruits: ["apple", "basaha", "peach", "sandia"],

    other: [
      { id: "gameover", src: "assets/gameover.png" },
      { id: "gametitle", src: "assets/gametitle.png" },
      { id: "shadow", src: "assets/shadow.png" },
      { id: "bomb", src: "assets/bomb.png" },
      { id: "miss", src: "assets/miss.png" },
      { id: "star", src: "assets/star.png" },
      { id: "score", src: "assets/score.png" },
      { id: "gamelife-3", src: "assets/gamelife-3.png" },
      { id: "gamelife-2", src: "assets/gamelife-2.png" },
      { id: "gamelife-1", src: "assets/gamelife-1.png" },
      { id: "gamelife-0", src: "assets/gamelife-0.png" },
      { id: "newgame", src: "assets/newgame.png" },

      { id: "throwFruit", src: "assets/sound/throw-fruit.ogg" },
      { id: "bombExplode", src: "assets/sound/bomb-explode.ogg" },
      { id: "splatter", src: "assets/sound/splatter.ogg" },
    ],
  },
};

var isUsingCamera = false;
var isDragging = false;
var mediaHandTracking = null;
var isHandTrackingEnabled = false;

var gameWidth = window.innerWidth;
var gameHeight = window.innerHeight;

var topCanvas;
var topContext;
var middleCanvas;
var middleContext;
var bottomCanvas;
var bottomContext;

var particleSystem;
var fruitSystem;
var bombSystem;
var bladeSystem;
var gravity;

var timer = 0;
var interval = 1.8;

var bladeColor;
var bladeWidth;
//game data
var mouse = {};
var score;
var gameLife;
var storage;
var isPlaying;
var GAME_READY = 1,
  GAME_PLAYING = 2,
  GAME_OVER = 3;
var gameState;
var gameLevel;
var levelStep = 0.0001;

//start game ui
var ui_gameTitle;
var ui_newGame;
var ui_startFruit;

var ui_scoreIcon;
var ui_gameLife;
var ui_gamelifeTexture;
var ui_gameOver;

//--collideTest
var collide;
