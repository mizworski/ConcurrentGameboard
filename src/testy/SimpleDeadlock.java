package testy;

import gra.Kierunek;
import gra.MojaPlansza;
import gra.MojaPostać;
import gra.Plansza;
import testy.zachowania.watkow.NieskończonyCykl;
import testy.zachowania.watkow.PrzesuwaniePostaci;

import java.util.ArrayList;

public class SimpleDeadlock {

  public static void main(String[] args) {

    Plansza plansza = new MojaPlansza(5, 6);

    Kierunek[] kierunkiArr1 = {Kierunek.PRAWO};
    Kierunek[] kierunkiArr2 = {Kierunek.LEWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1, 3);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2, 3);

    Thread w1 = new Thread(new PrzesuwaniePostaci(plansza, new MojaPostać(5, 2), 0, 0, kierunki1));
    Thread w2 = new Thread(new PrzesuwaniePostaci(plansza, new MojaPostać(2, 2), 3, 3, kierunki2));

    w1.start();
    w2.start();
    try {
      w1.join();
      w2.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  private static ArrayList<Kierunek> getKierunki(Kierunek[] kierunki, int k) {
    ArrayList<Kierunek> kierunkiList = new ArrayList<>();

    for (Kierunek kierunek : kierunki) {
      for (int i = 0; i < k; ++i) {
        kierunkiList.add(kierunek);
      }
    }
    return kierunkiList;
  }
}
