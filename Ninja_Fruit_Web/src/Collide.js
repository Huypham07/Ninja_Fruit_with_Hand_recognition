class Collide {
  sqr(x) {
    return x * x;
  }

  sign(n) {
    return n < 0 ? -1 : n > 0 ? 1 : 0;
  }

  equation12(a, b, c) {
    if (a == 0) return;

    let delta = b * b - 4 * a * c;
    if (delta == 0) return [(-b) / (2 * a), (-b) / (2 * a)];
    else if (delta > 0) return [(-b + Math.sqrt(delta)) / (2 * a), (-b - Math.sqrt(delta)) / (2 * a)];
  }

  lineXEllipse(p1, p2, c, r, e = 1) {
    if (r <= 0) return;
    
    let t1 = r,
      t2 = r * e;

    let a = this.sqr(t2) * this.sqr(p1[0] - p2[0]) + this.sqr(t1) * this.sqr(p1[1] - p2[1]);

    if (a <= 0) return;

    let b = 2 * this.sqr(t2) * (p2[0] - p1[0]) * (p1[0] - c[0]) + 2 * this.sqr(t1) * (p2[1] - p1[1]) * (p1[1] - c[1]);
    let cc = this.sqr(t2) * this.sqr(p1[0] - c[0]) + this.sqr(t1) * this.sqr(p1[1] - c[1]) - this.sqr(t1) * this.sqr(t2);

    let k = this.equation12(a, b, cc);
    if (!k) return;

    let result = [
      [p1[0] + k[0] * (p2[0] - p1[0]), p1[1] + k[0] * (p2[1] - p1[1])],
      [p1[0] + k[1] * (p2[0] - p1[0]), p1[1] + k[1] * (p2[1] - p1[1])],
    ];

    if (
      !(
        this.sign(result[0][0] - p1[0]) * this.sign(result[0][0] - p2[0]) <= 0 &&
        this.sign(result[0][1] - p1[1]) * this.sign(result[0][1] - p2[1]) <= 0
      )
    )
      result[0] = null;

    if (
      !(
        this.sign(result[1][0] - p1[0]) * this.sign(result[1][0] - p2[0]) <= 0 &&
        this.sign(result[1][1] - p1[1]) * this.sign(result[1][1] - p2[1]) <= 0
      )
    )
      result[1] = null;

    return result;
  }

  lineInEllipse(p1, p2, c, r, e) {
    let t = this.lineXEllipse(p1, p2, c, r, e);
    return t && (t[0] || t[1]);
  }
}
