# Plan d'Action : Microservice de Gestion vSphere (ESXi/vCenter)

## 🚀 Phase 1 : Refactorisation & Architecture MVCS
- [x] Initialiser le projet Spring Boot.
- [x] Externaliser les identifiants ESXi dans `application.properties`.
- [x] **Service Layer** : Finaliser le `VsphereService` avec une gestion de session robuste (Auto-reconnect & Session Validation).
- [x] **Connection Pooling** : Implémenter un mécanisme de cache pour la `ServiceInstance` afin d'éviter les `login/logout` à chaque requête. (Implémentation d'une instance unique avec reconnexion automatique)
- [x] **Modèle & DTO** : Créer des DTOs (ex: `VirtualMachineDTO`, `HostDTO`) pour ne pas exposer les objets complexes de YaviJava.
- [x] **Sécurité** : Remplacer l'option "Ignore SSL" par une gestion de `TrustStore` configurable.

## 🛠 Phase 2 : Développement du Provisioning Service
- [x] **Abstraction** : Support multi-hôtes dynamique (Ajouter/Supprimer ESXi via API).
- [x] **Discovery Engine** : Implémenter la recherche dynamique des ressources :
    - [x] Liste des Datastores (Capacité totale, espace libre, utilisé).
    - [x] Liste des PortGroups / VLANs (Réseaux).
    - [x] Liste des Adaptateurs Physiques (pNICs).
    - [x] Liste des Commutateurs Virtuels (vSwitches).
    - [x] Liste des Templates disponibles.
    - [x] Informations système de l'hôte (Matériel, Version).
- [x] **Provisioning Logic** :
    - [x] Implémenter le clonage de VM (Gestion asynchrone).
    - [x] Implémenter la reconfiguration à la volée (CPU/RAM).
    - [x] Asynchronisme : Utiliser `@Async` pour gérer les tâches VMware (Tasks) sans bloquer les appels API.

## 🏗 Phase 3 : Design Patterns & Robustesse
- [x] **Factory Pattern** : Créer une `VmConfigurationFactory` pour gérer les types d'instances (Small, Medium, Large).
- [x] **Strategy Pattern** : Implémenter une stratégie de placement (choix automatique du Datastore avec le plus d'espace).
- [x] **Global Exception Handling** : Utiliser `@ControllerAdvice` pour mapper les exceptions vSphere vers des réponses HTTP standardisées.
- [x] **Validation** : Ajouter des validations de contraintes sur les requêtes de création de VM.

## 📦 Phase 4 : DevOps & Observabilité
- [x] **Dockerisation** : Rédiger le `Dockerfile` optimisé pour Spring Boot. (Dépendance `spring-boot-docker-compose` temporairement retirée pour le démarrage de l'app)
- [x] **Health Checks** : Configurer `Spring Boot Actuator` pour monitorer l'application.
- [x] **Log Management** : Mise en place de Logback avec rotation et volumes Docker.
- [x] **Docker Compose** : Configurer l'environnement local avec la base de données MySQL pour `spring-session`.
- [x] **CI/CD** : Créer un pipeline simple pour builder et tester l'application automatiquement.

## 🌐 Phase 5 : API & Documentation
- [x] **REST Controller** : Développer le `VsphereController` pour exposer les endpoints de gestion.
- [x] **Documentation** : Intégrer `SpringDoc OpenAPI` (Swagger) pour tester les endpoints visuellement.

---
*Dernière mise à jour : 10 Avril 2026*
*Objectif : Automatisation complète du cycle de vie des ressources virtuelles.*