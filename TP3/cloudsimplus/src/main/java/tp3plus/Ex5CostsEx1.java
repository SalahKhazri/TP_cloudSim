package tp3plus;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Exercice 5 (Q4) : scénario de l'exercice 1 (1 hôte de 1 PE x 1000 MIPS, 3 VMs de 250 MIPS,
 * cloudlets de 40 000, 80 000 et 120 000 MI, 3 $/s) refait avec CloudSim Plus et VmCost,
 * pour comparer les deux modèles de coût.
 * Auteur : Khazri Salah Eddine
 */
public class Ex5CostsEx1 {
    public static void main(String[] args) {
        Log.setLevel(Level.WARN);
        run(false);
        run(true);
    }

    static void run(boolean destroyIdleVms) {
        final var simulation = new CloudSimPlus();
        final var host = new HostSimple(2048, 10_000, 1_000_000, List.of(new PeSimple(1000)));
        host.setVmScheduler(new VmSchedulerTimeShared()); // comme CloudSimExample2
        final var dc = new DatacenterSimple(simulation, List.of(host));
        dc.getCharacteristics().setCostPerSecond(3.0).setCostPerMem(0.05).setCostPerStorage(0.001).setCostPerBw(0.0);

        final var broker = new DatacenterBrokerSimple(simulation);
        if (destroyIdleVms) broker.setVmDestructionDelay(1.0);
        final long[] lengths = {40_000, 80_000, 120_000};
        final var vms = new ArrayList<Vm>();
        final var cloudlets = new ArrayList<Cloudlet>();
        for (long len : lengths) {
            final var vm = new VmSimple(250, 1).setRam(512).setBw(1000).setSize(10_000);
            vms.add(vm);
            final var c = new CloudletSimple(len, 1).setSizes(300);
            c.setVm(vm);
            cloudlets.add(c);
        }
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);
        simulation.start();

        new CloudletsTableBuilder(broker.getCloudletFinishedList())
                .setTitle("Scénario de l'exercice 1 avec CloudSim Plus" + (destroyIdleVms ? " (VMs détruites dès qu'inactives)" : "")).build();
        double total = 0;
        for (Vm vm : broker.getVmCreatedList()) {
            final var cost = new VmCost(vm);
            total += cost.getTotalCost();
            System.out.printf("  VM %d : CPU = %7.2f $ | RAM = %6.2f $ | Stockage = %6.2f $ | Total = %7.2f $ (durée %.2f s)%n",
                    vm.getId(), cost.getProcessingCost(), cost.getMemoryCost(), cost.getStorageCost(),
                    cost.getTotalCost(), vm.getTotalExecutionTime());
        }
        System.out.printf("Coût total = %.2f $%n%n", total);
    }
}
