# CoComposer Server
Serveur d'API Rest et websocket de l'application CoComposer

## Présentation générale de l'API REST

### Caractéristiques :

- Gestion de l'authentification par cookie de session (nom prévu : COCOMPOSERSE)
- Gestion de la protection CSRF par double submit cookie 
  - nom de cookie : XSRF-TOKEN
  - nom d'en-tête HTTP attendu sur requêtes protégées : X-XSRF-TOKEN
  - requêtes protégées : toutes les requêtes POST, PUT, PATCH, DELETE
- Contrôle du type de données transmises par requête : strict, requirerd application/json

### Vue d'ensemble des services :

Propriétés indiquées :

- [ANON] : utilisateurs anonymes (non authentifiés) seulement
- [AUTH] : utilisateurs authentifié seulement

#### Authentification et sécurité

- POST /api/login : authentification
- POST /api/logout : désauthentification [AUTH]
- GET /api/v1/rest/csrf : accès au jeton ciffré CSRF (utilisé pour l'initialisatio de la connexion Websocket) [AUTH]

#### Gestion de comptes utilisateur

- GET /api/v1/rest/accounts/myself : profil de l'utilisateur courant authentifié [AUTH]
- POST /api/v1/rest/accounts : création d'un compte utilisateur [ANON]
- PATCH /api/v1/rest/accounts/{userId} : modification partiel du compte utilisateur (sauf mot de passe) [AUTH]
- DELETE /api/v1/rest/accounts/{userId} : suppression d'un compte [AUTH]
- PUT /api/v1/rest/accounts/{userId}/password: changement de mot de passe [AUTH]

#### Gestion des compositions

- GET /api/v1/rest/compositions : résumé des compositions de l'utilisateur courant (en tant que propriétaire et invité) [AUTH]
- POST /api/v1/rest/compositions : création d'un composition [AUTH]
- GET /api/v1/rest/compositions/{compoId} : composition en détails (inclut ses éléments, le propriétaire et les invités) [AUTH]
- PATCH /api/v1/rest/compositions/{compoId} : modification exclusive du titre ou du status collaboratif d'une composition [AUTH]
- DELETE /api/v1/rest/compositions/{compoId} : suppression d'une composition [AUTH]

#### Gestion des éléments d'une composition
- POST /api/v1/rest/compositions/{compoId}/elements : création d'un élément d'une composition [AUTH]
- PUT /api/v1/rest/compositions/{compoId}/elements/{elemId} : modification d'un élément d'une composition (tout attribut) [AUTH]
- PUT /api/v1/rest/compositions/{compoId}/elements/{elemId}/position : modification de la position d'un élément d'une composition [AUTH]
- DELETE /api/v1/rest/compositions/{compoId}/elements/{elemId} : suppression d'un élément d'une composition [AUTH]

## Présentation générale de l'API Weboscket

### Caractéristiques

- Type de connexion websocket : STOMP over SockJS
- Authentification préalable obligatoire
- Protection CSRF par injection du jeton CSFR chiffré dans en-tête STOMP (accessible par le service REST GET /api/v1/rest/csrf)

### Canaux de communication (point de vue du client) :

- /user/queue/errors : file de reception des erreurs provoqué par un message du client
- /user/queue/compositions : file de reception des informations générales (composition invitée supprimée, composition invitée devient collaborative, utilisateurs connectés à une composition)
- /topic/compositions.{compoId} : file de reception des informations propre à l'édition d'une composition
- /app/compositions.{compoId} : file d'envoie des ordre de modification d'une composition (changement de titre, d'indicateur collaborative, gestion des éléments)

### Principe de fonctionnement chez le client

- Après authentification de l'utilisateur :
  - une connexion websocket doit être établie
  - le client doit souscrire aux files /user/queue/errors et /user/queue/compositions
  - le client peut alors recevoir sur le canal /user/queue/compositions :
    - la suppression d'une composition pour laquelle il est invité
    - la bascule d'une composition pour laquelle il est invité de personnelle vers collaborative

- Après ouverture d'une composition, si celle-ci est collaborative :
  - une souscription au topic /topic/compositions.{compoId} (avec compoId l'identifiant de la composition)
  - sur ce topic le client peut recevoir :
    - les modifications faites par les autres (changement de titre, ajout / modification / changement de position / suppression d'un élément)
    - l'arrivée ou le départ d'autres utilisateurs (propriétaire et invité) sur l'éditeur de la composition
    - si l'utilisateur est invité :
      - la bascule de l'état collaboratif de la composition à l'état personnel. Dans ce cas, le client doit basculer l'éditeur en lecture seule, et retirer sa souscription du topic
  - l'utilisateur va recevoir sur la file /user/queue/compositions la liste des utilisateurs de la composition actuellement connectés à l'éditeur (i.e. qui ont acutellement souscrit au topic /topic/compositions.{compoId})
  - l'utilisateur ne doit plus utiliser les services REST pour informer des modifications qu'il fait sur la composition, mais par la websocket en envoyant des ordre sur le canal /app/compositions.{compoId}
  - sur la file /user/queue/compositions, les messages suivant doivent également provoquer des changements dans l'éditeur de l'utilisateur :
    - bascule de la composition de l'état personnel à collaboratif : si l'utilisateur est invité, l'éditeur devait être en mode lecture seule et doit basculer en mode écriture et souscrire de nouveau au topic
    - suppression de la composition par son propriétaire : l'utilisateur étant un invité, il doit retirer la souscription du topic et fermer l'éditeur


  




