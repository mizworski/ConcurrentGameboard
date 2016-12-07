package testy;

import gra.*;

import java.util.ArrayList;

public class DeadlockTest {
  public static void main(String[] args) {
    utworzenieCyklu(2);
  }

  private static void utworzenieCyklu(int szerokość) {
    Plansza plansza = new MojaPlansza(szerokość, szerokość);
    ArrayList<MojaPostać> postacie = new ArrayList<>();

    int liczbaPostaci = 4 * szerokość - 4;

    for (int i = 0; i < liczbaPostaci; ++i) {
      postacie.add(new MojaPostać(1, 1));
    }

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1, szerokość - 1);


    ArrayList<Thread> wątki = new ArrayList<>();

    for (int i = 0; i < liczbaPostaci; ++i) {
      Thread nowyWątek = new Thread(new NieskończonyCykl(plansza, new MojaPostać(1, 1), 0, 0, kierunki1));
      wątki.add(nowyWątek);
    }

    for (Thread th : wątki) {
      th.start();
    }

    for (Thread th : wątki) {
      try {
        th.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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

