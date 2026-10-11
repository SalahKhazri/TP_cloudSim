# TP3 Cloud Computing — CloudSim 7.0.1 & CloudSim Plus 8.5.7

**Réalisé par :** Khazri Salah Eddine — LSI S5

**Encadré par :** Prof. C. El Amrani

Ce dépôt contient le projet CloudSim 7.0.1 complet, plus mon travail pour l'**Assignment 3** :

| Dossier / fichier | Contenu |
|---|---|
| [`Rapport/Rapport_TP3_Khazri_Salah_Eddine.pdf`](Rapport/Rapport_TP3_Khazri_Salah_Eddine.pdf) | **Rapport du TP** (version PDF) |
| [`Rapport/Rapport_TP3_Khazri_Salah_Eddine.docx`](Rapport/Rapport_TP3_Khazri_Salah_Eddine.docx) | Rapport (version Word) |
| [`Rapport/Assignment_3.pdf`](Rapport/Assignment_3.pdf) | Énoncé |
| [`TP3/cloudsim7/`](TP3/cloudsim7/) | Partie 1 : exercices 1 à 4 (CloudSim 7.0.1) |
| [`TP3/cloudsimplus/`](TP3/cloudsimplus/) | Partie 2 : exercices 5 à 7 (CloudSim Plus 8.5.7) |
| [`TP3/outputs/`](TP3/outputs/) | Sorties complètes de toutes les exécutions |

## Exercices

| Exercice | Classe principale | Sujet |
|---|---|---|
| 1 | `tp3.Ex1Costs` | Coût des cloudlets (basé sur CloudSimExample2) |
| 2 | `tp3.Ex2Heterogeneous` | Hôtes hétérogènes, temps d'exécution, heuristique LJF |
| 3 | `tp3.Ex3MultiDatacenters` | 3 datacenters, 3 utilisateurs, broker sensible au coût |
| 4 | `tp3.Ex4Schedulers` | Ordonnancement time-shared vs space-shared |
| 5 | `tp3plus.BasicFirstExample`, `tp3plus.Ex5Heterogeneous`, `tp3plus.Ex5CostsEx1` | Passage à CloudSim Plus, coûts avec `VmCost` |
| 6 | `tp3plus.Ex6AllocationPolicies` | Politiques d'allocation de VMs (Simple, FirstFit, BestFit, RoundRobin) |
| 7 | `tp3plus.Ex7Brokers` | Brokers et répartition de charge (Simple vs BestFit) |

Classes auxiliaires CloudSim 7 : `Cs7Util` (création des entités), `RecordingBroker` (mémorise le datacenter et l'hôte de chaque VM), `VmAllocationPolicyFixed` (VM i sur l'hôte i), `CostAwareBroker` (choisit d'abord le datacenter le moins cher).

## Exécution

Prérequis : **JDK 21** et **Maven 3.9+**.

**1. Installer CloudSim 7.0.1 dans le dépôt Maven local** (à faire une seule fois, depuis la racine du dépôt).
Cette étape est obligatoire : CloudSim 7.0.1 n'est pas publié sur Maven Central, et `TP3/cloudsim7` en dépend.

```bash
mvn clean install -DskipTests
```

**2. Partie 1 — CloudSim 7.0.1**

```bash
cd TP3/cloudsim7
mvn -q compile exec:java -Dexec.mainClass=tp3.Ex1Costs
mvn -q compile exec:java -Dexec.mainClass=tp3.Ex2Heterogeneous
mvn -q compile exec:java -Dexec.mainClass=tp3.Ex3MultiDatacenters
mvn -q compile exec:java -Dexec.mainClass=tp3.Ex4Schedulers
```

**3. Partie 2 — CloudSim Plus 8.5.7** (téléchargé automatiquement depuis Maven Central)

```bash
cd TP3/cloudsimplus
mvn -q compile exec:java -Dexec.mainClass=tp3plus.BasicFirstExample
mvn -q compile exec:java -Dexec.mainClass=tp3plus.Ex5Heterogeneous
mvn -q compile exec:java -Dexec.mainClass=tp3plus.Ex5CostsEx1
mvn -q compile exec:java -Dexec.mainClass=tp3plus.Ex6AllocationPolicies
mvn -q compile exec:java -Dexec.mainClass=tp3plus.Ex7Brokers
```

> Sous Windows, si les accents s'affichent mal dans la console, lancer d'abord `chcp 65001`.

## Résultats principaux

- **Ex. 1** — Dans CloudSim 7, `getProcessingCost()` ne compte que la bande passante (la partie CPU est commentée dans le code source), d'où 0 $ affiché. Le coût CPU réel est 2880 $, puis 5760 $ si le coût par seconde est doublé.
- **Ex. 2** — Makespan de 180 s avec l'affectation round-robin, 60 s avec LJF (−67 %).
- **Ex. 3** — Le broker par défaut remplit le premier datacenter sans regarder le prix. Un broker sensible au coût divise la facture par deux (1280 → 640 $), et LJF ramène le makespan de 80 à 60 s.
- **Ex. 4** — Les prédictions des diagrammes de Gantt sont confirmées. La seconde VM n'est créée qu'avec `VmSchedulerTimeShared` et des VMs de 500 MIPS.
- **Ex. 5** — 75 lignes en CloudSim Plus contre environ 197 en CloudSim 7, pour des résultats identiques. Les modèles de coût diffèrent : facturation par cloudlet dans CloudSim 7, par VM dans CloudSim Plus.
- **Ex. 6** — Seule BestFit place les 8 VMs quand le datacenter est plein ; RoundRobin et Simple étalent la charge.
- **Ex. 7** — Simple : 162,75 s ; BestFit avec ressoumission de la file d'attente : 79,20 s ; mapper MCT personnalisé : 73,89 s.

Le détail de chaque exercice (configuration, tableaux, analyses) se trouve dans le rapport.

---

*Le reste de ce fichier est le README original de CloudSim.*

# CloudSim: A Framework For Modeling And Simulation Of Cloud Computing Infrastructures And Services #

Cloud Computing is the leading approach for delivering reliable, secure, fault-tolerant, sustainable, and scalable computational services. Hence timely, repeatable, and controllable methodologies for performance evaluation of new cloud applications and policies before their actual development are required. Because utilization of real testbeds limits the experiments to the scale of the testbed and makes the reproduction of results an extremely difficult undertaking, simulation may be used.

CloudSim's goal is to provide a generalized and extensible simulation framework that enables modeling, simulation, and experimentation of emerging Cloud Computing infrastructures and application services, allowing its users to focus on specific system design issues that they want to investigate, without getting concerned about the low level details related to Cloud-Based infrastructures and services.

CloudSim is developed in [the Cloud Computing and Distributed Systems (CLOUDS) Laboratory](http://cloudbus.org/), at [the Computer Science and Software Engineering Department](http://www.csse.unimelb.edu.au/) of [the University of Melbourne](http://www.unimelb.edu.au/).

More information can be found on the [CloudSim's web site](http://cloudbus.org/cloudsim/).


# Main features #

  * Support for modeling and simulation of large scale Cloud Computing data centers
  * Support for modeling and simulation of virtualized server hosts, with customizable policies for provisioning host resources to Virtual Machines
  * Support for modeling and simulation of application containers
  * Support for modeling and simulation of energy-aware computational resources
  * Support for modeling and simulation of data center network topologies and message-passing applications
  * Support for modeling and simulation of federated clouds
  * Support for dynamic insertion of simulation elements, stop and resume of simulation
  * Support for user-defined policies for allocation of hosts to Virtual Machines and policies for allocation of host resources to Virtual Machines


# Download #

Either clone the repository or download a [release](https://github.com/Cloudslab/cloudsim/releases). The release package contains all the source code, examples, jars, and API html files.

# Installation
**Windows**
1) Install Java JDK21 on your system from the [official website](https://www.oracle.com/in/java/technologies/downloads/#java21) as shown in [JDK installation instructions](https://docs.oracle.com/en/java/javase/23/install/overview-jdk-installation.html)
2) Install Maven as shown on the [official website](https://maven.apache.org/install.html)
4) Compile and Run tests using the command prompt:
  ```prompt
  mvn clean install
  ```
You will find the jars in `modules/cloudsim/target/cloudsim-$VERSION.jar` and `modules/cloudsim-examples/target/cloudsim-examples-$VERSION.jar`

5) Run an example (e.g., CloudSimExample1) in cloudsim-examples using the command prompt:
```prompt
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
```

**Linux**
  1) Install Java JDK21 on your system:
  - On Debian-based Linux & Windows WSL2: 
    ```bash
    sudo apt install openjdk-21-jdk
    ```
  - On Red Hat-based Linux:  
    ```bash  
    sudo yum install java-21-openjdk
    ```
  2) Set Java JDK21 as default: 
  - On Debian-based Linux & Windows WSL2:
    ```bash
    sudo update-java-alternatives --set java-1.21.0-openjdk-amd64
    ```
  - On Red Hat-based Linux: 
    ```bash
    sudo update-alternatives --config 'java'
    ```
  3) Install Maven as shown on the [Official Website](https://maven.apache.org/install.html)
  4) Compile and run tests:
  ```bash
    mvn clean install
  ```
  You will find the jars in `modules/cloudsim/target/cloudsim-$VERSION.jar` and `modules/cloudsim-examples/target/cloudsim-examples-$VERSION.jar`

  5) Run an example (e.g., CloudSimExample1) in cloudsim-examples using the terminal:
  ```bash
  mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
  ```

  **Suggestion:** Use an IDE such as IDEA Intellij to faciliate steps 4) and 5)

# Preferred Publication #
  * **Remo Andreoli, Jie Zhao, Tommaso Cucinotta, and Rajkumar Buyya, [CloudSim 7G: An Integrated Toolkit for Modeling and Simulation of Future Generation Cloud Computing Environments](https://onlinelibrary.wiley.com/doi/10.1002/spe.3413), Software: Practice and Experience, Volume 55, Issue 6, Pages: 1041-1058, ISSN: 0038-0644, Wiley Press, New York, USA, June 2025.**

    
# Publications (Legacy) #

  * Anton Beloglazov, and Rajkumar Buyya, [Optimal Online Deterministic Algorithms and Adaptive Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in Cloud Data Centers](http://beloglazov.info/papers/2012-optimal-algorithms-ccpe.pdf), Concurrency and Computation: Practice and Experience, Volume 24, Number 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012.
  * Saurabh Kumar Garg and Rajkumar Buyya, [NetworkCloudSim: Modelling Parallel Applications in Cloud Simulations](http://www.cloudbus.org/papers/NetworkCloudSim2011.pdf), Proceedings of the 4th IEEE/ACM International Conference on Utility and Cloud Computing (UCC 2011, IEEE CS Press, USA), Melbourne, Australia, December 5-7, 2011.
  * Rodrigo N. Calheiros, Rajiv Ranjan, Anton Beloglazov, Cesar A. F. De Rose, and Rajkumar Buyya, [CloudSim: A Toolkit for Modeling and Simulation of Cloud Computing Environments and Evaluation of Resource Provisioning Algorithms](http://www.buyya.com/papers/CloudSim2010.pdf), Software: Practice and Experience (SPE), Volume 41, Number 1, Pages: 23-50, ISSN: 0038-0644, Wiley Press, New York, USA, January, 2011.
  * Bhathiya Wickremasinghe, Rodrigo N. Calheiros, Rajkumar Buyya, [CloudAnalyst: A CloudSim-based Visual Modeller for Analysing Cloud Computing Environments and Applications](http://www.cloudbus.org/papers/CloudAnalyst-AINA2010.pdf), Proceedings of the 24th International Conference on Advanced Information Networking and Applications (AINA 2010), Perth, Australia, April 20-23, 2010.
  * Rajkumar Buyya, Rajiv Ranjan and Rodrigo N. Calheiros, [Modeling and Simulation of Scalable Cloud Computing Environments and the CloudSim Toolkit: Challenges and Opportunities](http://www.cloudbus.org/papers/CloudSim-HPCS2009.pdf), Proceedings of the 7th High Performance Computing and Simulation Conference (HPCS 2009, ISBN: 978-1-4244-4907-1, IEEE Press, New York, USA), Leipzig, Germany, June 21-24, 2009.




[![](http://www.cloudbus.org/logo/cloudbuslogo-v5a.png)](http://cloudbus.org/)
