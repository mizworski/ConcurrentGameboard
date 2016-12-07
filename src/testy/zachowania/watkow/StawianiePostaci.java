package testy.zachowania.watkow;

import gra.Plansza;
import gra.Postać;

public class StawianiePostaci implements Runnable {
  private final Plansza plansza;
  private final Postać postać;
  private final int x;
  private final int y;

  public StawianiePostaci(Plansza plansza, Postać postać, int x, int y) {
    this.plansza = plansza;
    this.postać = postać;
    this.x = x;
    this.y = y;
  }

  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("Thread started Postaw: " + t.getName());
    try {
      plansza.postaw(postać, x, y);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }
    System.out.println("Thread ended Postaw, started Usuń: " + t.getName());
    plansza.usuń(postać);
    System.out.println("Thread ended Usuń: " + t.getName());
  }
}
