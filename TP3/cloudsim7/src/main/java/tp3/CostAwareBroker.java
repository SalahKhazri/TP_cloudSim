package tp3;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Broker qui essaie de créer ses VMs d'abord dans le datacenter le MOINS CHER
 * (tri des datacenters par coût CPU croissant), au lieu du premier de la liste.
 * Auteur : Khazri Salah Eddine
 */
public class CostAwareBroker extends RecordingBroker {

    public CostAwareBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics ch = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(ch.getId(), ch);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            // copie triée : on ne modifie pas la liste globale de CloudSim
            List<Integer> sorted = new ArrayList<>(getDatacenterIdsList());
            sorted.sort(Comparator.comparingDouble(id -> getDatacenterCharacteristicsList().get(id).getCostPerSecond()));
            setDatacenterIdsList(sorted);
            setDatacenterRequestedIdsList(new ArrayList<>());
            createVmsInDatacenter(sorted.getFirst());
        }
    }
}
