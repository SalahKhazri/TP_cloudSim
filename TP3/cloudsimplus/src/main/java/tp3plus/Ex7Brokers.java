package tp3plus;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerBestFit;
import org.cloudsimplus.brokers.DatacenterBrokerFirstFit;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.*;
import java.util.function.Function;

/**
 * Exercice 7 (Q2) : 3 VMs de 1000, 2000 et 4000 MIPS et 12 cloudlets de longueurs aléatoires
 * (10 000 à 100 000 MI, graine fixe). Comparaison du makespan selon le broker.
 * Auteur : Khazri Salah Eddine
 */
public class Ex7Brokers {
    static final long SEED = 42;
    static final int[] VM_MIPS = {1000, 2000, 4000};
    static final int CLOUDLETS = 12;

    public static void main(String[] args) {
        Log.setLevel(Level.ERROR);
        final long[] lengths = randomLengths();
        System.out.println("Longueurs des cloudlets (graine " + SEED + ") : " + Arrays.toString(lengths));
        System.out.println("Somme = " + Arrays.stream(lengths).sum() + " MI ; borne inférieure du makespan = somme / 7000 MIPS = "
                + String.format(Locale.US, "%.2f", Arrays.stream(lengths).sum() / 7000.0) + " s");

        final Map<String, Double> results = new LinkedHashMap<>();
        results.put("DatacenterBrokerSimple", run("DatacenterBrokerSimple", DatacenterBrokerSimple::new, lengths, false, false));
        results.put("DatacenterBrokerBestFit", run("DatacenterBrokerBestFit", DatacenterBrokerBestFit::new, lengths, false, false));
        // BestFit ne place qu'un cloudlet par PE libre : les autres restent en attente et ne sont jamais
        // ressoumis. Variante équitable : on ressoumet la file d'attente dès qu'un PE de VM se libère.
        results.put("DatacenterBrokerBestFit + file d'attente", run("DatacenterBrokerBestFit + ressoumission de la file d'attente", DatacenterBrokerBestFit::new, lengths, false, true));
        // Pour aller plus loin : FirstFit et un mapper personnalisé "MCT" (temps de complétion minimal)
        results.put("DatacenterBrokerFirstFit + file d'attente", run("DatacenterBrokerFirstFit + ressoumission de la file d'attente", DatacenterBrokerFirstFit::new, lengths, false, true));
        results.put("Simple + mapper MCT (personnalisé)", run("DatacenterBrokerSimple + mapper MCT", DatacenterBrokerSimple::new, lengths, true, false));

        System.out.println();
        System.out.println("=== Récapitulatif ===");
        results.forEach((k, v) -> System.out.printf(Locale.US, "%-42s makespan = %8.2f s%n", k, v));
    }

    static long[] randomLengths() {
        final var rnd = new Random(SEED);
        final long[] l = new long[CLOUDLETS];
        for (int i = 0; i < CLOUDLETS; i++) l[i] = 10_000 + rnd.nextInt(90_001); // [10 000 ; 100 000]
        return l;
    }

    static double run(String title, Function<CloudSimPlus, DatacenterBroker> brokerFactory, long[] lengths, boolean mct, boolean requeue) {
        final var simulation = new CloudSimPlus();
        final var pes = new ArrayList<Pe>();
        for (int i = 0; i < 4; i++) pes.add(new PeSimple(4000));
        new DatacenterSimple(simulation, List.of(new HostSimple(16_384, 100_000, 1_000_000, pes)));

        final DatacenterBroker broker = brokerFactory.apply(simulation);
        final var vms = new ArrayList<Vm>();
        for (int mips : VM_MIPS) vms.add(new VmSimple(mips, 1).setRam(1024).setBw(1000).setSize(10_000));

        final var cloudlets = new ArrayList<Cloudlet>();
        for (long len : lengths) cloudlets.add(new CloudletSimple(len, 1).setSizes(300));

        if (mct) {
            // MCT : chaque cloudlet va sur la VM où il finirait le plus tôt, compte tenu de la charge déjà affectée
            final Map<Vm, Double> load = new HashMap<>(); // MI déjà affectés à chaque VM
            broker.setVmMapper(cloudlet -> {
                Vm best = Vm.NULL;
                double bestFinish = Double.MAX_VALUE;
                for (Vm vm : broker.getVmExecList()) {
                    double finish = (load.getOrDefault(vm, 0.0) + cloudlet.getLength()) / vm.getMips();
                    if (finish < bestFinish) { bestFinish = finish; best = vm; }
                }
                load.merge(best, (double) cloudlet.getLength(), Double::sum);
                return best;
            });
        }

        if (requeue) {
            // à chaque avancée de l'horloge, si une VM a un PE libre, on ressoumet les cloudlets en attente
            simulation.addOnClockTickListener(info -> {
                final boolean freePe = broker.getVmExecList().stream().anyMatch(vm -> vm.getExpectedFreePesNumber() > 0);
                if (freePe && !broker.getCloudletWaitingList().isEmpty()) {
                    final List<Cloudlet> waiting = new ArrayList<>(broker.getCloudletWaitingList());
                    broker.getCloudletWaitingList().clear();
                    broker.submitCloudletList(waiting);
                }
            });
        }

        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);
        simulation.start();

        final List<Cloudlet> finished = new ArrayList<>(broker.getCloudletFinishedList());
        finished.sort(Comparator.comparingLong(Cloudlet::getId));
        new CloudletsTableBuilder(finished).setTitle(title).build();
        final double makespan = finished.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0);
        final Map<Long, Integer> perVm = new TreeMap<>();
        finished.forEach(c -> perVm.merge(c.getVm().getId(), 1, Integer::sum));
        System.out.printf(Locale.US, "Cloudlets terminés : %d/%d ; cloudlets par VM (id -> nb) : %s ; makespan = %.2f s%n",
                finished.size(), lengths.length, perVm, makespan);
        return makespan;
    }
}
