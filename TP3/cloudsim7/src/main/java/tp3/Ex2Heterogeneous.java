package tp3;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static tp3.Cs7Util.f;

/**
 * Exercice 2 : hôtes hétérogènes et temps d'exécution (basé sur CloudSimExample3).
 * 4 hôtes / 4 VMs de puissances différentes, 5 cloudlets de 30 000 à 150 000 MI.
 * Auteur : Khazri Salah Eddine
 */
public class Ex2Heterogeneous {
    // Hôtes : {PEs, MIPS par PE, RAM}
    static final int[][] HOSTS = {{1, 1000, 2048}, {2, 1500, 4096}, {4, 2000, 8192}, {2, 3000, 4096}};
    // VMs : {PEs, MIPS par PE}   (VM i créée sur l'hôte i)
    static final int[][] VMS = {{1, 1000}, {1, 1500}, {2, 2000}, {1, 3000}};
    static final long[] LENGTHS = {30_000, 60_000, 90_000, 120_000, 150_000};

    public static void main(String[] args) {
        // Q1-Q3 : affectation naïve (cloudlet i -> VM i mod 4, comme le round-robin du broker)
        int[] naive = new int[LENGTHS.length];
        for (int i = 0; i < LENGTHS.length; i++) naive[i] = i % VMS.length;
        double m1 = run("Affectation initiale (cloudlet i -> VM i mod 4)", naive);

        // Q4 : heuristique LJF (Longest Job First) + choix de la VM qui termine le plus tôt
        int[] ljf = ljfAssignment();
        double m2 = run("Affectation optimisée LJF (plus long cloudlet -> VM qui le termine le plus tôt)", ljf);

        System.out.println();
        System.out.println("Makespan initial = " + f(m1) + " s ; makespan LJF = " + f(m2)
                + " s ; gain = " + f(100 * (m1 - m2) / m1) + " %");
    }

    /**
     * LJF : on trie les cloudlets par longueur décroissante et on place chacun sur le "slot" (PE de VM)
     * qui le terminerait le plus tôt. Une VM à 2 PEs offre 2 slots indépendants.
     */
    static int[] ljfAssignment() {
        List<double[]> slots = new ArrayList<>(); // {vmId, mips, dateDeDisponibilité}
        for (int v = 0; v < VMS.length; v++)
            for (int p = 0; p < VMS[v][0]; p++) slots.add(new double[]{v, VMS[v][1], 0});

        Integer[] order = new Integer[LENGTHS.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, Comparator.comparingLong((Integer i) -> LENGTHS[i]).reversed());

        int[] assign = new int[LENGTHS.length];
        for (int c : order) {
            double[] best = null;
            double bestFinish = Double.MAX_VALUE;
            for (double[] s : slots) {
                double finish = s[2] + LENGTHS[c] / s[1];
                if (finish < bestFinish) { bestFinish = finish; best = s; }
            }
            best[2] = bestFinish;
            assign[c] = (int) best[0];
        }
        return assign;
    }

    static double run(String title, int[] assignment) {
        Cs7Util.init(1, false);

        List<Host> hosts = new ArrayList<>();
        for (int h = 0; h < HOSTS.length; h++)
            hosts.add(Cs7Util.createHost(h, HOSTS[h][0], HOSTS[h][1], HOSTS[h][2], Cs7Util.VmSched.TIME_SHARED));
        Cs7Util.createDatacenter("Datacenter_0", hosts, 3.0, 0.05, 0.001, 0.0, new VmAllocationPolicyFixed(hosts));

        RecordingBroker broker;
        try {
            broker = new RecordingBroker("Broker");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int brokerId = broker.getId();

        List<Vm> vms = new ArrayList<>();
        for (int v = 0; v < VMS.length; v++)
            vms.add(Cs7Util.createVm(v, brokerId, VMS[v][1], VMS[v][0], 1024, false));

        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int i = 0; i < LENGTHS.length; i++) {
            Cloudlet c = Cs7Util.createCloudlet(i, brokerId, LENGTHS[i]);
            c.setGuestId(assignment[i]);
            cloudlets.add(c);
        }
        broker.submitGuestList(vms);
        broker.submitCloudletList(cloudlets);

        CloudSim.startSimulation();
        List<Cloudlet> result = new ArrayList<>(broker.getCloudletReceivedList());
        CloudSim.stopSimulation();
        result.sort(Comparator.comparingInt(Cloudlet::getCloudletId));

        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.println("Placement des VMs :");
        for (Vm vm : vms)
            System.out.println("  VM " + vm.getId() + " (" + vm.getNumberOfPes() + " PE x " + (int) vm.getMips()
                    + " MIPS) -> " + broker.placement.get(vm.getId()));
        System.out.printf("%-8s %-8s %-3s %-8s %-8s %-8s %-10s %-14s%n",
                "Cloudlet", "Length", "VM", "VM MIPS", "Start", "Finish", "Exec time", "Length/MIPS");
        for (Cloudlet c : result) {
            double mips = VMS[c.getGuestId()][1];
            System.out.printf("%-8d %-8d %-3d %-8d %-8s %-8s %-10s %-14s%n",
                    c.getCloudletId(), c.getCloudletLength(), c.getGuestId(), (int) mips,
                    f(c.getExecStartTime()), f(c.getExecFinishTime()), f(c.getActualCPUTime()),
                    f(c.getCloudletLength() / mips));
        }
        double makespan = Cs7Util.makespan(result);
        System.out.println("Makespan = " + f(makespan) + " s");
        return makespan;
    }
}
