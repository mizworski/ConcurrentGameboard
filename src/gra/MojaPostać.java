package gra;

public class MojaPostać implements Postać {
  private final int wysokość;
  private final int szerokość;

  public MojaPostać(int wysokość, int szerokość) {
    this.wysokość = wysokość;
    this.szerokość = szerokość;
  }
  @Override
  public int dajWysokość() {
    return wysokość;
  }

  @Override
  public int dajSzerokość() {
    return szerokość;
  }
}
