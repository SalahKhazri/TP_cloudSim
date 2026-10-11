package tp3;

import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Politique d'allocation qui place la VM d'identifiant i sur l'hôte d'indice i
 * (placement imposé par le tableau de l'exercice 2).
 * Auteur : Khazri Salah Eddine
 */
public class VmAllocationPolicyFixed extends VmAllocationPolicySimple {
    public VmAllocationPolicyFixed(List<? extends HostEntity> list) {
        super(list);
    }

    @Override
    public boolean allocateHostForGuest(GuestEntity guest) {
        HostEntity host = getHostList().get(guest.getId() % getHostList().size());
        return allocateHostForGuest(guest, host);
    }
}
