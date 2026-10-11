package tp3;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static tp3.Cs7Util.f;

/**
 * Exercice 4 : ordonnancement time-shared vs space-shared.
 * 1 hôte (1 PE de 1000 MIPS), 1 VM (1 PE), 3 cloudlets de 10 000 MI soumis en même temps.
 * Auteur : Khazri Salah Eddine
 */
public class Ex4Schedulers {

    public static void main(String[] args) {
        // Q1/Q2 : ordonnanceur de cloudlets (dans la VM)
        cloudletScheduling(true);
        cloudletScheduling(false);

        // Q3 : ordonnanceur de VMs (dans l'hôte) avec 2 VMs de 1 PE sur l'hôte de 1 PE
        vmScheduling(Cs7Util.VmSched.SPACE_SHARED, 1000);
        vmScheduling(Cs7Util.VmSched.TIME_SHARED, 1000);
        vmScheduling(Cs7Util.VmSched.SPACE_SHARED, 500);
        vmScheduling(Cs7Util.VmSched.TIME_SHARED, 500);
    }

    static void cloudletScheduling(boolean spaceShared) {
        Cs7Util.init(1, false);
        List<Host> hosts = new ArrayList<>();
        hosts.add(Cs7Util.createHost(0, 1, 1000, 2048, Cs7Util.VmSched.TIME_SHARED));
        Cs7Util.createDatacenter("Datacenter_0", hosts, 3.0, 0.05, 0.001, 0.0);
        RecordingBroker broker = newBroker();

        List<Vm> vms = List.of(Cs7Util.createVm(0, broker.getId(), 1000, 1, 512, spaceShared));
        List<Cloudlet> cls = new ArrayList<>();
        for (int i = 0; i < 3; i++) cls.add(Cs7Util.createCloudlet(i, broker.getId(), 10_000));
        broker.submitGuestList(vms);
        broker.submitCloudletList(cls);

        CloudSim.startSimulation();
        CloudSim.stopSimulation();
        print("CloudletScheduler" + (spaceShared ? "SpaceShared" : "TimeShared"), broker);
    }

    static void vmScheduling(Cs7Util.VmSched sched, double vmMips) {
        Cs7Util.init(1, false);
        List<Host> hosts = new ArrayList<>();
        hosts.add(Cs7Util.createHost(0, 1, 1000, 2048, sched));
        Cs7Util.createDatacenter("Datacenter_0", hosts, 3.0, 0.05, 0.001, 0.0);
        RecordingBroker broker = newBroker();

        List<Vm> vms = new ArrayList<>();
        for (int v = 0; v < 2; v++) vms.add(Cs7Util.createVm(v, broker.getId(), vmMips, 1, 512, false));
        List<Cloudlet> cls = new ArrayList<>();
        for (int i = 0; i < 3; i++) cls.add(Cs7Util.createCloudlet(i, broker.getId(), 10_000));
        broker.submitGuestList(vms);
        broker.submitCloudletList(cls);

        CloudSim.startSimulation();
        CloudSim.stopSimulation();
        String name = "VmScheduler" + (sched == Cs7Util.VmSched.SPACE_SHARED ? "SpaceShared" : "TimeShared")
                + " - 2 VMs de 1 PE x " + (int) vmMips + " MIPS";
        print(name, broker);
        for (Vm vm : vms)
            System.out.println("  VM " + vm.getId() + " : " + broker.placement.get(vm.getId()));
    }

    static RecordingBroker newBroker() {
        try {
            return new RecordingBroker("Broker");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void print(String title, DatacenterBroker broker) {
        List<Cloudlet> list = new ArrayList<>(broker.getCloudletReceivedList());
        list.sort(Comparator.comparingInt(Cloudlet::getCloudletId));
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.printf("%-8s %-3s %-8s %-8s %-8s%n", "Cloudlet", "VM", "Start", "Finish", "Time");
        for (Cloudlet c : list)
            System.out.printf("%-8d %-3d %-8s %-8s %-8s%n", c.getCloudletId(), c.getGuestId(),
                    f(c.getExecStartTime()), f(c.getExecFinishTime()), f(c.getActualCPUTime()));
        System.out.println("Makespan = " + f(Cs7Util.makespan(list)) + " s");
    }
}
