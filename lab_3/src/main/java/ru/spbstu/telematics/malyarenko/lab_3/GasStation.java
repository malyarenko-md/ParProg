package ru.spbstu.telematics.malyarenko.lab_3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.Queue;
import java.util.LinkedList;

public class GasStation {
    public static void main( String[] args ) {
        Semaphore availablePumpSemaphore = new Semaphore(3, true);
        BlockingQueue<Order> cashboxQueue = new ArrayBlockingQueue<>(3, true);
        BlockingQueue<Semaphore> cashierQueue = new ArrayBlockingQueue<>(1, true);
        Queue<FuelPumpThread> fuelPumpThreads = new LinkedList<>();
        Semaphore fuelPumpSemaphore[] = {new Semaphore(1), new Semaphore(1), new Semaphore(1)};

        // Потоки топливных насосов
        for (int i = 0; i < 3; i++) {
            try {
                fuelPumpSemaphore[i].acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            FuelPumpThread fuelPumpThread = new FuelPumpThread(fuelPumpSemaphore[i]);
            fuelPumpThreads.add(fuelPumpThread);
            new Thread(fuelPumpThread).start();
        }

        // Поток кассира
        new Thread(new CashierThrerad(cashboxQueue, cashierQueue, fuelPumpThreads)).start();

        // Потоки клиентов
        for (int i = 0; i < 10; i++) {
            new Thread(new CustomerThread(availablePumpSemaphore, cashboxQueue, cashierQueue)).start();
        }
    }
}

