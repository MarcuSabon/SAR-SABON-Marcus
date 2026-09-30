=============================================================================
COMMUNICATION CHANNEL/BROKER
=============================================================================


A. Bootstrap et BrokerManager
L'interface Bootstrap permet de démarrer le runtime et de créer les 
premiers courtiers et tâches.
- Composant : BootstrapImpl 
- Rôle : Singleton ou registre global thread-safe (via ConcurrentHashMap) 
  qui stocke avec un nom unique.
- Contrainte : Si un courtier distant n'existe pas lors d'un appel à connect, 
  la méthode doit retourner null.

B. Task
Une tâche associe un code à exécuter à un broker.
- Composant : Task
- Rôle : Encapsule un Thread et un Runnable.

C. Broker
Ils sont nommés de manière unique. 
Ils peuvent être partagés entre plusieurs tâches et doivent obligatoirement 
être thread-safe.
- Composant : Broker
- Rôle : Gérer les connexions entrantes (accept) et sortantes (connect).
- Structure : Utilise une ConcurrentHashMap<Integer, RendezVous> pour 
  associer les ports à un point d'attente de connexion.

D. Mécanisme de Rendez-vous
Il n'y a pas de préséance entre connect et accept ; il s'agit d'un 
rendez-vous symétrique où la première opération attend la seconde.
- Composant : RendezVous
- Rôle : Permet de bloquer le thread serveur ou client jusqu'à ce que 
  l'autre partie arrive. Utilise des mécanismes de synchronisation 
  (wait()/notify()). 

E. Channel et Buffers
Un canal est constitué de deux terminaisons connectées.
- Composant : Channel et CircularBuffer
- Rôle : Channel agit comme une façade exposant les méthodes de 
  lecture, écriture et déconnexion. Les flux de données croisés 
  sont gérés par deux instances de CircularBuffer partagées entre les deux tâches.

=============================================================================
 PROBLÈMES POTENTIELS ET SOLUTIONS DE DESIGN
=============================================================================

PROBLÈME 1 : Unicité de l'écoute sur un port
- Contexte : Une seule tâche peut écouter (accept) sur un port donné d'un broker donné. Différentes tâches sur différents brokers peuvent accepter sur le même numéro de port.
- Solution : Dans Broker, lors de l'appel à accept(port), il faut vérifier si le port est déjà en cours d'utilisation dans la table locale du broker. Si une autre tâche est déjà en attente sur ce RendezVous, une exception (ex: IllegalStateException) doit être levée pour empêcher un deuxième accès concurrent local.

PROBLÈME 2 : Concurrence locale sur un même point d'accès (Channel)
- Contexte : Les lectures ou écritures simultanées par différentes tâches sur la même terminaison d'un canal ne sont pas supportées. Cependant, deux tâches peuvent invoquer simultanément la lecture et l'écriture sur le même canal.
- Solution : Channel ne synchronise pas toute l'instance. La méthode read() délègue à un buffer de réception synchronisé de manière isolée, tandis que write() délègue à un buffer d'émission distinct. Cela permet la lecture et l'écriture simultanées sans verrouillage global. La responsabilité de ne pas faire plusieurs read simultanés incombe à l'utilisateur.

PROBLÈME 3 : Gestion asynchrone de la déconnexion et perte de données
- Contexte : Les octets en transit doivent pouvoir être lus par la partie distante avant que le canal ne paraisse déconnecté de ce côté. Un appel write bloqué doit se débloquer lors d'une déconnexion en abandonnant les octets restants. Un appel read bloqué doit retourner 0.
- Solution : 
  Chaque CircularBuffer possédera deux indicateurs : eof (fin de fichier/écrivain parti) et Disconnected.
  * Lors d'un appel disconnect() local :
    1. Le buffer sortant (local -> distant) reçoit le signal eof = true.
    2. Le buffer entrant (distant -> local) reçoit le signal Disconnected = true.
    3. Les threads bloqués dans un wait() sur ces buffers sont réveillés via notifyAll().
  * Impact sur read() : 
    Si un read est bloqué et qu'il est réveillé, il retourne 0 si eof est vrai et que le buffer est vide. Un canal déconnecté à distance permet les opérations de lecture localement jusqu'à ce que tous les octets écrits aient été lus.
  * Impact sur write() :
    Invoquer write sur un canal déconnecté supprime simplement les octets. Si Disconnected est vrai, write() se termine immédiatement (et abandonne les octets sans retourner d'erreur).

PROBLÈME 4 : Boucle d'attente active lors de l'écriture ou la lecture
- Contexte : Une opération d'écriture avec une valeur de retour null bloquera au lieu de provoquer une attente active (spinning).
- Solution : Le CircularBuffer utilisera un modèle strict de Producteur/Consommateur avec wait(). Si un appel à write() ne trouve pas de place, il appelle wait() sur l'objet buffer jusqu'à ce que read() consomme des octets et appelle notify(). Ainsi, l'attente est gérée par le planificateur du système d'exploitation et ne consomme pas de ressources CPU.
