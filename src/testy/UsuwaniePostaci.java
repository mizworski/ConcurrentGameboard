package testy;

import gra.Plansza;
import gra.Postać;

class UsuwaniePostaci implements Runnable {
  private final Plansza plansza;
  private final Postać postać;

  UsuwaniePostaci(Plansza plansza, Postać postać) {
    this.plansza = plansza;
    this.postać = postać;
  }

  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("Thread started: " + t.getName());
    plansza.usuń(postać);
    System.out.println("Thread ended: " + t.getName());
  }
}
