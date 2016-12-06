package gra;

import java.util.*;
import java.util.concurrent.Semaphore;

public class MojaPlansza implements Plansza {
  private final static int PUSTE_POLE = -1;
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
  private volatile Map<Integer, Set<Pozycja>> polaWykorzystywanePodczasAkcji;

  /// Semafory, na których czekać będą
  /// Zakładam, że nie więcej niż jeden wątek, będzie sterował jedną postacią.
  private volatile Map<Integer, Semaphore> semafory;

  private Semaphore mutex;

  // Górne, lewe rogi postaci, tzn pozycja, zajmowana przez naszą postać, o najmniejszych współrzędnych.
  private volatile Map<Integer, Pozycja> pozycjePostaci;

  /// Postacie na planszy.
  private volatile Set<Integer> naPlanszyLubOczekującyNaWejście;

  /// Postacie, które można będzie wybudzić.
  private volatile LinkedList<Integer> doWybudzenia;

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
    polaWykorzystywanePodczasAkcji = new HashMap<>();
    semafory = new HashMap<>();
    naPlanszyLubOczekującyNaWejście = new HashSet<>();
    pozycjePostaci = new HashMap<>();
    doWybudzenia = new LinkedList<>();
  }

  private Integer getPostaćId(Postać postać) {
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

  private boolean czyAkcjaMożliwa(int idPostaci) {
    /// Zakładam, że istnieje taki set przypisany do id postaci.
    for (Pozycja pozycja : polaWykorzystywanePodczasAkcji.get(idPostaci)) {
      if (plansza[pozycja.getX()][pozycja.getY()] != PUSTE_POLE) {
        return false;
      }
      if (zablokowanePola.contains(pozycja)) {
        return false;
      }
    }

    return true;
  }

  private boolean czyZablokowane(int idPostaci) {
    /// Zakładam, że istnieje taki set przypisany do id postaci.
    for (Pozycja pozycja : polaWykorzystywanePodczasAkcji.get(idPostaci)) {
      if (zablokowanePola.contains(pozycja)) {
        return true;
      }
    }

    return false;
  }

  private void dodajWymaganePola(Postać postać, int wiersz, int kolumna) {
    int idPostaci = getPostaćId(postać);

    if (!polaWykorzystywanePodczasAkcji.containsKey(idPostaci)) {
      polaWykorzystywanePodczasAkcji.put(idPostaci, new HashSet<>());
    }
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (int i = wiersz; i < wiersz + postać.dajWysokość(); ++i) {
      for (int j = kolumna; j < kolumna + postać.dajSzerokość(); ++j) {
        pozycje.add(new Pozycja(i, j));
      }
    }
  }

  private void przemieśćSię(Postać postać) {
    int idPostaci = getPostaćId(postać);

    /// Nie będe musiał usuwać się z list osób, na które oczekuję oraz później sie do nich dodawać,
    /// gdyż właśnie wykonuję akcje, więc nie mogę czekać na nikogo.
    usuńSięOdOczekującychNaCiebie(postać);
    ustawSięNaPozycjach(postać);
    dodajSięDoOczekującychNaCiebie(postać);

    /// Po wykonaniu ruchu nie potrzebuję już żadnego pola, aby wykonać ruch.
    polaWykorzystywanePodczasAkcji.remove(idPostaci);
  }

  private void usuńSięOdOczekującychNaCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Integer> oczekującyNaMnie = postacieOczekująceNaPostać.get(idPostaci);

    for (int idOczekującego : oczekującyNaMnie) {
      usuńSięOdOczekującego(idPostaci, idOczekującego);
    }

    oczekującyNaMnie.clear();

    assert oczekującyNaMnie.isEmpty();
  }

  private void usuńSięOdOczekującego(int idPostaci, int idOczekującego) {
    Set<Integer> oczekiwaniPrzezOczekującego = postacieNaKtóreOczekujePostać.get(idOczekującego);

    /// Usuwam się z listy postaci oczekiwanych przez oczekującego mnie.
    oczekiwaniPrzezOczekującego.remove(idPostaci);

    Set<Integer> postacieOczekująceNaOczekującego = postacieOczekująceNaPostać.get(idOczekującego);
    /// Rekurencyjnie usuwam się z list postaci, które oczekiwały na oczekującego
    /// (ponieważ wówczas oczekiwały również na mnie).
    for (int oczekującyNaOczekującego : postacieOczekująceNaOczekującego) {
      usuńSięOdOczekującego(idPostaci, oczekującyNaOczekującego);
    }
  }

  private void usuńSięOdOczekiwanychPrzezCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Integer> oczekiwaniPrzezeMnie = postacieNaKtóreOczekujePostać.get(idPostaci);

    for (int idOczekiwanego : oczekiwaniPrzezeMnie) {
      usuńSięOdOczekiwanego(idPostaci, idOczekiwanego);
    }

    oczekiwaniPrzezeMnie.clear();

    assert oczekiwaniPrzezeMnie.isEmpty();
  }

  private void usuńSięOdOczekiwanego(int idPostaci, int idOczekiwanego) {
    Set<Integer> oczekującyPrzezOczekiwanego = postacieOczekująceNaPostać.get(idOczekiwanego);

    /// Usuwam się z listy postaci oczekującyc przez oczekiwanego przeze mnie.
    oczekującyPrzezOczekiwanego.remove(idPostaci);

    Set<Integer> postacieOczekiwanePrzezOczekiwanego = postacieNaKtóreOczekujePostać.get(idOczekiwanego);
    /// Rekurencyjnie usuwam się z list postaci, które oczekiwały na oczekującego
    /// (ponieważ wówczas oczekiwały również na mnie).
    for (int oczekiwanyPrzezOczekiwanego : postacieOczekiwanePrzezOczekiwanego) {
      usuńSięOdOczekiwanego(idPostaci, oczekiwanyPrzezOczekiwanego);
    }
  }

  private void ustawSięNaPozycjach(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    /// Inicjalizacja niepotrzebna, ale IDE pokazywało błąd.
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    boolean pierwsza = true;

    for (Pozycja pozycja : pozycje) {
      /// Ustawiam się na każdym z pól.
      plansza[pozycja.getX()][pozycja.getY()] = idPostaci;

      if (pierwsza) {
        minX = pozycja.getX();
        minY = pozycja.getY();
        pierwsza = false;
      }

      if (minX > pozycja.getX()) {
        minX = pozycja.getX();
      }
      if (minY > pozycja.getY()) {
        minY = pozycja.getY();
      }
    }

    /// Ustawiam górny, lewy róg.
    pozycjePostaci.put(idPostaci, new Pozycja(minX, minY));
  }

  private void wyczyśćPlansze(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);
    for (Pozycja pozycja : pozycje) {
      /// Ustawiam się na każdym z pól.
      plansza[pozycja.getX()][pozycja.getY()] = PUSTE_POLE;
    }
  }

  private void dodajSięDoOczekującychNaCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

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

  private void dodajSięDoOczekiwanychPrzezCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      dodajSięDoOczekiwanegoNaPozycji(idPostaci, pozycja);
    }
  }

  private void dodajSięDoOczekujących(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      if (!postacieOczekująceNaPole.containsKey(pozycja)) {
        postacieOczekująceNaPole.put(pozycja, new LinkedList<>());
      }

      /// Dodaje się do listy.
      postacieOczekująceNaPole.get(pozycja).add(idPostaci);
    }
  }

  private void usuńSięZOczekujących(Postać postać) {
    Integer idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      /// Usuwam się z listy.
      postacieOczekująceNaPole.get(pozycja).remove(idPostaci);
    }
  }

  private void dodajSięDoOczekiwanegoNaPozycji(int idPostaci, Pozycja pozycja) {
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
    try {
      mutex.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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

    if (!czyAkcjaMożliwa(idPostaci)) {
      dodajSięDoOczekujących(postać);
      dodajSięDoOczekiwanychPrzezCiebie(postać);
      mutex.release();
      try {
        semafory.get(idPostaci).acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      usuńSięZOczekujących(postać);
    }

    przemieśćSię(postać);

    mutex.release();
  }

  @Override
  public void przesuń(Postać postać, Kierunek kierunek) throws InterruptedException, DeadlockException {

  }

  @Override
  public void usuń(Postać postać) {
    try {
      mutex.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Integer idPostaci = getPostaćId(postać);
    if (!naPlanszyLubOczekującyNaWejście.contains(idPostaci)) {
      throw new IllegalArgumentException("Postaci nie ma na planszy.");
    }

    Pozycja pozycjaPostaci = pozycjePostaci.get(idPostaci);

    dodajWymaganePola(postać, pozycjaPostaci.getX(), pozycjaPostaci.getY());

    if (czyZablokowane(idPostaci)) {
      mutex.release();
      try {
        semafory.get(idPostaci).acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Set<Integer> oczekującyNaMnie = new HashSet<>(postacieOczekująceNaPostać.get(idPostaci));

    wyczyśćPlansze(postać);
    usuńSięOdOczekującychNaCiebie(postać);
    usuńSięOdOczekiwanychPrzezCiebie(postać);

    assert oczekującyNaMnie.size() != 0;

    wyznaczPostacieDoWybudzenia(oczekującyNaMnie);

    /// do obudzenia lista oczekujących na mnie

    /// wybudź oczekujących

    naPlanszyLubOczekującyNaWejście.remove(idPostaci);
    semafory.remove(idPostaci);
    postacieOczekująceNaPostać.remove(idPostaci);
    postacieNaKtóreOczekujePostać.remove(idPostaci);

    assert !Objects.equals(doWybudzenia.peek(), idPostaci);
    if (doWybudzenia.size() == 0) {
      mutex.release();
    } else {
      semafory.get(doWybudzenia.poll()).release();
    }
  }

  private void wyznaczPostacieDoWybudzenia(Set<Integer> oczekującyNaMnie) {
    List<Integer> doUsunięcia = new Vector<>();

    for (Integer idBudzonego : doWybudzenia) {
      if (postacieOczekująceNaPostać.get(idBudzonego).size() != 0) {
        doUsunięcia.add(idBudzonego);
      }
    }

    for (Integer idUsuwanego : doUsunięcia) {
      //noinspection SuspiciousMethodCalls
      doWybudzenia.remove(doUsunięcia);
    }

    for (Integer idOczekującego : oczekującyNaMnie) {
      if (postacieNaKtóreOczekujePostać.get(idOczekującego).size() == 0) {
        if (!doWybudzenia.contains(idOczekującego)) {
          doWybudzenia.add(idOczekującego);
        }
      }
    }

  }

  @Override
  public void sprawdź(int wiersz, int kolumna, Akcja jeśliZajęte, Runnable jeśliWolne) {

  }
}
