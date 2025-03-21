class Fruit extends SPP.Particle {
  constructor() {
    super();
  }

  drawTexture = function (context, texture, x, y) {
    context.drawImage(
      texture,
      x,
      y,
      texture.width,
      texture.height,
      -texture.width * 0.5,
      -texture.height * 0.5,
      texture.width,
      texture.height
    );
  };
  update() {
    this.rotation += this.rotationStep;
    this.context.translate(this.position.x - 20, this.position.y - 20);
    this.context.scale(this.scale, this.scale);
    this.drawTexture(this.context, this.shadow, 0, 0);
    this.context.setTransform(1, 0, 0, 1, 0, 0);

    this.context.translate(this.position.x, this.position.y);
    this.context.rotate(this.rotation);
    this.context.scale(this.scale, this.scale);
    this.drawTexture(this.context, this.texture, 0, 0);
    this.context.setTransform(1, 0, 0, 1, 0, 0);

    if (this.position.y > this.bottomY && this.bottomY != null) {
      this.life = 0;
    }
  }

  init(x, y, life, texture, shadow, context) {
    super.init(x, y, life);

    this.context = context;
    this.texture = texture;
    this.shadow = shadow;
    this.rotation = 0;
    this.scale = 1;
    this.radius = texture.width >= texture.height ? texture.width * 0.5 : texture.height * 0.5;
    this.radius *= this.scale;
    this.bottomY = null;

    this.rotationStep = (1 - Math.random() * 2) * 0.1;
    this.rotationStep = this.rotationStep <= 0 ? -0.1 : 0.1;
  }
}
