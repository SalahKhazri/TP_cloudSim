package tp3;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Fonctions utilitaires communes aux exercices CloudSim 7 du TP3.
 * Auteur : Khazri Salah Eddine
 */
public final class Cs7Util {
    private Cs7Util() {}

    /** Type d'ordonnanceur de VM utilisé par un hôte. */
    public enum VmSched { TIME_SHARED, SPACE_SHARED }

    /** Crée un hôte de {@code pes} cœurs à {@code mips} MIPS chacun. */
    public static Host createHost(int id, int pes, int mips, int ram, VmSched sched) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < pes; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(mips)));
        }
        VmScheduler vmScheduler = sched == VmSched.SPACE_SHARED
                ? new VmSchedulerSpaceShared(peList)
                : new VmSchedulerTimeShared(peList);
        return new Host(id, new RamProvisionerSimple(ram), new BwProvisionerSimple(100_000),
                1_000_000, peList, vmScheduler);
    }

    /** Crée un datacenter avec les coûts donnés (cost = $/s de CPU). */
    public static Datacenter createDatacenter(String name, List<Host> hosts, double costPerSec,
                                              double costPerMem, double costPerStorage, double costPerBw) {
        return createDatacenter(name, hosts, costPerSec, costPerMem, costPerStorage, costPerBw,
                new VmAllocationPolicySimple(hosts));
    }

    public static Datacenter createDatacenter(String name, List<Host> hosts, double costPerSec,
                                              double costPerMem, double costPerStorage, double costPerBw,
                                              VmAllocationPolicy policy) {
        DatacenterCharacteristics ch = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hosts, 10.0, costPerSec, costPerMem, costPerStorage, costPerBw);
        try {
            return new Datacenter(name, ch, policy, new LinkedList<>(), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Vm createVm(int id, int brokerId, double mips, int pes, int ram, boolean spaceSharedCloudlets) {
        CloudletScheduler cs = spaceSharedCloudlets ? new CloudletSchedulerSpaceShared() : new CloudletSchedulerTimeShared();
        return new Vm(id, brokerId, mips, pes, ram, 1000, 10_000, "Xen", cs);
    }

    public static Cloudlet createCloudlet(int id, int brokerId, long length) {
        UtilizationModel full = new UtilizationModelFull();
        Cloudlet c = new Cloudlet(id, length, 1, 300, 300, full, full, full);
        c.setUserId(brokerId);
        return c;
    }

    /** Initialise CloudSim en désactivant les traces (sortie lisible). */
    public static void init(int users, boolean verbose) {
        if (!verbose) {
            Log.disable();
        } else {
            Log.enable();
        }
        CloudSim.init(users, java.util.Calendar.getInstance(), false);
    }

    public static String f(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    /** Makespan = date de fin du dernier cloudlet. */
    public static double makespan(List<? extends Cloudlet> list) {
        return list.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(0);
    }
}
