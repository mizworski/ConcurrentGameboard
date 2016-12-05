package gra;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class MojaPlansza implements Plansza {
  private final static int PUSTE_POLE = -1;
  private final static int POCZĄTKOWA_LICZBA_SEMAFORÓW = 16;
  private final int wysokość;
  private final int szerokość;
  private int licznikPostaci;

  /// Plansza wraz z identyfikatorami postaci, które znajdują się na poszczególnym polu.
  private int[][] plansza;
  /// Identyfikatory postaci.
  private Map<Postać, Integer> postacie;
  /// Pola obecnie zablokowane przez metodę sprawdź.
  private Map<Pozycja, Boolean> zablokowanePola;

  /// Wszyscy, którzy czekają na ruch danej postaci.
  private Map<Integer, Set<Integer>> postacieZależne;
  /// Postacie, które muszą zostać ruszone, aby umożliwić ruch danej postaci.
  private Map<Integer, Set<Integer>> zależności;

  /// Lista postaci obecnie oczekujących na zwolnienie się danego pola.
  private Map<Pozycja, ArrayList<Integer>> postacieOczekująceNaPole;

  /// Pola, których podana postać wymaga, aby wykonać swój ruch.
  private Map<Integer, Set<Pozycja>> polaPotrzebneDoRuchu;

  private ArrayList<Semaphore> semafory;


  public MojaPlansza(int wysokość, int szerokość) {
    this.wysokość = wysokość;
    this.szerokość = szerokość;
    licznikPostaci = 0;
    plansza = new int[wysokość][szerokość];

    for (int i = 0; i < wysokość; ++i) {
      for (int j = 0; j < szerokość; ++j) {
        plansza[i][j] = PUSTE_POLE;
      }
    }

    semafory = new ArrayList<>(POCZĄTKOWA_LICZBA_SEMAFORÓW);

    /*
    for (int i = 0; i < POCZĄTKOWA_LICZBA_SEMAFORÓW; ++i) {
      semafory.add(new Semaphore(0));
    }
    */
  }

  @Override
  public void postaw(Postać postać, int wiersz, int kolumna) throws InterruptedException {

  }

  @Override
  public void przesuń(Postać postać, Kierunek kierunek) throws InterruptedException, DeadlockException {

  }

  @Override
  public void usuń(Postać postać) {

  }

  @Override
  public void sprawdź(int wiersz, int kolumna, Akcja jeśliZajęte, Runnable jeśliWolne) {

  }
}
