# vSphere Proxy-API Manager

Ce projet est un microservice **Spring Boot 3.2.5** conçu pour servir de passerelle (Proxy REST) entre des systèmes tiers et une infrastructure de virtualisation **VMware vSphere (ESXi/vCenter)**. Il simplifie l'automatisation du cycle de vie des machines virtuelles et la découverte des ressources.

## 🚀 Fonctionnalités Clés

*   **Gestion Multi-Hôtes** : Enregistrement dynamique de plusieurs serveurs ESXi/vCenter via API.
*   **Inventaire & Discovery** : Extraction des données sur les Datastores, Réseaux (vSwitches, PortGroups), Templates et informations matérielles.
*   **Cycle de Vie des VMs** : Démarrage, arrêt, suppression et reconfiguration à chaud (CPU/RAM).
*   **Provisioning Automatisé** : Clonage de machines virtuelles à partir de templates avec stratégie de placement intelligent (choix du Datastore avec le plus d'espace libre).
*   **Sécurité** : Chiffrement AES des identifiants vSphere en base de données et gestion de TrustStore pour le SSL.
*   **Asynchronisme** : Exécution des tâches VMware lourdes en arrière-plan via `@Async` (CompletableFuture).

## 🏗 Architecture Technique

*   **Langage** : Java 17
*   **Framework** : Spring Boot 3.x
*   **API VMware** : YaviJava (Librairie SOAP performante)
*   **Base de données** : MySQL (pour la persistance des hôtes et des sessions)
*   **Documentation** : Swagger UI / OpenAPI 3
*   **Patterns** : Factory (configurations de VM), Strategy (sélection de stockage), DTO (isolation des modèles API).

## 🛠 Prérequis

*   **Java 17** ou supérieur.
*   **Maven 3.8+**.
*   **MySQL 8.0+** (ou un conteneur Docker).
*   Un accès réseau à un hôte **VMware ESXi** ou **vCenter**.

## ⚙️ Configuration

Éditez le fichier `src/main/resources/application.properties` ou utilisez des variables d'environnement :

```properties
# Configuration Base de Données
spring.datasource.url=jdbc:mysql://localhost:3306/vsphere_db?createDatabaseIfNotExist=true
spring.datasource.username=votre_user
spring.datasource.password=votre_password

# Sécurité (Clé de chiffrement AES de 16 caractères)
encryption.host.key=ma-cle-secrete-123

# (Optionnel) Hôte par défaut au démarrage
vsphere.ip=192.168.1.X
vsphere.username=root
vsphere.password=mon-mot-de-pazss
```

## 🏃 Démarrage et Exécution

### 1. Cloner le projet
```bash
git clone <url-du-repo>
cd demo
```

### 2. Compiler le projet
```bash
mvn clean install
```

### 3. Lancer l'application
```bash
mvn spring-boot:run
```

L'application sera disponible sur `http://localhost:8080`.

## 📖 Documentation de l'API (Swagger)

Une fois l'application lancée, accédez à l'interface interactive pour tester les endpoints :
👉 http://localhost:8080/swagger-ui/index.html

### Principaux Endpoints :
*   `POST /api/v1/vsphere/hosts` : Enregistrer un nouvel ESXi.
*   `GET /api/v1/vsphere/{ip}/vms` : Lister les VMs d'un hôte.
*   `POST /api/v1/vsphere/{ip}/vms/clone` : Lancer un clonage de VM.
*   `GET /api/v1/vsphere/{ip}/system` : Voir les détails matériels du serveur.

## 🐳 Dockerisation (Docker Compose)

Le projet utilise le fichier `compose.yaml` pour orchestrer l'API et sa base de données MySQL de manière automatisée.

### Exécution complète (API + Base de données)
```bash
docker compose up -d --build
```
*   L'API sera accessible sur : `http://localhost:8080`
*   La base de données sera exposée sur le port : `3306`

### Construction manuelle de l'image seule
```bash
docker build -t vsphere-proxy-api .
```

## 📊 Observabilité

L'application expose des indicateurs de santé via **Spring Boot Actuator**.
Vérifiez l'état des connexions vSphere sur :
`http://localhost:8080/actuator/health`

---
*Dernière mise à jour : 10 Avril 2026*
*Contact : Equipe NGCloud*