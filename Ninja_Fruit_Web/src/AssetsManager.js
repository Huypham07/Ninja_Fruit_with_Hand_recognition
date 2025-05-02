class AssetsManager extends SPP.EventDispatcher {
  constructor() {
    super();

    this.fruitsDir = "assets/fruits/";
    this.fruitStateLabels = ["w", "l", "r", "s", "j"];
    this.fruitImageType = ".png";

    this.fruitsObj = {};
    this.fruitsArray = [];
    this.images = {};
    this.sounds = {};
    this.loader = new createjs.LoadQueue();
    this.loader.installPlugin(createjs.Sound);

    this.loader.addEventListener("complete", () => this.handleComplete());
  }

  handleComplete() {
    const fruits = Game.assets.fruits;
    for (let i = 0; i < fruits.length; i++) {
      let obj = {};
      for (let j = 0; j < this.fruitStateLabels.length; j++) {
        obj[this.fruitStateLabels[j]] = this.loader.getResult(fruits[i] + this.fruitStateLabels[j]);
      }
      this.fruitsArray.push(obj);
      this.fruitsObj[fruits[i]] = obj;
    }

    const other = Game.assets.other;
    for (let i = 0; i < other.length; i++) {
      this[other[i].id] = this.loader.getResult(other[i].id);
    }

    this.dispatchEvent(new SPP.Event("complete"));
  }

  start() {
    const fruits = Game.assets.fruits;
    for (let i = 0; i < fruits.length; i++) {
      for (let j = 0; j < this.fruitStateLabels.length; j++) {
        this.loader.loadFile(
          {
            id: fruits[i] + this.fruitStateLabels[j],
            src: `${this.fruitsDir}${fruits[i]}-${this.fruitStateLabels[j]}${this.fruitImageType}`,
          },
          false
        );
      }
    }
    this.loader.loadManifest(Game.assets.other, false);
    this.loader.load();
  }

  getRandomFruit() {
    return this.fruitsArray[Math.floor(this.fruitsArray.length * Math.random())];
  }


}
