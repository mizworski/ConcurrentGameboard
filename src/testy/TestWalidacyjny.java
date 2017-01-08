import java.lang.invoke.MethodHandles;

import gra.Akcja;
import gra.DeadlockException;
import gra.Kierunek;
import gra.MojaPlansza;
import gra.Plansza;
import gra.Postać;

public class TestWalidacyjny {

    private static final int WIERSZE = 10;
    private static final int KOLUMNY = 20;
    private static final int WYSOKOŚĆ = 1;
    private static final int SZEROKOŚĆ = 1;

    public static void main(final String[] args) {
        System.out.println("Początek " + MethodHandles.lookup().lookupClass());
        final int wiersz = WIERSZE / 2;
        final int kolumna = KOLUMNY / 2;
        final Postać postać = new Postać() {

            @Override
            public int dajWysokość() {
                return WYSOKOŚĆ;
            }

            @Override
            public int dajSzerokość() {
                return SZEROKOŚĆ;
            }

        };
        final Plansza plansza = new MojaPlansza(WIERSZE, KOLUMNY);
        try {
            plansza.postaw(postać, wiersz, kolumna);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            plansza.przesuń(postać, Kierunek.GÓRA);
            plansza.przesuń(postać, Kierunek.DÓŁ);
            plansza.przesuń(postać, Kierunek.LEWO);
            plansza.przesuń(postać, Kierunek.PRAWO);
        } catch (InterruptedException | DeadlockException e) {
            throw new RuntimeException(e);
        }
        Akcja akcja = new Akcja() {
            @Override
            public void wykonaj(Postać p) {
                final int w = p.dajWysokość();
                final int s = p.dajSzerokość();
                if (!(w == WYSOKOŚĆ && s == SZEROKOŚĆ)) {
                    throw new RuntimeException(w + " " + s);
                }
            }
        };
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("puste");
            }
        };
        plansza.sprawdź(wiersz, kolumna, akcja, runnable);
        plansza.usuń(postać);
        System.out.println("DOBRZE 1/1");
        System.out.println("Koniec " + MethodHandles.lookup().lookupClass());
    }

}
