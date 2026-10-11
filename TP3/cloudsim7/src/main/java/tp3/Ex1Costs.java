package tp3;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

import static tp3.Cs7Util.f;

/**
 * Exercice 1 : coût des cloudlets (basé sur CloudSimExample2).
 * Un datacenter, un hôte (1 PE de 1000 MIPS), des VMs de 250 MIPS, un cloudlet par VM.
 * Auteur : Khazri Salah Eddine
 */
public class Ex1Costs {

    public static void main(String[] args) {
        // Q1 : exemple 2 inchangé (2 VMs de 250 MIPS, 2 cloudlets de 250 000 MI, coût 3 $/s)
        run("Q1 - CloudSimExample2 inchangé", new long[]{250_000, 250_000}, 3.0, 0.0);
        // Q2/Q3 : 3 cloudlets de longueurs différentes + colonne Cost
        run("Q2/Q3 - 3 cloudlets (40k, 80k, 120k MI), cost=3.0 $/s, costPerBw=0.0", new long[]{40_000, 80_000, 120_000}, 3.0, 0.0);
        // Q4 : pour comprendre ce que mesure getProcessingCost(), on met un coût de bande passante non nul
        run("Q4 - même scénario avec costPerBw=0.1 $/MB", new long[]{40_000, 80_000, 120_000}, 3.0, 0.1);
        // Q5 : coût par seconde doublé
        run("Q5 - coût par seconde doublé (6.0 $/s)", new long[]{40_000, 80_000, 120_000}, 6.0, 0.0);
    }

    static void run(String title, long[] lengths, double costPerSec, double costPerBw) {
        Cs7Util.init(1, false);

        List<Host> hosts = new ArrayList<>();
        hosts.add(Cs7Util.createHost(0, 1, 1000, 2048, Cs7Util.VmSched.TIME_SHARED));
        // mêmes coûts que CloudSimExample2 : 3.0 $/s CPU, 0.05 $/MB RAM, 0.001 $/MB stockage
        Datacenter dc = Cs7Util.createDatacenter("Datacenter_0", hosts, costPerSec, 0.05, 0.001, costPerBw);

        DatacenterBroker broker;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int brokerId = broker.getId();

        List<Vm> vms = new ArrayList<>();
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int i = 0; i < lengths.length; i++) {
            Vm vm = Cs7Util.createVm(i, brokerId, 250, 1, 512, false);
            vms.add(vm);
            Cloudlet c = Cs7Util.createCloudlet(i, brokerId, lengths[i]);
            c.setGuestId(vm.getId()); // un cloudlet par VM (équivalent de bindCloudletToVm)
            cloudlets.add(c);
        }
        broker.submitGuestList(vms);
        broker.submitCloudletList(cloudlets);

        CloudSim.startSimulation();
        List<Cloudlet> result = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.printf("%-8s %-8s %-4s %-3s %-8s %-9s %-9s %-9s %-10s %-12s%n",
                "Cloudlet", "STATUS", "DC", "VM", "Length", "CPU Time", "Start", "Finish", "Cost", "CPU cost(*)");
        double totalDisplayed = 0, totalCpu = 0;
        for (Cloudlet c : result) {
            double cpuCost = c.getActualCPUTime() * c.getCostPerSec();
            totalDisplayed += c.getProcessingCost();
            totalCpu += cpuCost;
            System.out.printf("%-8d %-8s %-4d %-3d %-8d %-9s %-9s %-9s %-10s %-12s%n",
                    c.getCloudletId(), c.getStatus() == Cloudlet.CloudletStatus.SUCCESS ? "SUCCESS" : "FAILED",
                    c.getResourceId(), c.getGuestId(), c.getCloudletLength(), f(c.getActualCPUTime()),
                    f(c.getExecStartTime()), f(c.getExecFinishTime()), f(c.getProcessingCost()), f(cpuCost));
        }
        System.out.println("(*) CPU cost = actual CPU time x cost per second (" + costPerSec + " $/s)");
        System.out.println("Total Cost (getProcessingCost) = " + f(totalDisplayed)
                + " | Total CPU cost = " + f(totalCpu)
                + " | Total utilisateur (CPU + transfert) = " + f(totalDisplayed + totalCpu));
    }
}
