package testy;

import gra.Plansza;
import gra.Postać;

class StawianiePostaci implements Runnable {
  private final Plansza plansza;
  private final Postać postać;
  private final int x;
  private final int y;

  StawianiePostaci(Plansza plansza, Postać postać, int x, int y) {
    this.plansza = plansza;
    this.postać = postać;
    this.x = x;
    this.y = y;
  }

  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("Thread started: " + t.getName());
    try {
      plansza.postaw(postać, x, y);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }
    System.out.println("Thread ended: " + t.getName());
  }
}
