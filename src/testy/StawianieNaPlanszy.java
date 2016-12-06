package testy;

import gra.MojaPlansza;
import gra.MojaPostać;

public class StawianieNaPlanszy {

  public static void main(String[] args) {

    MojaPlansza plansza = new MojaPlansza(4, 4);
    MojaPostać p1 = new MojaPostać(2, 2);
    MojaPostać p2 = new MojaPostać(2, 2);
    MojaPostać p3 = new MojaPostać(1, 1);
    MojaPostać p4 = new MojaPostać(2, 2);
    MojaPostać p5 = new MojaPostać(4, 3);

    Thread th1 = new Thread(new StawianiePostaci(plansza, p1, 0, 2));
    Thread th2 = new Thread(new StawianiePostaci(plansza, p2, 1, 1));
    Thread th3 = new Thread(new StawianiePostaci(plansza, p3, 2, 2));
    Thread th4 = new Thread(new StawianiePostaci(plansza, p4, 1, 1));
    Thread th5 = new Thread(new StawianiePostaci(plansza, p5, 0, 0));

    th1.start();
    th2.start();
    th3.start();
    th4.start();
    th5.start();
/*

    Thread thu1 = new Thread(new UsuwaniePostaci(plansza, p1));
    Thread thu2 = new Thread(new UsuwaniePostaci(plansza, p2));
    Thread thu3 = new Thread(new UsuwaniePostaci(plansza, p3));
    Thread thu4 = new Thread(new UsuwaniePostaci(plansza, p4));
    Thread thu5 = new Thread(new UsuwaniePostaci(plansza, p5));

    try {
      th1.join();
      thu1.start();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      th2.join();
      thu2.start();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      th3.join();
      thu3.start();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      th4.join();
      thu4.start();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      th5.join();
      thu5.start();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
*/


    try {
      th1.join();
      th2.join();
      th3.join();
      th4.join();
      th5.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }



  }

}
