package tp3;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static tp3.Cs7Util.f;

/**
 * Exercice 3 : plusieurs datacenters et plusieurs utilisateurs (basé sur CloudSimExample4 et 5).
 * 3 datacenters (2 hôtes chacun, coût CPU 3.0, 2.0 et 1.0 $/s), 3 utilisateurs (brokers),
 * chacun avec 2 VMs et 3 cloudlets.
 * Auteur : Khazri Salah Eddine
 */
public class Ex3MultiDatacenters {
    static final double[] DC_COSTS = {3.0, 2.0, 1.0};
    static final long[] LENGTHS = {20_000, 40_000, 60_000}; // 3 cloudlets par utilisateur
    static final int USERS = 3;

    public static void main(String[] args) {
        run("A - Brokers par défaut, cloudlets distribués en round-robin", false, false);
        run("B - Optimisation du temps : affectation LJF des cloudlets", false, true);
        run("C - Optimisation du coût : broker qui choisit le datacenter le moins cher", true, false);
        run("D - Coût + temps : broker 'moins cher' + affectation LJF", true, true);
    }

    static void run(String title, boolean costAware, boolean ljf) {
        Cs7Util.init(USERS, false); // CloudSim doit connaître le nombre d'utilisateurs (= brokers)

        for (int d = 0; d < DC_COSTS.length; d++) {
            List<Host> hosts = new ArrayList<>();
            for (int h = 0; h < 2; h++)
                hosts.add(Cs7Util.createHost(h, 2, 1000, 4096, Cs7Util.VmSched.TIME_SHARED));
            Cs7Util.createDatacenter("Datacenter_" + d, hosts, DC_COSTS[d], 0.05, 0.001, 0.0);
        }

        List<RecordingBroker> brokers = new ArrayList<>();
        int vmId = 0, cloudletId = 0;
        for (int u = 0; u < USERS; u++) {
            RecordingBroker broker;
            try {
                broker = costAware ? new CostAwareBroker("Broker_" + u) : new RecordingBroker("Broker_" + u);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<Vm> vms = new ArrayList<>();
            for (int v = 0; v < 2; v++)
                vms.add(Cs7Util.createVm(vmId++, broker.getId(), 1000, 1, 1024, false));
            List<Cloudlet> cls = new ArrayList<>();
            for (int c = 0; c < LENGTHS.length; c++) {
                Cloudlet cl = Cs7Util.createCloudlet(cloudletId++, broker.getId(), LENGTHS[c]);
                if (ljf) {
                    // LJF : le plus long cloudlet seul sur la 1re VM, les deux autres sur la 2e
                    cl.setGuestId(c == 2 ? vms.get(0).getId() : vms.get(1).getId());
                }
                cls.add(cl);
            }
            broker.submitGuestList(vms);
            broker.submitCloudletList(cls);
            brokers.add(broker);
        }

        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.printf("%-9s %-8s %-3s %-22s %-8s %-7s %-8s %-8s %-8s%n",
                "User", "Cloudlet", "VM", "Datacenter / Host", "Length", "Start", "Finish", "Time", "Cost($)");
        double globalMakespan = 0, globalCost = 0;
        for (RecordingBroker b : brokers) {
            List<Cloudlet> list = new ArrayList<>(b.getCloudletReceivedList());
            list.sort(Comparator.comparingInt(Cloudlet::getCloudletId));
            double cost = 0;
            for (Cloudlet c : list) {
                double cpuCost = c.getActualCPUTime() * c.getCostPerSec();
                cost += cpuCost;
                System.out.printf("%-9s %-8d %-3d %-22s %-8d %-7s %-8s %-8s %-8s%n",
                        b.getName(), c.getCloudletId(), c.getGuestId(), b.placement.get(c.getGuestId()),
                        c.getCloudletLength(), f(c.getExecStartTime()), f(c.getExecFinishTime()),
                        f(c.getActualCPUTime()), f(cpuCost));
            }
            double ms = Cs7Util.makespan(list);
            globalMakespan = Math.max(globalMakespan, ms);
            globalCost += cost;
            System.out.println("  -> " + b.getName() + " : makespan = " + f(ms) + " s, coût total = " + f(cost) + " $");
        }
        System.out.println("Global : makespan = " + f(globalMakespan) + " s, coût total des 3 utilisateurs = " + f(globalCost) + " $");
    }
}
