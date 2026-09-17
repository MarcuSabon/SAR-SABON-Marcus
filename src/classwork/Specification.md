Specification :


---

Broker -> permet d'établir des canaux de communication (Channel) avec d'autres tâches du système. Toute identifié par un nom.

Broker(String name) -> Initialise un nouveau gestionnaire de communication avec un nom d'id. Name est le nom unique du broker, il ne doit pas être null.

Channel accept(int port) -> écoute sur un port spé jusqu'à ce qu'une autre tâche demande à s'y connecter. Souvent une méthode bloquante.

Channel connect(String name, int port) -> Tente de se connecter sur un broker distant spécifié par son nom, sur un port précis.


---

Channel -> C'est un canal de communications qui va permettre de de lire écrire et se déconnecter.

int read(byte[] bytes, int offset, int length) -> lit une liste de byte, commence a un certain moment de la liste ( offset ) et lit pour un total de length byte. 
Cette method est sujet a des problèmes de gestion de parallélisme, il faudra alors gérer les synchronisation s'il y a un accès a des variables communes.
Renvoie le nombre d'octets réellement lus

int write(byte[] bytes, int offset, int length) -> écris dans une liste de byte, à partir d'un certain offset, et écris length byte dans la liste. C'est aussi sujet a des problèmes de parallélisme.
Renvoie le nombre d'octets réellement écrits.

void disconnect() -> Ferme le canal de communication. Toute tentative future de lecture ou d'écriture devra échouer. Si un thread était bloqué dans un read ou un write, il doit être débloqué.

boolean disconnected() -> Renvoie true si la méthode disconnect() a été appelée, false sinon.

---

Task -> Son rôle principal est d'associer un code à exécuter avec un gestionnaire de communication. 

Task(Broker b, Runnable r) -> Crée un nouveau thread qui exécutera le code défini dans le Runnable r. Associe cette tâche au Broker b.


static Broker getBroker() -> Elle renvoyer le Broker associé au thread actuellement en cours d'exécution.