package tp3;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Broker standard de CloudSim 7 qui mémorise, pour chaque VM, le datacenter et l'hôte
 * où elle a été créée (ces informations sont perdues après la destruction des VMs).
 * Auteur : Khazri Salah Eddine
 */
public class RecordingBroker extends DatacenterBroker {
    /** vmId -> "Datacenter_x / Host y" ou "ECHEC" */
    public final Map<Integer, String> placement = new LinkedHashMap<>();

    public RecordingBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void processVmCreateAck(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int vmId = data[1];
        boolean ok = data[2] == org.cloudbus.cloudsim.core.CloudSimTags.TRUE;
        super.processVmCreateAck(ev);
        GuestEntity vm = VmList.getById(getGuestList(), vmId);
        if (ok && vm != null && vm.getHost() != null) {
            placement.put(vmId, CloudSim.getEntityName(data[0]) + " / Host " + vm.getHost().getId());
        } else if (!placement.containsKey(vmId) || placement.get(vmId).startsWith("ECHEC")) {
            String prev = placement.getOrDefault(vmId, "ECHEC dans");
            placement.put(vmId, prev + " " + CloudSim.getEntityName(data[0]));
        }
    }
}
