package jantarDosFilosofos;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class JantarDosFilosofos {
    static final int NUM_FILOSOFOS = 5;
    static final int PENSANDO = 0;
    static final int FAMINTO = 1;
    static final int COMENDO = 2;
    
    static int[] estado = new int[NUM_FILOSOFOS];
    static Semaphore mutex = new Semaphore(1);
    static Semaphore[] semaforos = new Semaphore[NUM_FILOSOFOS];
    
    static AtomicInteger[] contadorRefeicoes = new AtomicInteger[NUM_FILOSOFOS];
    static volatile boolean executando = true;
    
    static {
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            semaforos[i] = new Semaphore(0, true);
            contadorRefeicoes[i] = new AtomicInteger(0);
            estado[i] = PENSANDO;
        }
    }
    
    static int ESQUERDA(int i) {
        return (i + NUM_FILOSOFOS - 1) % NUM_FILOSOFOS;
    }
    
    static int DIREITA(int i) {
        return (i + 1) % NUM_FILOSOFOS;
    }
    
    static class Filosofo implements Runnable {
        private int id;
        
        public Filosofo(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            while (executando) {
                try {
                    pensar();
                    pegar_garfos(id);
                    comer();
                    colocar_garfos(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        private void pensar() throws InterruptedException {
            if (!executando) return;
            System.out.println("Filósofo " + id + " está pensando");
            Thread.sleep((long) (Math.random() * 1000));
        }
        
        private void comer() throws InterruptedException {
            if (!executando) return;
              
            if (estado[ESQUERDA(id)] == COMENDO || estado[DIREITA(id)] == COMENDO) {
                System.err.printf("VIOLAÇÃO: Filósofo %d comendo com vizinho comendo! ESQUERDA %d=%d, DIREITA %d=%d\n", 
                    id, ESQUERDA(id), estado[ESQUERDA(id)], DIREITA(id), estado[DIREITA(id)]);
            }
            System.out.printf("Filósofo %d está comendo (garfos %d e %d)\n", id, id, ESQUERDA(id));
            
            contadorRefeicoes[id].incrementAndGet();
            Thread.sleep((long) (Math.random() * 1000));
        }
    }
    
    static void pegar_garfos(int id) throws InterruptedException {
        if (!executando) return;
        mutex.acquire();
        try {
            estado[id] = FAMINTO;
            testar(id);
        } finally {
            mutex.release();
        }
        semaforos[id].acquire();
    }
    
    static void colocar_garfos(int id) throws InterruptedException {
        if (!executando) return;
        mutex.acquire();
        try {
            estado[id] = PENSANDO;
            testar(ESQUERDA(id));
            testar(DIREITA(id));
        } finally {
            mutex.release();
        }
    }
    
    
    static void testar(int id) {
        if (estado[id] == FAMINTO && 
            estado[ESQUERDA(id)] != COMENDO && 
            estado[DIREITA(id)] != COMENDO) {
            
            estado[id] = COMENDO;
            semaforos[id].release();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Iniciando jantar dos filósofos...");
        System.out.println("Executando por 30 segundos...");
        
        Thread[] filosofos = new Thread[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            filosofos[i] = new Thread(new Filosofo(i));
            filosofos[i].start();
        }
        
        Thread parador = new Thread(() -> {
            try {
                Thread.sleep(30000);
                executando = false;
                System.out.println("\n--- Parando execução após 30 segundos ---");
                
                for (Thread filosofo : filosofos) {
                    filosofo.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        parador.start();
        
        try {
            parador.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
    
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            try {
                filosofos[i].join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        mostrarEstatisticas();
    }
    
    private static void mostrarEstatisticas() {
        System.out.println("\n=== ESTATÍSTICAS FINAIS ===");
        System.out.println("Tempo de execução: 30 segundos");
        System.out.println("\nNúmero de vezes que cada filósofo comeu:");
        
        int totalRefeicoes = 0;
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            int refeicoes = contadorRefeicoes[i].get();
            totalRefeicoes += refeicoes;
            System.out.printf("Filósofo %d: %d vezes\n", i, refeicoes);
        }
        
        System.out.printf("\nTotal de refeições: %d\n", totalRefeicoes);
        System.out.printf("Média de refeições por filósofo: %.2f\n", (double) totalRefeicoes / NUM_FILOSOFOS);
        
        int maxRefeicoes = 0;
        int minRefeicoes = Integer.MAX_VALUE;
        int filosofoMax = -1;
        int filosofoMin = -1;
        
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            int refeicoes = contadorRefeicoes[i].get();
            if (refeicoes > maxRefeicoes) {
                maxRefeicoes = refeicoes;
                filosofoMax = i;
            }
            if (refeicoes < minRefeicoes) {
                minRefeicoes = refeicoes;
                filosofoMin = i;
            }
        }
        
        System.out.printf("Filósofo que mais comeu: %d (%d vezes)\n", filosofoMax, maxRefeicoes);
        System.out.printf("Filósofo que menos comeu: %d (%d vezes)\n", filosofoMin, minRefeicoes);
        System.out.printf("Diferença: %d vezes\n", maxRefeicoes - minRefeicoes);
    }
}

