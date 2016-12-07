package testy;

import gra.Kierunek;
import gra.MojaPlansza;
import gra.MojaPostać;
import testy.zachowania.watkow.PrzesuwaniePostaci;

import java.util.ArrayList;

public class PrzesuwaniePoPlanszy {

  public static void main(String[] args) {

    simpleTest();
    simpleTest2(6);
    simpleTest3(8, 4, 5);
    simpleTest3(8, 2, 3);
  }

  private static void simpleTest2(int szerokość) {
    MojaPlansza plansza = new MojaPlansza(szerokość, szerokość);
    ArrayList<MojaPostać> postacie = new ArrayList<>();

    int liczbaPostaci = 2 * (szerokość - 1);
    for (int i = 0; i < liczbaPostaci; ++i) {
      postacie.add(new MojaPostać(1, 1));
    }

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1, 1);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2, 1);

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
    MojaPlansza plansza = new MojaPlansza(5, 5);
    MojaPostać p1 = new MojaPostać(1, 1);
    MojaPostać p2 = new MojaPostać(1, 1);

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1, 1);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2, 1);

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

  private static void simpleTest3(int szerokosc, int liczbaPierscieni, int wysokoscPierscienia) {
    MojaPlansza plansza = new MojaPlansza(szerokosc, szerokosc);

    ArrayList<Thread> wątki = new ArrayList<>();

    for (int i = 0; i < liczbaPierscieni; ++i) {
      Pierścień(plansza, i, wysokoscPierscienia - 1, wątki);
    }


    for (Thread wątek : wątki) {
      wątek.start();
    }
    for (Thread wątek : wątki) {
      try {
        wątek.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static void Pierścień(MojaPlansza plansza, int i, int k, ArrayList<Thread> wątki) {
    MojaPostać p1 = new MojaPostać(1, 1);
    MojaPostać p2 = new MojaPostać(1, 1);
    MojaPostać p3 = new MojaPostać(1, 1);
    MojaPostać p4 = new MojaPostać(1, 1);

    Kierunek[] kierunkiArr1 = {Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO};
    Kierunek[] kierunkiArr2 = {Kierunek.PRAWO, Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ};
    Kierunek[] kierunkiArr3 = {Kierunek.GÓRA, Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO};
    Kierunek[] kierunkiArr4 = {Kierunek.LEWO, Kierunek.DÓŁ, Kierunek.PRAWO, Kierunek.GÓRA};

    ArrayList<Kierunek> kierunki1 = getKierunki(kierunkiArr1, k);
    ArrayList<Kierunek> kierunki2 = getKierunki(kierunkiArr2, k);
    ArrayList<Kierunek> kierunki3 = getKierunki(kierunkiArr3, k);
    ArrayList<Kierunek> kierunki4 = getKierunki(kierunkiArr4, k);

    Thread th1 = new Thread(new PrzesuwaniePostaci(plansza, p1, i, i, kierunki1));
    Thread th2 = new Thread(new PrzesuwaniePostaci(plansza, p2, i + k, i, kierunki2));
    Thread th3 = new Thread(new PrzesuwaniePostaci(plansza, p3, i + k, i + k, kierunki3));
    Thread th4 = new Thread(new PrzesuwaniePostaci(plansza, p4, i, i + k, kierunki4));
    wątki.add(th1);
    wątki.add(th2);
    wątki.add(th3);
    wątki.add(th4);
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
