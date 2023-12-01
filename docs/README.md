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
- Point de connexion : /api/v1/websocket

### Canaux de communication (point de vue du client) :

- /user/queue/errors : file de reception des erreurs provoqué par un message du client
- /user/queue/compositions : file de reception des informations générales (composition invitée supprimée, composition invitée devient collaborative, utilisateurs connectés à une composition)
- /topic/compositions.{compoId} : file de reception des informations propre à l'édition d'une composition
- /app/compositions.{compoId} : file d'envoie des ordre de modification d'une composition (changement de titre, d'indicateur collaborative, gestion des éléments)
