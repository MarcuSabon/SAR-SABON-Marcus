===========================================
Design Queues
===========================================

A. Tasks
Contrairement au modele oriente thread classique, les taches suivent ici une execution orientee evenement.
- Role : Chaque tache possede une boucle d'evenements interne qui depile et execute des objets executables soumis via la methode post.
- Cycle de vie : Une tache peut se terminer normalement avec la methode exit ou echouer avec fail. 
- Notification : Les auditeurs ecoutant le resultat d'une tache sont executes sur la tache qui a defini l'auditeur.

B. Brokers
Les Brokers permettent de lier les files de messages entre les taches.
Role : Gerer de maniere asynchrone les demandes d'association et de connexion.

C. Files de Messages
Les files sont bidirectionnelles, de point a point, orientees messages, et appartiennent au broker qui les a creees.
- Emission : L'envoi de messages est asynchrone. La plage d'octets envoyee appartient a la file jusqu'a ce que le Listener d'envoi soit notifie.
- Reception : Les messages sont recus via un Listener. Ce Listener est invoque sur la tache qui l'a defini.

===========================================
PROBLEMES POTENTIELS ET SOLUTIONS DE DESIGN
===========================================

PROBLEME 1 : Gestion de la memoire et propriete des donnees en transit
Contexte : L'emetteur confie la propriete d'une plage d'octets a la file lors de l'envoi. La file garde cette propriete jusqu'a ce qu'elle notifie l'auditeur d'envoi.
Solution : La file de messages doit encapsuler la requete d'envoi dans une structure contenant la reference du tableau, l'offset, la longueur et l'auditeur.
  Ce n'est qu'apres la livraison du message a l'auditeur distant que l'auditeur d'envoi local est notifie.
  Si la file est fermee et le message rejete, la propriete est rendue immediatement.

PROBLEME 2 : Executions asynchrones et affinite de tache
Contexte : Les retours d'appels de connexion, d'association et de reception de messages doivent etre executes sur des taches specifiques.
Solution : Chaque appel ne doit pas etre execute de maniere synchrone par le fil d'execution courant.
  Il doit etre encapsule et transmis a la methode post de la tache appropriee.
  Cela garantit que toutes les notifications s'executent dans le contexte de la bonne boucle d'evenements.

PROBLEME 3 : Gestion du cycle de vie des taches sur erreur
Contexte : Toute exception non interceptee dans un executable publie entraine l'echec de la tache.
Solution : La boucle d'evenements interne de la tache doit encapsuler l'appel d'execution dans un bloc d'interception d'erreur.
 En cas d'exception, la boucle doit appeler automatiquement la methode d'echec de la tache, arreter le traitement des evenements suivants, et notifier tous les Listeners enregistres en postant les notifications sur leurs taches respectives.

Cote fermeture locale : L'extremite fermee cesse de livrer les messages ou rejette tout message envoye, en rendant la propriete des plages d'octets des messages rejetes. Un signal de fermeture est envoye a l'autre extremite.

Cote reception distante : L'extremite en cours de fermeture continue de livrer les messages envoyes avant l'operation de fermeture. Tout message recemment envoye depuis cette extremite est rejete, et la propriete rendue. Une fois tous les anciens messages recus, l'extremite en cours de fermeture devient definitivement fermee.