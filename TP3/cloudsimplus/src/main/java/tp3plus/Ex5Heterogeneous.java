package tp3plus;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Exercice 5 (Q2 et Q4) : réécriture de l'exercice 2 avec CloudSim Plus 8.5.7,
 * puis ajout des coûts du datacenter et affichage du coût de chaque VM (VmCost).
 * Auteur : Khazri Salah Eddine
 */
public class Ex5Heterogeneous {
    static final int[][] HOSTS = {{1, 1000, 2048}, {2, 1500, 4096}, {4, 2000, 8192}, {2, 3000, 4096}};
    static final int[][] VMS = {{1, 1000}, {1, 1500}, {2, 2000}, {1, 3000}};
    static final long[] LENGTHS = {30_000, 60_000, 90_000, 120_000, 150_000};
    static final int[] NAIVE = {0, 1, 2, 3, 0};
    static final int[] LJF = {0, 1, 2, 2, 3}; // même affectation LJF que dans l'exercice 2

    public static void main(String[] args) {
        Log.setLevel(Level.WARN);
        run("Affectation initiale (cloudlet i -> VM i mod 4)", NAIVE, false);
        run("Affectation LJF", LJF, false);
        // Variante : chaque VM est détruite dès qu'elle devient inactive (on ne paie plus le temps d'attente)
        run("Affectation LJF + destruction des VMs inactives", LJF, true);
    }

    static void run(String title, int[] assignment, boolean destroyIdleVms) {
        final var simulation = new CloudSimPlus();

        final var hosts = new ArrayList<Host>();
        for (int[] h : HOSTS) {
            final var pes = new ArrayList<Pe>();
            for (int i = 0; i < h[0]; i++) pes.add(new PeSimple(h[1]));
            hosts.add(new HostSimple(h[2], 100_000, 1_000_000, pes));
        }
        // VM i sur l'hôte i (comme dans le tableau de l'énoncé)
        final var policy = new VmAllocationPolicySimple();
        policy.setFindHostForVmFunction((p, vm) -> Optional.of(hosts.get((int) vm.getId())));
        final var datacenter = new DatacenterSimple(simulation, hosts, policy);
        // Q4 : coûts identiques à l'exercice 1 (CloudSimExample2)
        datacenter.getCharacteristics()
                .setCostPerSecond(3.0).setCostPerMem(0.05).setCostPerStorage(0.001).setCostPerBw(0.0);

        final var broker = new DatacenterBrokerSimple(simulation);
        if (destroyIdleVms) broker.setVmDestructionDelay(1.0);
        final var vms = new ArrayList<Vm>();
        for (int[] v : VMS) vms.add(new VmSimple(v[1], v[0]).setRam(1024).setBw(1000).setSize(10_000));

        final var cloudlets = new ArrayList<Cloudlet>();
        for (int i = 0; i < LENGTHS.length; i++) {
            final var c = new CloudletSimple(LENGTHS[i], 1);
            c.setSizes(300);
            c.setVm(vms.get(assignment[i])); // équivalent de cloudlet.setGuestId(vmId)
            cloudlets.add(c);
        }
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);

        simulation.start();

        final List<Cloudlet> finished = broker.getCloudletFinishedList();
        finished.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        new CloudletsTableBuilder(finished).setTitle(title).build();
        System.out.printf("Makespan = %.2f s%n", finished.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0));

        // Q4 : coût total de chaque VM
        System.out.println("Coût par VM (VmCost) :");
        double total = 0;
        for (Vm vm : broker.getVmCreatedList()) {
            final var cost = new VmCost(vm);
            total += cost.getTotalCost();
            System.out.printf("  VM %d : CPU = %8.2f $ | RAM = %6.2f $ | Stockage = %6.2f $ | BW = %4.2f $ | Total = %8.2f $ (durée d'exécution %.2f s)%n",
                    vm.getId(), cost.getProcessingCost(), cost.getMemoryCost(), cost.getStorageCost(),
                    cost.getBwCost(), cost.getTotalCost(), vm.getTotalExecutionTime());
        }
        System.out.printf("Coût total des VMs = %.2f $%n%n", total);
    }
}
