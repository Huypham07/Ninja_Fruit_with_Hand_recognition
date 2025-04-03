(function () {
  //jucie

  var juiceUpdate = function () {
    this.scale -= 0.013;
    if (this.scale < 0) {
      this.scale = 0;
      this.life = 0;
    }
  };
  var buildJuice = function (target, juiceCount) {
    for (var i = 0; i < juiceCount; i++) {
      var juice = particleSystem.createParticle(SPP.SpriteImage);
      juice.init(target.position.x, target.position.y, Infinity, target.textureObj.j, middleContext);
      juice.onUpdate = juiceUpdate;
      juice.scale = Math.random() * 0.7;
      juice.damp.reset(0, 0);
      juice.velocity.reset(0, -(4 + Math.random() * 4));
      juice.velocity.rotate(360 * Math.random());
      juice.addForce("g", gravity);
    }
  };
  //splash
  var splashUpdate = function () {
    this.alpha -= 0.005;
    if (this.alpha < 0) {
      this.alpha = 0;
      this.life = 0;
    }
  };
  var buildSplash = function (target) {
    var splash = particleSystem.createParticle(SPP.SpriteImage);
    splash.init(target.position.x, target.position.y, Infinity, target.textureObj.s, bottomContext);
    splash.onUpdate = splashUpdate;
    splash.scale = 1 + Math.random();
    splash.rotation = Math.PI * 2 * Math.random();
  };
  var buildHalfFruit = function (target) {
    var speed = 3 + Math.random() * 3;

    var right = particleSystem.createParticle(Fruit);
    right.init(
      target.position.x,
      target.position.y,
      Infinity,
      target.textureObj.r,
      assetsManager.shadow,
      middleContext
    );
    right.velocity.reset(0, -speed);
    right.velocity.rotate(20 * Math.random());
    right.damp.reset(0, 0);
    right.rotation = target.rotation;
    right.bottomY = gameHeight + target.textureObj.r.height;
    right.addForce("g", gravity);

    var left = particleSystem.createParticle(Fruit);
    left.init(target.position.x, target.position.y, Infinity, target.textureObj.l, assetsManager.shadow, middleContext);
    left.velocity.reset(0, -speed);
    left.velocity.rotate(-20 * Math.random());
    left.damp.reset(0, 0);
    left.rotation = target.rotation;
    left.bottomY = gameHeight + target.textureObj.l.height;
    left.addForce("g", gravity);
  };
  //if miss fruit
  var missUpdate = function () {
    this.alpha -= 0.01;
    if (this.alpha < 0) {
      this.alpha = 0;
      this.life = 0;
    }
  };
  var missFruit = function (target) {
    var lose = particleSystem.createParticle(SPP.SpriteImage);
    var x = target.position.x;
    if (x <= 0) x = 40;
    if (x > gameWidth) x = gameWidth - 40;
    lose.init(x, gameHeight - assetsManager.miss.height, Infinity, assetsManager.miss, topContext);
    lose.velocity.reset(0, -1);
    lose.damp.reset(0.01, 0.01);
    lose.onUpdate = missUpdate;
  };

  //throw fruit
  throwFruit = function () {
    var textureObj = assetsManager.getRandomFruit();
    // Special fruit spawn chance (5%)
    const specialFruitChance = Math.random();
    if (specialFruitChance < 0.05) { // 5% chance for special fruits
      // Randomly choose between freeze and explode (50/50)
      if (Math.random() < 0.5) {
        textureObj = assetsManager.fruitsObj["freeze_fruit"];
        console.log("Spawning freeze fruit");
      } else {
        textureObj = assetsManager.fruitsObj["explode_fruit"];
        console.log("Spawning explode fruit");
      }
    } else {
      // Get random normal fruit
      const normalFruits = ["apple", "basaha", "peach", "sandia"];
      const randomIndex = Math.floor(Math.random() * normalFruits.length);
      textureObj = assetsManager.fruitsObj[normalFruits[randomIndex]];
    }

    var p = fruitSystem.createParticle(Fruit);
    p.velocity.reset(0, -(10 + Math.random() * 3));
    p.velocity.rotate(8 - Math.random() * 16);
    p.damp.reset(0, 0);
    p.addForce("g", gravity);

    p.addEventListener("dead", missHandler);
    p.init(
      gameWidth * 0.5 + (1 - Math.random() * 2) * 200,
      gameHeight + textureObj.w.height,
      Infinity,
      textureObj.w,
      assetsManager.shadow,
      middleContext
    );
    p.textureObj = textureObj;
    p.bottomY = gameHeight + textureObj.w.height;
  };
  //cut fruit
  cutFruit = function (target) {

    if (target.textureObj === assetsManager.fruitsObj["freeze_fruit"]) {
      //target.freeze();
      isFrozen = true;
      //freezeTimer = freezeDuration;
      fruitSystem.getParticles().forEach(fruit => {
        if (fruit !== target) {
          //console.log(fruit);
          fruit.velocity.reset(0, 0);
          //fruit.damp.reset(1, 1);
          fruit.removeForce("g");
          //fruit.removeForce('rotation');
        }
      })
      bombSystem.getParticles().forEach(bomb => {
        bomb.velocity.reset(0, 0);
        //bomb.damp.reset(1, 1);
        bomb.removeForce("g");
      })

      setTimeout(() => {
        isFrozen = false;
        console.log(fruitSystem.getParticles());
        fruitSystem.getParticles().forEach(fruit => {
          fruit.addForce("g", gravity);
        })
        bombSystem.getParticles().forEach(bomb => {
          bomb.addForce("g", gravity);
        })
      }, 5000)


    }

    else if (target.textureObj === assetsManager.fruitsObj["explode_fruit"]) {
      console.log("Explode fruit hit!"); // Debug log

      // Get all active fruits except the explode fruit
      const activeFruits = fruitSystem.getParticles().filter(fruit =>
        fruit !== target && fruit.life > 0
      );

      // Cut all active fruits
      activeFruits.forEach(fruit => {
        fruit.removeEventListener("dead", missHandler);
        buildHalfFruit(fruit);
        buildJuice(fruit, ((Math.random() * 30) >> 0) + 30);
        buildSplash(fruit);
        fruit.life = 0;
        score++; // Add score for each exploded fruit
      });
    }

    score++;
    target.removeEventListener("dead", missHandler);

    buildHalfFruit(target);
    buildJuice(target, ((Math.random() * 30) >> 0) + 30);
    buildSplash(target);

    target.life = 0;
    createjs.Sound.play("splatter");
  };
  missHandler = function (e) {
    e.target.removeEventListener("dead", missHandler);
    if (e.target.textureObj === assetsManager.fruitsObj["freeze_fruit"] ||
      e.target.textureObj === assetsManager.fruitsObj["explode_fruit"]) {
      return; // Don't reduce life for special fruits
    }
    missFruit(e.target);
    gameLife--;
    if (gameLife == 0) gameOver();
    if (gameLife < 0) gameLife = 0;
    ui_gamelifeTexture = assetsManager["gamelife-" + gameLife];
    ui_gameLife.texture = ui_gamelifeTexture;
  };
})();