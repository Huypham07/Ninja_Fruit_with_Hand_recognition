var gameWidth = window.innerWidth;
var gameHeight = window.innerHeight;

var screen;
var screenContext;

var particleSystem;
var fruitSystem;
var bombSystem;
var bladeSystem;
var gravity;

var timer = 0;
var interval = 1.8;

var bladeColor;
var bladeWidth;

var score;
var gameLife;
var storage;
var mouse = {};
var gameState;
var gameLevel;
var levelStep = 0.0001;
var isPlaying;
var GAME_READY = 1;
var GAME_PLAYING = 2;
var GAME_OVER = 3;

var ui_gameTitle;
var ui_newGame;
var ui_startFruit;

var ui_scoreIcon;
var ui_gameLife;
var ui_gamelifeTexture;
var ui_gameover;

var collide;