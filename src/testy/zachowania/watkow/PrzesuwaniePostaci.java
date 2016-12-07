package testy.zachowania.watkow;

import gra.DeadlockException;
import gra.Kierunek;
import gra.Plansza;
import gra.Postać;

import java.util.ArrayList;

public class PrzesuwaniePostaci implements Runnable {
  private final Plansza plansza;
  private final Postać postać;
  private final ArrayList<Kierunek> kierunki;
  private final int x;
  private final int y;

  public PrzesuwaniePostaci(Plansza plansza, Postać postać, int x, int y, ArrayList<Kierunek> kierunki) {
    this.plansza = plansza;
    this.postać = postać;
    this.kierunki = kierunki;
    this.x = x;
    this.y = y;
  }

  @Override
  public void run() {
    Thread t = Thread.currentThread();
    System.out.println("Thread started przesuń: " + t.getName());

    try {
      plansza.postaw(postać, x, y);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }
//    try {
//      Thread.sleep(2000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }

    for (Kierunek kierunek : kierunki) {
      try {
        plansza.przesuń(postać, kierunek);
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      } catch (DeadlockException e) {
        System.out.println("Deadlock");
      }
    }

    plansza.usuń(postać);

    System.out.println("Thread ended przesuń: " + t.getName());
  }
}
