package tp3plus;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.allocationpolicies.*;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
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
import org.cloudsimplus.vms.VmSimple;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Exercice 6 : politiques d'allocation de VMs.
 * 1 datacenter, 4 hôtes (8, 4, 4 et 2 PEs de 1000 MIPS), 8 VMs de 1 à 4 PEs.
 * Auteur : Khazri Salah Eddine
 */
public class Ex6AllocationPolicies {
    static final int[] HOST_PES = {8, 4, 4, 2};
    static int[] vmPes;

    public static void main(String[] args) {
        Log.setLevel(Level.ERROR);
        // Scénario 1 : 8 VMs = 16 PEs demandés pour 18 disponibles (il reste de la marge)
        // Scénario 2 : 8 VMs = 18 PEs demandés pour 18 disponibles (tout rentre, mais sans marge)
        // Scénario 3 : charge légère, 8 VMs = 12 PEs (rentre sur 2 hôtes seulement : 8 + 4)
        int[][] scenarios = {{2, 4, 1, 3, 2, 1, 1, 2}, {2, 4, 1, 3, 2, 1, 3, 2}, {2, 1, 3, 1, 2, 1, 1, 1}};
        for (int[] sc : scenarios) {
            vmPes = sc;
            System.out.println();
            System.out.println("##### VMs (PEs) = " + Arrays.toString(sc) + " -> " + Arrays.stream(sc).sum()
                    + " PEs demandés / " + Arrays.stream(HOST_PES).sum() + " disponibles #####");
            System.out.printf("%-30s | %-5s | %-62s | %-12s | %-8s%n", "Politique", "Hôtes", "VMs placées sur chaque hôte (VM:PEs)", "VMs échouées", "Makespan");
            run("VmAllocationPolicySimple", VmAllocationPolicySimple::new);
            run("VmAllocationPolicyFirstFit", VmAllocationPolicyFirstFit::new);
            run("VmAllocationPolicyBestFit", VmAllocationPolicyBestFit::new);
            run("VmAllocationPolicyRoundRobin", VmAllocationPolicyRoundRobin::new);
        }
    }

    static void run(String name, Supplier<VmAllocationPolicy> policy) {
        final var simulation = new CloudSimPlus();
        final var hosts = new ArrayList<Host>();
        for (int pes : HOST_PES) {
            final var peList = new ArrayList<Pe>();
            for (int i = 0; i < pes; i++) peList.add(new PeSimple(1000));
            hosts.add(new HostSimple(16_384, 100_000, 1_000_000, peList));
        }
        new DatacenterSimple(simulation, hosts, policy.get());

        final var broker = new DatacenterBrokerSimple(simulation);
        broker.getVmCreation().setRetryDelay(-1); // ne pas réessayer indéfiniment de créer les VMs refusées
        simulation.terminateAt(1000);      // garde-fou
        final Map<Vm, Host> placement = new LinkedHashMap<>();
        final var vms = new ArrayList<Vm>();
        final var cloudlets = new ArrayList<Cloudlet>();
        for (int pes : vmPes) {
            final var vm = new VmSimple(1000, pes).setRam(1024).setBw(1000).setSize(10_000);
            vm.addOnHostAllocationListener(info -> placement.put(info.getVm(), info.getHost()));
            vms.add(vm);
            // un cloudlet par VM, qui utilise tous ses PEs pendant 20 s
            final var c = new CloudletSimple(20_000, pes).setSizes(300);
            c.setVm(vm);
            cloudlets.add(c);
        }
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);
        simulation.start();

        // VMs regroupées par hôte
        final Map<Long, List<String>> byHost = new TreeMap<>();
        placement.forEach((vm, host) -> byHost.computeIfAbsent(host.getId(), k -> new ArrayList<>())
                .add(vm.getId() + ":" + vm.getPesNumber()));
        final String perHost = hosts.stream()
                .map(h -> "H" + h.getId() + "[" + String.join(",", byHost.getOrDefault(h.getId(), List.of())) + "]")
                .collect(Collectors.joining(" "));
        final String failed = broker.getVmFailedList().isEmpty() ? "aucune"
                : broker.getVmFailedList().stream().map(vm -> "VM" + vm.getId()).collect(Collectors.joining(","));
        final double makespan = broker.getCloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime).max().orElse(0);
        final long finished = broker.getCloudletFinishedList().size();
        System.out.printf("%-30s | %-5d | %-62s | %-12s | %.2f s (%d/%d cloudlets)%n",
                name, byHost.size(), perHost, failed, makespan, finished, cloudlets.size());
    }
}
