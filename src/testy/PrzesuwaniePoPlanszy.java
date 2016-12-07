package testy;

import gra.Kierunek;
import gra.MojaPlansza;
import gra.MojaPostać;

import java.util.ArrayList;
import java.util.Collections;

public class PrzesuwaniePoPlanszy {

  public static void main(String[] args) {

//    simpleTest();
//    simpleTest2(3);
    simpleTest3();
  }

  private static void simpleTest2(int szerokość) {
    MojaPlansza plansza = new MojaPlansza(4, 4);
    ArrayList<MojaPostać> postacie = new ArrayList<>();

    int liczbaPostaci = 2 * (szerokość - 1);
    for (int i = 0; i < liczbaPostaci; ++i) {
      postacie.add(new MojaPostać(1, 1));
    }

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2);

    ArrayList<Thread> wątki = new ArrayList<>();

    for (int i = 0; i < liczbaPostaci; ++i) {
      Thread wątek;
      if (i % 2 == 0) {
        wątek = new Thread(new PrzesuwaniePostaci(plansza, postacie.get(i), i / 2, i / 2, kierunki1));
      } else {
        wątek = new Thread(new PrzesuwaniePostaci(plansza, postacie.get(i), 1 + i / 2, 1 + i / 2, kierunki2));
      }
      wątki.add(wątek);
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

  private static void simpleTest() {
    MojaPlansza plansza = new MojaPlansza(2, 2);
    MojaPostać p1 = new MojaPostać(1, 1);
    MojaPostać p2 = new MojaPostać(1, 1);

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2);

    Thread th1 = new Thread(new PrzesuwaniePostaci(plansza, p1, 0, 0, kierunki1));
    Thread th2 = new Thread(new PrzesuwaniePostaci(plansza, p2, 1, 1, kierunki2));

    th1.start();
    th2.start();

    try {
      th1.join();
      th2.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void simpleTest3() {
    MojaPlansza plansza = new MojaPlansza(3, 3);
    MojaPostać p1 = new MojaPostać(1, 1);
    MojaPostać p2 = new MojaPostać(1, 1);
    MojaPostać p3 = new MojaPostać(1, 1);
    MojaPostać p4 = new MojaPostać(1, 1);

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.GÓRA, Kierunek.LEWO, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.PRAWO, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.GÓRA, Kierunek.LEWO, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.DÓŁ};
    Kierunek[] kierunkiArr3 = {Kierunek.GÓRA, Kierunek.GÓRA, Kierunek.LEWO, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.PRAWO};
    Kierunek[] kierunkiArr4 = {Kierunek.LEWO, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.GÓRA};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2);
    ArrayList<Kierunek> kierunki3 = getKierunki(kierunkiArr3);
    ArrayList<Kierunek> kierunki4 = getKierunki(kierunkiArr4);

    Thread th1 = new Thread(new PrzesuwaniePostaci(plansza, p1, 0, 0, kierunki1));
    Thread th2 = new Thread(new PrzesuwaniePostaci(plansza, p2, 2, 0, kierunki2));
    Thread th3 = new Thread(new PrzesuwaniePostaci(plansza, p3, 2, 2, kierunki3));
    Thread th4 = new Thread(new PrzesuwaniePostaci(plansza, p4, 0, 2, kierunki4));

    th1.start();
    th2.start();
    th3.start();
    th4.start();

    try {
      th1.join();
      th2.join();
      th3.join();
      th4.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static ArrayList<Kierunek> getKierunki(Kierunek[] kierunki) {
    ArrayList<Kierunek> kierunkiList = new ArrayList<>();

    for (int i = 0; i < 2; ++i) {
      Collections.addAll(kierunkiList, kierunki);
    }
    return kierunkiList;
  }

}
