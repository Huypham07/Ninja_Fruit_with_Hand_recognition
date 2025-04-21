(function () {
    // freeze fruit update
    var freezeUpdate = function () {
        this.scale -= 0.013;
        if (this.scale < 0) {
            this.scale = 0;
            this.life = 0;
        }
    };

    var freezeJuiceUpdate = function () {
        this.scale -= 0.02;
        if (this.scale < 0) {
            this.scale = 0;
            this.life = 0;
        }
    };

    var buildFreezeJuice = function (target, juiceCount) {
        for (var i = 0; i < juiceCount; i++) {
            var juice = particleSystem.createParticle(SPP.SpriteImage);
            juice.init(target.position.x, target.position.y, Infinity, target.textureObj.j, middleContext);
            juice.onUpdate = freezeJuiceUpdate;
            juice.scale = Math.random() * 0.7;
            juice.damp.reset(0, 0);
            juice.velocity.reset(0, -(4 + Math.random() * 4));
            juice.velocity.rotate(360 * Math.random());
            juice.addForce("g", gravity);
        }
    };

    // splash when freeze fruit is cut
    var freezeSplashUpdate = function () {
        this.alpha -= 0.005;
        if (this.alpha < 0) {
            this.alpha = 0;
            this.life = 0;
        }
    };

    var buildFreezeSplash = function (target) {
        var splash = particleSystem.createParticle(SPP.SpriteImage);
        splash.init(target.position.x, target.position.y, Infinity, target.textureObj.s, bottomContext);
        splash.onUpdate = freezeSplashUpdate;
        splash.scale = 1 + Math.random();
        splash.rotation = Math.PI * 2 * Math.random();
    };

    var buildHalfFreezeFruit = function (target) {
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

    // throw freeze fruit
    throwFreezeFruit = function () {
        var textureObj = assetsManager.fruitsObj["freeze_fruit"];

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

    // cut freeze fruit
    cutFreezeFruit = function (target) {
        // Start freezing all fruits
        isFrozen = true;
        freezeTimer = freezeDuration; // Set the freeze duration to 5 seconds

        // Stop all fruits from moving
        fruitSystem.getParticles().forEach(fruit => {
            fruit.velocity.reset(0, 0); // Stop the movement of all fruits
        });

        score++;
        target.removeEventListener("dead", missHandler);

        buildHalfFreezeFruit(target);
        buildFreezeJuice(target, ((Math.random() * 30) >> 0) + 30);
        buildFreezeSplash(target);

        target.life = 0;
        createjs.Sound.play("splatter");
    };

    missHandler = function (e) {
        e.target.removeEventListener("dead", missHandler);
        if (gameState == GAME_OVER) return;
        missFruit(e.target);
        gameLife--;
        if (gameLife == 0) gameOver();
        if (gameLife < 0) gameLife = 0;
        ui_gamelifeTexture = assetsManager["gamelife-" + gameLife];
        ui_gameLife.texture = ui_gamelifeTexture;
    };
})();
