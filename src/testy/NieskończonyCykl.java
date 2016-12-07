package testy;

import gra.DeadlockException;
import gra.Kierunek;
import gra.Plansza;
import gra.Postać;

import java.util.ArrayList;

public class NieskończonyCykl implements Runnable {
  private final Plansza plansza;
  private final Postać postać;
  private final int x;
  private final int y;
  private final ArrayList<Kierunek> kierunki;

  NieskończonyCykl(Plansza plansza, Postać postać, int x, int y, ArrayList<Kierunek> kierunki) {
    this.plansza = plansza;
    this.postać = postać;
    this.x = x;
    this.y = y;
    this.kierunki = kierunki;
  }


  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("Thread started dprzesuń: " + t.getName());

    try {
      plansza.postaw(postać, x, y);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }

    int i = 0;

    do {
      for (Kierunek kierunek : kierunki) {
        try {
          plansza.przesuń(postać, kierunek);
        } catch (InterruptedException e) {
          System.out.println(e.getMessage());
        } catch (DeadlockException e) {
          System.out.println("Deadlock.");
        }


      }

    } while (true);
  }
}
