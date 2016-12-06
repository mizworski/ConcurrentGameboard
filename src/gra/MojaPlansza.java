package gra;

import java.util.*;
import java.util.concurrent.Semaphore;

public class MojaPlansza implements Plansza {
  private final static int PUSTE_POLE = -1;
  //  private final static int POCZĄTKOWA_LICZBA_SEMAFORÓW = 16;
  private final int wysokość;
  private final int szerokość;
  private int licznikPostaci;

  /// Plansza wraz z identyfikatorami postaci, które znajdują się na poszczególnym polu.
  private int[][] plansza;
  /// Identyfikatory postaci.
  private Map<Postać, Integer> postacie;
  /// Pola obecnie zablokowane przez metodę sprawdź.
  private Set<Pozycja> zablokowanePola;

  /// Wszyscy, którzy czekają na ruch danej postaci.
  private Map<Integer, Set<Integer>> postacieZależne;
  /// Postacie, które muszą zostać ruszone, aby umożliwić ruch danej postaci.
  private Map<Integer, Set<Integer>> zależności;

  /// Lista postaci obecnie oczekujących na zwolnienie się danego pola.
  private Map<Pozycja, LinkedList<Integer>> postacieOczekująceNaPole;

  /// Pola, których podana postać wymaga, aby wykonać swój ruch.
  private Map<Integer, Set<Pozycja>> polaPotrzebneDoRuchu;

  private Map<Integer, Semaphore> semafory;

  /// Postacie na planszy.
  private Set<Integer> aktualnieNaPlanszy;

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

    /*
    for (int i = 0; i < POCZĄTKOWA_LICZBA_SEMAFORÓW; ++i) {
      semafory.add(new Semaphore(0));
    }
    */
  }

  private int getPostaćId(Postać postać) {
    int idPostaci;

    if (!postacie.containsKey(postać)) {
      idPostaci = postacie.get(postać);
    } else {
      idPostaci = licznikPostaci;
      postacie.put(postać, licznikPostaci);
      ++licznikPostaci;
    }

    return idPostaci;
  }

  private boolean czyAkcjaMożliwa(Postać postać) {
    int idPostaci = getPostaćId(postać);

    /// Zakładam, że istnieje taki set przypisany do id postaci.
    for (Pozycja pozycja : polaPotrzebneDoRuchu.get(idPostaci)) {
      if (plansza[pozycja.getX()][pozycja.getY()] != PUSTE_POLE) {
        return false;
      }
      if (zablokowanePola.contains(pozycja)) {
        return false;
      }
    }

    return true;
  }

  private void dodajWymaganePola(Postać postać, int wiersz, int kolumna) {
    int idPostaci = getPostaćId(postać);

    if (!polaPotrzebneDoRuchu.containsKey(idPostaci)) {
      polaPotrzebneDoRuchu.put(idPostaci, Collections.emptySet());
    }
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.get(idPostaci);

    for (int i = wiersz; i < wiersz + postać.dajWysokość(); ++i) {
      for (int j = kolumna; j < kolumna + postać.dajSzerokość(); ++j) {
        pozycje.add(new Pozycja(wiersz, kolumna));
      }
    }
  }

  private void przemieśćSię(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.remove(idPostaci);

    for (Pozycja pozycja : pozycje) {
      plansza[pozycja.getX()][pozycja.getY()] = idPostaci;
    }
  }

  private void dodajDoOczekujących(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.remove(idPostaci);


    for (Pozycja pozycja : pozycje) {
      if (!postacieOczekująceNaPole.containsKey(pozycja)) {
        postacieOczekująceNaPole.put(pozycja, new LinkedList<>());
      }
      postacieOczekująceNaPole.get(pozycja).add(idPostaci);
    }
  }

  @Override
  public void postaw(Postać postać, int wiersz, int kolumna) throws InterruptedException {
    int idPostaci = getPostaćId(postać);
    if (aktualnieNaPlanszy.contains(idPostaci)) { //todo lepsza nazwa
      throw new IllegalArgumentException("Postać jest już na planszy.");
    } else {
      aktualnieNaPlanszy.add(idPostaci);
      // zakładam, że nie będzie można próbować postawić dwa razy z rzedu postaci, bez jej usunięcia
      // czyli nie dopuszczam do sytuacji:
      // >próbujemy postawić postać, zablokowaliśmy proces,
      // >nie postawiliśmy jeszcze naszej postaci
      // >inny proces próbuje postawić tę postać gdzieś indziej.
      semafory.put(idPostaci, new Semaphore(0));
    }
    dodajWymaganePola(postać, wiersz, kolumna);
    // dodaj swoj semafor

    if (czyAkcjaMożliwa(postać)) {
      przemieśćSię(postać);
    } else {
      dodajDoOczekujących(postać);
    }
  }

  @Override
  public void przesuń(Postać postać, Kierunek kierunek) throws InterruptedException, DeadlockException {

  }

  @Override
  public void usuń(Postać postać) {
    int idPostaci = getPostaćId(postać);
    if (!aktualnieNaPlanszy.contains(idPostaci)) {
      throw new IllegalArgumentException("Postaci nie ma na planszy.");
    } else {
      aktualnieNaPlanszy.remove(idPostaci);
      semafory.remove(idPostaci);
    }

    // część dalsza
  }

  @Override
  public void sprawdź(int wiersz, int kolumna, Akcja jeśliZajęte, Runnable jeśliWolne) {

  }
}
