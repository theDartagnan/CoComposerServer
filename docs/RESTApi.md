# CoComposer Server : détail de l'API REST

## Rappel des caractéristiques :

- Gestion de l'authentification par cookie de session (nom prévu : COCOMPOSERSE)
- Gestion de la protection CSRF par double submit cookie 
  - nom de cookie : XSRF-TOKEN
  - nom d'en-tête HTTP attendu sur requêtes protégées : X-XSRF-TOKEN
  - requêtes protégées : toutes les requêtes POST, PUT, PATCH, DELETE
- Contrôle du type de données transmises par requête : strict, requirerd application/json

## Authentification et sécurité

### [POST] /api/login

Authentifie l'utilisateur. Peut être invoquée en étant déjà authentifié, auquel cas une nouvelle session est créée.

#### Requête

- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "username": le mail de l'utilisateur. String. Contrainte : format mail valide, min. 1 car., max 100 car.
  "password": mot de passe. String. Contrainte : 8 à 100 car. parmis a-z a-Z 0-9 _ - ; : % . * # < > $ ? + - 
}
```

#### Status possibles

- 302 : authentification réussi, redirection vers /api/v1/rest/accounts/myself (le client doit suivre cette redirection et récupérer le profil utilisateur)
- 400 : Bad request, données invalides
- 401 : Unauthorized : identifiants invalides
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

- la réponse étant une redirection, il n'y a pas de cors de réponse
- en-tête attendus :
  - Set-Cookie : &lt; le cookie de session &gt;

### [POST] POST /api/logout : désauthentification [AUTH]

Désauthentifie l'utilisateur. L'utilisateur doit être authentifié

#### Requête

- en-têtes HTTP attendus :
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête : aucun

#### Status possibles

- 204 : désauthentification réussi.
- 401 : Unauthorized : cookie de session manquant ou invalide
- 403 : Forbidden : token XSRF manquant ou invalide
- 500 : Internal Server Error

#### Réponse en cas de succès

- la réponse étant une 204 no-content, il n'y a pas de cors de réponse
- en-tête attendus :
  - Set-Cookie : &lt; le cookie de session, ivalidée &gt;

### [GET] /api/v1/rest/csrf

Accès au jeton chiffré CSRF  (utilisé uniquement pour l'initialisatio de la connexion Websocket). L'utilisateur doit être authentifié

#### Status possibles

- 200 : Ok.
- 401 : Unauthorized : cookie de session manquant ou invalide
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "headerName": nom du header à utiliser. String.
  "token": valeur du token. String.
}
```

## Gestion de comptes utilisateur

### [GET] /api/v1/rest/accounts/myself

Profil de l'utilisateur courant authentifié. L'utilisateur doit être authentifié.

#### Status possibles

- 200 : Ok.
- 401 : Unauthorized : cookie de session manquant ou invalide
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant. String.
  "email": email. String.
  "lastname": nom de famille. String.
  "firstname": prénom. String.
  "admin": indicateur de rôle administrateur. booléen.
}
```

### [POST] /api/v1/rest/accounts

Création d'un compte utilisateur. L'utilisateur doit être anonyme.

#### Requête

- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "password": mot de passe. String. Contrainte : 8 à 100 car. parmis a-z a-Z 0-9 _ - ; : % . * # < > $ ? + - 
  "memberInfo": {
    "email": le mail de l'utilisateur. String. Contrainte : format mail valide, min. 1 car., max 100 car.
    "firstname": prénom. String. Contraint : non blanc, entre 1 et 100 car.
    "lastname": nom de famille. String. Contraint : non blanc, entre 1 et 100 car.
  }
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur déjà authentifié
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant. String.
  "email": email. String.
  "lastname": nom de famille. String.
  "firstname": prénom. String.
  "admin": indicateur de rôle administrateur. booléen.
}
```

### [PATCH] /api/v1/rest/accounts/{userId} : 

Modification partiel du compte utilisateur (sauf mot de passe). L'utilisateur doit être authentifié.

#### Requête

- paramètres d'url :
  - userId: identifiant de l'utilisateur. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "email": le mail de l'utilisateur. String. Contrainte : format mail valide, min. 1 car., max 100 car. Peut être nul si pas de modification.
  "firstname": prénom. String. Contraint : non blanc, entre 1 et 100 car. Peut être nul si pas de modification.
  "lastname": nom de famille. String. Contraint : non blanc, entre 1 et 100 car. Peut être nul si pas de modification.
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not Found : utilisateur inconnu
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant. String.
  "email": email. String.
  "lastname": nom de famille. String.
  "firstname": prénom. String.
  "admin": indicateur de rôle administrateur. booléen.
}
```

### [DELETE] /api/v1/rest/accounts/{userId} : 

Suppression d'un compte. L'utilisateur doit être authentifié. userId doit être l'identifiant de l'utilisateur courant si celui-ci n'est pas administrateur.

#### Requête

- paramètres d'url :
  - userId: identifiant de l'utilisateur. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête : aucun

#### Status possibles

- 204 : Ok, no content
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not Found : utilisateur inconnu
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

Pas de donnée renvoyée en cas de succès

### [PUT] /api/v1/rest/accounts/{userId}/password

Changement de mot de passe. L'utilisateur doit être authentifié. userId doit être l'identifiant de l'utilisateur courant si celui-ci n'est pas administrateur.

#### Requête

- paramètres d'url :
  - userId: identifiant de l'utilisateur. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "currentPassword": le mot de passe actuel. String. Contrainte : 8 à 100 car. parmis a-z a-Z 0-9 _ - ; : % . * # < > $ ? + - 
  "newPassword": le nouveau mot de passe. String. Contrainte : 8 à 100 car. parmis a-z a-Z 0-9 _ - ; : % . * # < > $ ? + - 
}
```

#### Status possibles

- 204 : Ok, no content
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié ou mot de passe courant invalide
- 404 : Not Found : utilisateur inconnu
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

Pas de donnée renvoyée en cas de succès

## Gestion des compositions

### [GET] /api/v1/rest/compositions 

Résumés des compositions de l'utilisateur courant (en tant que propriétaire et invité). L'utilisateur doit être authentifié.

#### Status possibles

- 200 : Ok
- 401 : Unauthorized : utilisateur non authentifié
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  ownedCompositions: [
    {
      "id": identifiant de la composition. String.
      "title": titre. String.
      "collaborative": composition collaborative. Booléen
      "updateDatetime": date de dernière modification. String (date format UTC)
    }, ...
  ],
  guestCompositions: [
    {
      "id": identifiant de la composition. String.
      "title": titre. String.
      "collaborative": composition collaborative. Booléen
      "updateDatetime": date de dernière modification. String (date format UTC)
    }, ...
  ]
}
```

### [POST] /api/v1/rest/compositions

Création d'un composition. L'utilisateur doit être authentifié. L'utilisateur sera obligatoirement le propriétaire de la composition.

#### Requête

- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "title": titre. String. Contrainte: non blanc, entre 5 et 150 car.
  "collaborative": composition collaborative. Booléen. Valeur par défaut: false
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant de la composition. String.
  "title": titre. String.
  "collaborative": composition collaborative. Booléen
  "updateDatetime": date de dernière modification. String (date format UTC),
  "elements": [] tableau vide à la création
  "owner": {"id", "email", "firstname", "lastname"}
  "guests": [] tableau vide à la création
}
```

### [GET] /api/v1/rest/compositions/{compoId}

Composition en détails (inclut ses éléments, le propriétaire et les invités). L'utilisateur doit être authentifié. Si l'utilisateur n'est pas le propriétaire, il sera automatiquement ajouté comme invité si cela n'est pas déjà le cas.

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule

#### Status possibles

- 200 : Ok
- 401 : Unauthorized : utilisateur non authentifié
- 403 : Forbidden : token XSRF manquant ou invalide
- 404 : Not found : composition inconnue
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant de la composition. String.
  "title": titre. String.
  "collaborative": composition collaborative. Booléen
  "updateDatetime": date de dernière modification. String (date format UTC),
  "elements": [
    {
      "id": identifiant de l'élément (unique au sein de la composition). String.
      "elementType" : type de l'élément. String.
      "x" : coordonnée en abscisse. Number (double). 
      "y" : coordonnée en ordonnée. Number (double). 
      "style": style de l'élément. String. Possiblement null
      ...extraProps : propriétés supplémentaires possible. Toutes les valeurs sont des String pour celles-ci.
    }, ...
  ]
  "owner": {"id", "email", "firstname", "lastname"}
  "guests": [
    {"id", "email", "firstname", "lastname"},
    ...
  ]
}
```

### [PATCH] /api/v1/rest/compositions/{compoId} : 

Modification exclusive du titre ou du status collaboratif d'une composition (l'un ou l'autre mais pas les deux à la fois). L'utilisateur doit être authentifié. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action. Seule une composition personnelle peut être modifiée par ce service REST.

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "title": titre. String. Contrainte: non blanc, entre 5 et 150 car.
}

OU

{
  "collaborative": composition collaborative. Booléen. Valeur par défaut: false
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not Found : composition inconnue
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "title": titre. String.
}

OU

{
  "collaborative": composition collaborative.
}
```

### [DELETE] /api/v1/rest/compositions/{compoId} : 

Suppression d'une composition. L'utilisateur doit être authentifié. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action.

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête : aucun

#### Status possibles

- 204 : Ok, no content
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not Found : composition inconnue
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

Pas de donnée renvoyée en cas de succès

## Gestion des éléments d'une composition

### [POST] /api/v1/rest/compositions/{compoId}/elements 

Création d'un élément d'une composition. L'utilisateur doit être authentifié. Seule une composition personnelle peut être modifiée par ce service REST. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action (la composition devant être personnelle).

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "elementType" : type de l'élément. String. Contrainte : non blanc
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
  "style": style de l'élément. String. Possiblement null
  ...extraProps : propriétés supplémentaires possible.
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not found. Composition inconnue.
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant de l'élément (unique au sein de la composition). String.
  "elementType" : type de l'élément. String.
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
  "style": style de l'élément. String. Possiblement null
  ...extraProps : propriétés supplémentaires possible. Toutes les valeurs sont des String pour celles-ci.
}
```

### [PUT] /api/v1/rest/compositions/{compoId}/elements/{elemId} 

Modification d'un élément d'une composition (tout attribut). L'utilisateur doit être authentifié. Seule une composition personnelle peut être modifiée par ce service REST. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action (la composition devant être personnelle).

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
  - elemId: identifiant de l'élément. Contraintes : 4 à 50 car a-z A-Z _  - #
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "id": identifiant de l'élément (unique au sein de la composition). String.
  "elementType" : type de l'élément. String. Contrainte : non blanc
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
  "style": style de l'élément. String. Possiblement null
  ...extraProps : propriétés supplémentaires possible.
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not found. Composition ou élément de composition inconnue.
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "id": identifiant de l'élément (unique au sein de la composition). String.
  "elementType" : type de l'élément. String.
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
  "style": style de l'élément. String. Possiblement null
  ...extraProps : propriétés supplémentaires possible. Toutes les valeurs sont des String pour celles-ci.
}
```

### [PUT] /api/v1/rest/compositions/{compoId}/elements/{elemId}/position 

Modification de la position d'un élément d'une composition. L'utilisateur doit être authentifié. Seule une composition personnelle peut être modifiée par ce service REST. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action (la composition devant être personnelle).

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
  - elemId: identifiant de l'élément. Contraintes : 4 à 50 car a-z A-Z _  - #
- en-têtes HTTP attendus :
  - Content-Type: application/json
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête :
```
{
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
}
```

#### Status possibles

- 200 : Ok
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not found. Composition ou élément de composition inconnue.
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

```
{
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
}
```

### [DELETE] /api/v1/rest/compositions/{compoId}/elements/{elemId}

Suppression d'un élément d'une composition. L'utilisateur doit être authentifié. Seule une composition personnelle peut être modifiée par ce service REST. Si l'utilisateur n'est pas administrateur, seul le propriétaire de la composition peut effectuer cette action (la composition devant être personnelle).

#### Requête

- paramètres d'url :
  - compoId: identifiant de la composition. Contraintes : 24 car hexadécimaux en minuscule
  - elemId: identifiant de l'élément. Contraintes : 4 à 50 car a-z A-Z _  - #
- en-têtes HTTP attendus :
  - X-XSRF-TOKEN: &lt; valeur du jeton CSRF présent dans le cookie XSRF-TOKEN &gt;
- corps de requête : aucun

#### Status possibles

- 204 : Ok, no content
- 400 : Bad request, données invalides
- 401 : Unauthorized : utilisateur non authentifié
- 404 : Not Found : composition inconnue
- 403 : Forbidden : token XSRF manquant ou invalide
- 415 : Unsuported Media Type, en-tête Content-Type invalide ou données non JSON
- 500 : Internal Server Error

#### Réponse en cas de succès

Pas de donnée renvoyée en cas de succès