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
  private volatile int[][] plansza;
  /// Identyfikatory postaci.
  private volatile Map<Postać, Integer> postacie;
  /// Pola obecnie zablokowane przez metodę sprawdź.
  private volatile Set<Pozycja> zablokowanePola;

  /// Wszyscy, którzy czekają na ruch danej postaci.
  private volatile Map<Integer, Set<Integer>> postacieOczekująceNaPostać;
  /// Postacie, które muszą zostać ruszone, aby umożliwić ruch danej postaci.
  private volatile Map<Integer, Set<Integer>> postacieNaKtóreOczekujePostać;

  /// Lista postaci obecnie oczekujących na zwolnienie się danego pola.
  private volatile Map<Pozycja, LinkedList<Integer>> postacieOczekująceNaPole;

  /// Pola, których podana postać wymaga, aby wykonać swój ruch.
  private volatile Map<Integer, Set<Pozycja>> polaPotrzebneDoRuchu;

  /// Semafory, na których czekać będą
  /// Zakładam, że nie więcej niż jeden wątek, będzie sterował jedną postacią.
  private volatile Map<Integer, Semaphore> semafory;

  private Semaphore mutex;


  /// Postacie na planszy.
  private volatile Set<Integer> naPlanszyLubOczekującyNaWejście;

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

    mutex = new Semaphore(1);
    postacie = new HashMap<>();
    zablokowanePola = new HashSet<>();
    postacieOczekująceNaPostać = new HashMap<>();
    postacieOczekująceNaPole = new HashMap<>();
    postacieNaKtóreOczekujePostać = new HashMap<>();
    polaPotrzebneDoRuchu = new HashMap<>();
    semafory = new HashMap<>();
    naPlanszyLubOczekującyNaWejście = new HashSet<>();
    /*
    for (int i = 0; i < POCZĄTKOWA_LICZBA_SEMAFORÓW; ++i) {
      semafory.add(new Semaphore(0));
    }
    */
  }

  private int getPostaćId(Postać postać) {
    int idPostaci;

    if (postacie.containsKey(postać)) {
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
      polaPotrzebneDoRuchu.put(idPostaci, new HashSet<>());
    }
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.get(idPostaci);

    for (int i = wiersz; i < wiersz + postać.dajWysokość(); ++i) {
      for (int j = kolumna; j < kolumna + postać.dajSzerokość(); ++j) {
        pozycje.add(new Pozycja(i, j));
      }
    }
  }

  private void przemieśćSię(Postać postać) {
    int idPostaci = getPostaćId(postać);

    usuńSięZZależności(postać);
    ustawSięNaPozycjach(postać);
    dodajSięDoZależności(postać);

    polaPotrzebneDoRuchu.remove(idPostaci);
  }

  private void usuńSięZZależności(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Integer> oczekującyNaMnie = postacieOczekująceNaPostać.get(idPostaci);

    for (int idOczekującego : oczekującyNaMnie) {
      usuńSięOdOczekującego(idPostaci, idOczekującego);
    }

    assert oczekującyNaMnie.isEmpty();
  }

  private void usuńSięOdOczekującego(int idPostaci, int idOczekującego) {
    /// Zakładam, że każdy oczekujący na liste oczekującego na niego (możliwe, że pustą).
    Set<Integer> postacieOczekująceNaOczekującego = postacieOczekująceNaPostać.get(idOczekującego);

    /// Usuwam się z listy postaci, na które oczekuje postać, która na mnie oczekuje :)
    postacieNaKtóreOczekujePostać.get(idOczekującego).remove(idPostaci);
    /// Usuwam postać ze swojej listy.
    postacieNaKtóreOczekujePostać.get(idPostaci).remove(idOczekującego);

    /// Rekurencyjnie usuwam się z list postaci, które oczekiwały na oczekującego
    /// (ponieważ wówczas oczekiwały również na mnie).
    for (int oczekującyNaOczekującego : postacieOczekująceNaOczekującego) {
      usuńSięOdOczekującego(idPostaci, oczekującyNaOczekującego);
    }
  }

  private void ustawSięNaPozycjach(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.get(idPostaci);
    for (Pozycja pozycja : pozycje) {
      /// Ustawiam się na każdym z pól.
      plansza[pozycja.getX()][pozycja.getY()] = idPostaci;
    }
  }

  private void dodajSięDoZależności(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      /// Sprawdzam czy ktoś oczekuje na daną pozycje na planszy.
      if (!postacieOczekująceNaPole.containsKey(pozycja)) {
        postacieOczekująceNaPole.put(pozycja, new LinkedList<>());
      }

      for (int idOczekującego : postacieOczekująceNaPole.get(pozycja)) {
        dodajSięDoZależnościOczekującego(idPostaci, idOczekującego);
      }
    }
  }

  private void dodajSięDoZależnościOczekującego(int idPostaci, int idOczekującego) {
    Set<Integer> postacieOczekująceNaMnie = postacieOczekująceNaPostać.get(idPostaci);
    Set<Integer> oczekiwaniPrzezOczekującego = postacieNaKtóreOczekujePostać.get(idOczekującego);

    /// Dodaję się do listy postaci oczekiwanych przez oczekującego mnie
    /// oraz dodaję oczekującego na mnie do mojej listy oczekujących.
    oczekiwaniPrzezOczekującego.add(idPostaci);
    postacieOczekująceNaMnie.add(idOczekującego);

    Set<Integer> oczekującyNaOczekującego = postacieOczekująceNaPostać.get(idOczekującego);
    /// Dodaję się do listy oczekiwanych wszystkich oczekujących na oczekującego mnie.
    for (int idOczekującegoNaOczekującego : oczekującyNaOczekującego) {
      dodajSięDoZależnościOczekującego(idPostaci, idOczekującegoNaOczekującego);
    }

  }

  private void dodajSięDoOczekującychOrazAktualizujZależności(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaPotrzebneDoRuchu.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      dodajSięDoOczekującychNaPoleOrazAktualizujZależności(idPostaci, pozycja);
    }
  }

  private void dodajSięDoOczekującychNaPoleOrazAktualizujZależności(int idPostaci, Pozycja pozycja) {
    /// Jeśli nie istnieje lista oczekujących na dane pole, to ją tworzę.
    if (!postacieOczekująceNaPole.containsKey(pozycja)) {
      postacieOczekująceNaPole.put(pozycja, new LinkedList<>());
    }

    /// Dodaje się do listy.
    postacieOczekująceNaPole.get(pozycja).add(idPostaci);

    /// Dodaję wszystkich, na których oczekuję oraz
    /// dodaję się na ich listy postaci, które na nich oczekują.
    int idObecnejPostaci = plansza[pozycja.getX()][pozycja.getY()];
    if (idObecnejPostaci != PUSTE_POLE) {
      dodajPostacieNaKtóreOczekujesz(idPostaci, idObecnejPostaci);
    }

  }

  private void dodajPostacieNaKtóreOczekujesz(int idPostaci, int idOczekiwanego) {
    Set<Integer> postacieNaKtóreOczekuję = postacieNaKtóreOczekujePostać.get(idPostaci);
    Set<Integer> oczekującyNaOczekiwanego = postacieOczekująceNaPostać.get(idOczekiwanego);

    /// Dodaje oczekiwanego przeze mnie do listy moich oczekiwanych
    /// oraz dodaje się do listy oczekujących postaci, która mnie oczekuje.
    postacieNaKtóreOczekuję.add(idOczekiwanego);
    oczekującyNaOczekiwanego.add(idPostaci);

    Set<Integer> oczekiwaniPrzezOczekiwanego = postacieNaKtóreOczekujePostać.get(idOczekiwanego);
    for (int oczekiwanaPrzezOczekiwanego : oczekiwaniPrzezOczekiwanego) {
      dodajPostacieNaKtóreOczekujesz(idPostaci, oczekiwanaPrzezOczekiwanego);
    }
  }

  @Override
  public void postaw(Postać postać, int wiersz, int kolumna) throws InterruptedException {
    mutex.acquire();
    int idPostaci = getPostaćId(postać);
    if (naPlanszyLubOczekującyNaWejście.contains(idPostaci)) {
      throw new IllegalArgumentException("Postać jest już na planszy.");
    } else {
      // zakładam, że nie będzie można próbować postawić dwa razy z rzedu postaci, bez jej usunięcia
      // czyli nie dopuszczam do sytuacji:
      // >próbujemy postawić postać, zablokowaliśmy proces,
      // >nie postawiliśmy jeszcze naszej postaci
      // >inny proces próbuje postawić tę postać gdzieś indziej.
      semafory.put(idPostaci, new Semaphore(0));
      naPlanszyLubOczekującyNaWejście.add(idPostaci);
      postacieOczekująceNaPostać.put(idPostaci, new HashSet<>());
      postacieNaKtóreOczekujePostać.put(idPostaci, new HashSet<>());
    }

    dodajWymaganePola(postać, wiersz, kolumna);

    if (!czyAkcjaMożliwa(postać)) {
      dodajSięDoOczekującychOrazAktualizujZależności(postać);
      mutex.release();
      try {
        semafory.get(idPostaci).acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    przemieśćSię(postać);

    mutex.release();
  }

  @Override
  public void przesuń(Postać postać, Kierunek kierunek) throws InterruptedException, DeadlockException {

  }

  @Override
  public void usuń(Postać postać) {
    int idPostaci = getPostaćId(postać);
    if (!naPlanszyLubOczekującyNaWejście.contains(idPostaci)) {
      throw new IllegalArgumentException("Postaci nie ma na planszy.");
    } else {
      naPlanszyLubOczekującyNaWejście.remove(idPostaci);
      semafory.remove(idPostaci);
      postacieOczekująceNaPostać.remove(idPostaci);
      postacieNaKtóreOczekujePostać.remove(idPostaci);
    }

    // część dalsza
  }

  @Override
  public void sprawdź(int wiersz, int kolumna, Akcja jeśliZajęte, Runnable jeśliWolne) {

  }
}
