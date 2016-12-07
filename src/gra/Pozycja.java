package gra;

import java.util.Objects;

class Pozycja {
  private int x;
  private int y;

    Pozycja(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public boolean equals(Object other) {
    return other instanceof Pozycja && this.x == ((Pozycja) other).getX() && this.y == ((Pozycja) other).getY();

  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  int getX() {
    return x;
  }

  int getY() {
    return y;
  }
}