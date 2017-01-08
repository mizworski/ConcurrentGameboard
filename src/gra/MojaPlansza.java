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

  /// Górne, lewe rogi postaci, tzn pozycja, zajmowana przez naszą postać, o najmniejszych współrzędnych.
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

    mutex = new Semaphore(1, true);
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

  @Override
  public void postaw(Postać postać, int wiersz, int kolumna) throws InterruptedException {
    try {
      mutex.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int idPostaci = getPostaćId(postać);

    błądJeśliPostaćNaMapie(idPostaci);
    błądJeśliPozycjaPozaMapą(postać, wiersz, kolumna);

    /// Dodaję docelową pozycję na mapie.
    pozycjePostaci.put(idPostaci, new Pozycja(wiersz, kolumna));
    dodajWymaganePola(postać, wiersz, kolumna);

    czekajJeśliAkcjaNiemożliwa(postać);

    przemieśćSię(postać);

    mutex.release();
  }

  @Override
  public void przesuń(Postać postać, Kierunek kierunek) throws InterruptedException, DeadlockException {
    try {
      mutex.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Set<Pozycja> zwalnianePola = new HashSet<>();
    Integer idPostaci = getPostaćId(postać);

    sprawdżCzyPoprawnyRuch(postać, kierunek);
    wyznaczPolaKtóreSięZmienią(postać, kierunek, zwalnianePola);
    sprawdźDeadlock(postać);

    czekajJeśliAkcjaNiemożliwa(postać);

    Set<Integer> wcześniejOczekującyNaMnie = new HashSet<>(postacieOczekująceNaPostać.get(idPostaci));
    przemieśćSię(postać);
    wyczyśćPlansze(zwalnianePola);
    wyznaczPostacieDoWybudzenia(wcześniejOczekującyNaMnie);

    if (doWybudzenia.size() == 0) {
      mutex.release();
    } else {
      semafory.get(doWybudzenia.poll()).release();
    }

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

    Pozycja pozycjaPostaci = pozycjePostaci.remove(idPostaci);

    dodajWymaganePola(postać, pozycjaPostaci.getX(), pozycjaPostaci.getY());

    śpijJeśliZablokowane(idPostaci);

    /// Wyznaczam postacie, które przed ukończeniem ruchu oczekiwały na mnie.
    Set<Integer> wcześniejOczekującyNaMnie = new HashSet<>(postacieOczekująceNaPostać.get(idPostaci));
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    wyczyśćPlansze(pozycje);
    usuńSięOdOczekującychNaCiebie(postać);
    usuńSięOdOczekiwanychPrzezCiebie(postać);

    wyznaczPostacieDoWybudzenia(wcześniejOczekującyNaMnie);

    naPlanszyLubOczekującyNaWejście.remove(idPostaci);
    semafory.remove(idPostaci);
    postacieOczekująceNaPostać.remove(idPostaci);
    postacieNaKtóreOczekujePostać.remove(idPostaci);

    if (doWybudzenia.size() == 0) {
      mutex.release();
    } else {
      semafory.get(doWybudzenia.poll()).release();
    }
  }

  @Override
  public void sprawdź(int wiersz, int kolumna, Akcja jeśliZajęte, Runnable jeśliWolne) {
    if (wiersz < 0 || kolumna < 0 || wiersz >= wysokość || kolumna >= szerokość) {
      throw new IllegalArgumentException("Pozycja poza mapą");
    }

    Integer idPostaci = plansza[wiersz][kolumna];

    if (idPostaci == PUSTE_POLE) {
      jeśliWolne.run();
    } else {
      Postać postać = znajdźPostac(idPostaci);
      Pozycja pozycja = new Pozycja(wiersz, kolumna);
      zablokowanePola.add(pozycja);
      jeśliZajęte.wykonaj(postać);
      zablokowanePola.remove(pozycja);
    }
  }

  private Postać znajdźPostac(Integer idPostaci) {
    for (Postać postać : postacie.keySet()) {
      if (Objects.equals(postacie.get(postać), idPostaci)) {
        return postać;
      }
    }

    /// Nigdy się tak nie wydarzy.
    return null;
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
      if (plansza[pozycja.getX()][pozycja.getY()] != PUSTE_POLE
        /* plansza[pozycja.getX()][pozycja.getY()] != idPostaci */) {
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
      polaWykorzystywanePodczasAkcji.put(idPostaci, new HashSet<Pozycja>());
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

    /*
    Set<Integer> postacieOczekująceNaOczekującego = postacieOczekująceNaPostać.get(idOczekującego);
    /// Rekurencyjnie usuwam się z list postaci, które oczekiwały na oczekującego
    /// (ponieważ wówczas oczekiwały również na mnie).
    for (int oczekującyNaOczekującego : postacieOczekująceNaOczekującego) {
      usuńSięOdOczekującego(idPostaci, oczekującyNaOczekującego);
    }
    */
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
  }

  private void ustawSięNaPozycjach(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      /// Ustawiam się na każdym z pól.
      plansza[pozycja.getX()][pozycja.getY()] = idPostaci;
    }
  }

  private void wyczyśćPlansze(Set<Pozycja> pozycje) {
    for (Pozycja pozycja : pozycje) {
      /// Ustawiam się na każdym z pól.
      plansza[pozycja.getX()][pozycja.getY()] = PUSTE_POLE;
    }
  }

  private Set<Pozycja> wyznaczPozycjePostaci(Postać postać) {
    Integer idPostaci = getPostaćId(postać);
    Pozycja pozycjaPostaci = pozycjePostaci.get(idPostaci);

    Set<Pozycja> res = new HashSet<>();

    for (int i = pozycjaPostaci.getX(); i < pozycjaPostaci.getX() + postać.dajWysokość(); ++i) {
      for (int j = pozycjaPostaci.getY(); j < pozycjaPostaci.getY() + postać.dajSzerokość(); ++j) {
        res.add(new Pozycja(i, j));
      }
    }

    return res;
  }

  private void dodajSięDoOczekującychNaCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = wyznaczPozycjePostaci(postać);

    for (Pozycja pozycja : pozycje) {
      /// Sprawdzam czy ktoś oczekuje na daną pozycje na planszy.
      if (!postacieOczekująceNaPole.containsKey(pozycja)) {
        postacieOczekująceNaPole.put(pozycja, new LinkedList<Integer>());
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
  }

  private void dodajSięDoOczekujących(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = polaWykorzystywanePodczasAkcji.get(idPostaci);

    for (Pozycja pozycja : pozycje) {
      if (!postacieOczekująceNaPole.containsKey(pozycja)) {
        postacieOczekująceNaPole.put(pozycja, new LinkedList<Integer>());
      }

      /// Dodaje się do listy.
      postacieOczekująceNaPole.get(pozycja).add(idPostaci);
    }
  }

  private void dodajSięDoOczekiwanychPrzezCiebie(Postać postać) {
    int idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = wyznaczPozycjePostaci(postać);

    for (Pozycja pozycja : pozycje) {
      dodajSięDoOczekiwanegoNaPozycji(idPostaci, pozycja);
    }
  }

  private void usuńSięZOczekujących(Postać postać) {
    Integer idPostaci = getPostaćId(postać);
    Set<Pozycja> pozycje = wyznaczPozycjePostaci(postać);

    for (Pozycja pozycja : pozycje) {
      postacieOczekująceNaPole.get(pozycja).remove(idPostaci);
    }
  }

  private void dodajSięDoOczekiwanegoNaPozycji(int idPostaci, Pozycja pozycja) {
    int idObecnejPostaci = plansza[pozycja.getX()][pozycja.getY()];
    if (idObecnejPostaci != PUSTE_POLE && idObecnejPostaci != idPostaci) {
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
  }

  private void błądJeśliPostaćNaMapie(int idPostaci) {
    if (naPlanszyLubOczekującyNaWejście.contains(idPostaci)) {
      throw new IllegalArgumentException("Postać jest już na planszy.");
    } else {
      /// zakładam, że nie będzie można próbować postawić dwa razy z rzedu postaci, bez jej usunięcia
      /// czyli nie dopuszczam do sytuacji:
      /// >próbujemy postawić postać, zablokowaliśmy proces,
      /// >nie postawiliśmy jeszcze naszej postaci
      /// >inny proces próbuje postawić tę postać gdzieś indziej.
      semafory.put(idPostaci, new Semaphore(0));
      naPlanszyLubOczekującyNaWejście.add(idPostaci);
      postacieOczekująceNaPostać.put(idPostaci, new HashSet<Integer>());
      postacieNaKtóreOczekujePostać.put(idPostaci, new HashSet<Integer>());
    }
  }

  private void błądJeśliPozycjaPozaMapą(Postać postać, int wiersz, int kolumna) {
    if (wiersz < 0 || kolumna < 0 ||
        wiersz + postać.dajWysokość() > wysokość ||
        kolumna + postać.dajSzerokość() > szerokość) {
      throw new IllegalArgumentException("Postać nie mieści się na mapie.");
    }
  }

  private void czekajJeśliAkcjaNiemożliwa(Postać postać) {
    int idPostaci = getPostaćId(postać);

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
  }


  private void sprawdźDeadlock(Postać postać) throws DeadlockException {
    Integer idPostaci = getPostaćId(postać);
    Set<Pozycja> wymaganePola = polaWykorzystywanePodczasAkcji.get(idPostaci);
    Set<Integer> postacieNaPolach = new HashSet<>();

    for (Pozycja pozycja : wymaganePola) {
      Integer idPostaciNaPolu = plansza[pozycja.getX()][pozycja.getY()];
      if (idPostaciNaPolu != PUSTE_POLE) {
        postacieNaPolach.add(idPostaciNaPolu);
      }
    }

    /// Wyznaczam wszystkich, którzy są przeze mnie oczekiwani.
    Set<Integer> oczekiwaniPrzezCiebie = new HashSet<>();

    for (Integer oczekiwany : postacieNaPolach) {
      wyznaczOczekiwanychPrzezCiebie(oczekiwany, oczekiwaniPrzezCiebie);
    }

    /// Wyznaczam wszystkich, którzy mnie oczekują.
    Set<Integer> oczekującyNaCiebie = new HashSet<>();

    for (Integer oczekujący : postacieOczekująceNaPostać.get(idPostaci)) {
      wyznaczOczekującychNaCiebie(oczekujący, oczekującyNaCiebie);
    }
    /// Sprawdzam czy ktoś z oczekiwanych przeze mnie nie oczekuje na oczekującego mnie.

    sprawdźCzyOczekiwanyOczekujeOczekującego(oczekiwaniPrzezCiebie, oczekującyNaCiebie);

    /// Sprawdzam czy sam nie oczekuję na kogoś, kto oczekuje mnie.
    Set<Integer> przecięcie = new HashSet<>(oczekiwaniPrzezCiebie);
    przecięcie.retainAll(oczekującyNaCiebie);

    if (przecięcie.size() > 0) {
      throw new DeadlockException();
    }
  }

  private void sprawdźCzyOczekiwanyOczekujeOczekującego(Set<Integer> oczekiwaniPrzezCiebie, Set<Integer> oczekującyNaCiebie) throws DeadlockException {
    for (Integer idOczekiwanego : oczekiwaniPrzezCiebie) {
      for (Integer idOczekującego : oczekującyNaCiebie) {
        if (postacieNaKtóreOczekujePostać.get(idOczekiwanego).contains(idOczekującego)) {
          throw new DeadlockException();
        }
      }
    }
  }

  private void wyznaczOczekującychNaCiebie(Integer idOczekującego, Set<Integer> oczekującyNaCiebie) {
    oczekującyNaCiebie.add(idOczekującego);

    Set<Integer> oczekiwaniPrzezOczekiwanego = postacieOczekująceNaPostać.get(idOczekującego);

    for (Integer idOczekującegoNaOczekującego : oczekiwaniPrzezOczekiwanego) {
      wyznaczOczekującychNaCiebie(idOczekującegoNaOczekującego, oczekującyNaCiebie);
    }
  }

  private void wyznaczOczekiwanychPrzezCiebie(Integer idOczekiwanego, Set<Integer> oczekiwaniPrzezCiebie) {
    oczekiwaniPrzezCiebie.add(idOczekiwanego);

    Set<Integer> oczekiwaniPrzezOczekiwanego = postacieNaKtóreOczekujePostać.get(idOczekiwanego);

    for (Integer idOczekiwanegoPrzezOczekiwanego : oczekiwaniPrzezOczekiwanego) {
      wyznaczOczekiwanychPrzezCiebie(idOczekiwanegoPrzezOczekiwanego, oczekiwaniPrzezCiebie);
    }
  }

  private void wyznaczPolaKtóreSięZmienią(Postać postać, Kierunek kierunek, Set<Pozycja> zwalnianePola) {
    Integer idPostaci = getPostaćId(postać);
    Pozycja pozycjaPostaci = pozycjePostaci.get(idPostaci);

    if (!polaWykorzystywanePodczasAkcji.containsKey(idPostaci)) {
      polaWykorzystywanePodczasAkcji.put(idPostaci, new HashSet<Pozycja>());
    }
    /// Nowe pola, które będę zajmował.
    Set<Pozycja> wymaganePola = polaWykorzystywanePodczasAkcji.get(idPostaci);

    /// Góry lewy róg prostokąta, który będe zajmował (a wcześniej nie zajmowałem).
    int naKtóreX = pozycjaPostaci.getX();
    int naKtóreY = pozycjaPostaci.getY();

    /// Góry lewy róg prostokąta, który będe zwalniał.
    int zKtóregoX = pozycjaPostaci.getX();
    int zKtóregoY = pozycjaPostaci.getY();

    switch (kierunek) {
      case GÓRA:
        naKtóreX = pozycjaPostaci.getX() - 1;
        zKtóregoX = pozycjaPostaci.getX() + postać.dajWysokość() - 1;
        pozycjePostaci.put(idPostaci, new Pozycja(pozycjaPostaci.getX() - 1, pozycjaPostaci.getY()));
        break;
      case DÓŁ:
        zKtóregoX = pozycjaPostaci.getX();
        naKtóreX = pozycjaPostaci.getX() + postać.dajWysokość();
        pozycjePostaci.put(idPostaci, new Pozycja(pozycjaPostaci.getX() + 1, pozycjaPostaci.getY()));
        break;
      case LEWO:
        naKtóreY = pozycjaPostaci.getY() - 1;
        zKtóregoY = pozycjaPostaci.getY() + postać.dajSzerokość() - 1;
        pozycjePostaci.put(idPostaci, new Pozycja(pozycjaPostaci.getX(), pozycjaPostaci.getY() - 1));
        break;
      case PRAWO:
        zKtóregoY = pozycjaPostaci.getY();
        naKtóreY = pozycjaPostaci.getY() + postać.dajSzerokość();
        pozycjePostaci.put(idPostaci, new Pozycja(pozycjaPostaci.getX(), pozycjaPostaci.getY() + 1));
        break;
    }

    /// Po wyznaczeniu rogów prostokątów, wyznaczam całe prostokąty (do zwolnienia oraz do zajęcia).
    switch (kierunek) {
      case GÓRA:
      case DÓŁ:
        for (int i = naKtóreY; i < naKtóreY + postać.dajSzerokość(); ++i) {
          wymaganePola.add(new Pozycja(naKtóreX, i));
          zwalnianePola.add(new Pozycja(zKtóregoX, i));
        }
        break;
      case LEWO:
      case PRAWO:
        for (int i = naKtóreX; i < naKtóreX + postać.dajWysokość(); ++i) {
          wymaganePola.add(new Pozycja(i, naKtóreY));
          zwalnianePola.add(new Pozycja(i, zKtóregoY));
        }
        break;
    }
  }

  private void sprawdżCzyPoprawnyRuch(Postać postać, Kierunek kierunek) {
    Integer idPostaci = getPostaćId(postać);
    Pozycja pozycjaPostaci = pozycjePostaci.get(idPostaci);

    switch (kierunek) {
      case GÓRA:
        if (pozycjaPostaci.getX() == 0) {
          throw new IllegalArgumentException("Nie można przesunąć się do góry.");
        }
        break;
      case DÓŁ:
        if (pozycjaPostaci.getX() + postać.dajWysokość() == wysokość) {
          throw new IllegalArgumentException("Nie można przesunąć się w dół.");
        }
        break;
      case LEWO:
        if (pozycjaPostaci.getY() == 0) {
          throw new IllegalArgumentException("Nie można przesunąć się w lewo.");
        }
        break;
      case PRAWO:
        if (pozycjaPostaci.getY() + postać.dajSzerokość() == szerokość) {
          throw new IllegalArgumentException("Nie można przesunąć się w prawo.");
        }
        break;
    }
  }


  private void śpijJeśliZablokowane(Integer idPostaci) {
    if (czyZablokowane(idPostaci)) {
      mutex.release();
      try {
        semafory.get(idPostaci).acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void wyznaczPostacieDoWybudzenia(Set<Integer> oczekującyNaMnie) {
    List<Integer> doUsunięcia = new Vector<>();

    for (Integer idBudzonego : doWybudzenia) {
      if (postacieNaKtóreOczekujePostać.get(idBudzonego).size() != 0) {
        doUsunięcia.add(idBudzonego);
      }
    }

    for (Integer idUsuwanego : doUsunięcia) {
      doWybudzenia.remove(idUsuwanego);
    }

    for (Integer idOczekującego : oczekującyNaMnie) {
      if (postacieNaKtóreOczekujePostać.get(idOczekującego).size() == 0) {
        if (!doWybudzenia.contains(idOczekującego)) {
          doWybudzenia.add(idOczekującego);
        }
      }
    }

  }
}
