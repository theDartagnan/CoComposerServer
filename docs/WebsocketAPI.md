# CoComposer Server : détail de l'API Websocket

L'API websocket est reservée à l'utilisateur authentifié. Elle est utilisée pour La modification et la gestion d'élément de compositions collaborative, ainsi que pour la notification à l'utilisateur invité à une composition de son changement de nature (personnelle ou collaborative), sa suppression, et l'arrivée ou le départ d'autre éditeurs de cette dernière.

## Caractéristiques

- Type de connexion : STOMP over SockJS
- Authentification préalable obligatoire
- Protection CSRF par injection du jeton CSFR chiffré dans en-tête STOMP (accessible par le service REST GET /api/v1/rest/csrf)
- Point de connexion : /api/v1/websocket

Exemple d'exploitation sur [/src/test/resources/webIntegration](../src/test/ressources/webIntegration)

## Canaux de communication (point de vue du client) :

- __/user/queue/errors__ : file de reception des erreurs provoqué par l'envoie d'un message invalide de l'utilisateur.
- __/user/queue/compositions__ : file de reception des informations générales (composition invitée supprimée, composition invitée devient collaborative, utilisateurs connectés à une composition)
- __/topic/compositions.{compoId}__ : topic de reception des informations propre à l'édition d'une composition collaborative d'identifiant _compoId_
- __/app/compositions.{compoId}__ : file d'envoie des ordre de modification d'une composition collaborative d'identifiant _compoId_ (changement de titre, d'indicateur collaborative, gestion des éléments)

## Principe de fonctionnement chez le client

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

## Format des messages échangés

### Ordres d'édition de composition collaborative et de la gestion de ses éléments

Ces messages sont envoyés par l'utilisateurs au travers de sa file __/app/compositions.{compoId}__ et retransmis à tous les utilisateurs éditeurs de la composition actuellement connectés __/topic/compositions.{compoId}__, enrichis par le serveur d'information sur la composition (_compositId_) l'auteur (_authorEmail_) de l'ordre et le temps de l'action (_orderDatetime_). Les messages se distingue par leur attribut _orderType_ dont les valeurs obligatoires sont données ci-dessous.

#### Changement de titre

Changement du titre de la composition. Seul l'auteur peut appliquer ce changelent.

```
{
  "orderType": "compositiontitleChanged",
  "title": titre. String. Contrainte: non blanc, entre 5 et 150 car.
}
```

#### Changement de status de la composition

Bascule du status de composition collaborative à personnelle. (la bascule inverse ne peut se faire qu'en appel REST).

```
{
  "orderType": "compositionCollaborativeChanged",
  "collaborative": indicateur de collaboration. booléen. Devrait toujours être false d'après la logique applicative.
}
```

#### Ajout d'un élément

Ajout d'un élément de composition. Une fois l'ordre reçue et traitée par le serveur, ce dernier se renvoyé avec l'élément effectivement créé (incluant son id).

```
{
  "orderType": "elementAdded",
  "element": {
    "elementType" : type de l'élément. String. Contrainte : non blanc
    "x" : coordonnée en abscisse. Number (double). 
    "y" : coordonnée en ordonnée. Number (double). 
    "style": style de l'élément. String. Possiblement null
    ...extraProps : propriétés supplémentaires possible.
  }
}
```



#### Modification d'un élément

Modification d'un élément de la composition.

```
{
  "orderType": "elementChanged",
  "element": {
    "id": identifiant de l'élément (unique au sein de la composition). String. Contrainte : 4 à 50 caractères parmis a-z A-Z 0-9 _ - #
    "elementType" : type de l'élément. String. Contrainte : non blanc
    "x" : coordonnée en abscisse. Number (double). 
    "y" : coordonnée en ordonnée. Number (double). 
    "style": style de l'élément. String. Possiblement null
    ...extraProps : propriétés supplémentaires possible.
  }
}
```

#### Modification de la position d'un élément

Modification de la position d'un élément de la composition.

```
{
  "orderType": "elementPositionChanged",
  "elementId": identifiant de l'élément. String. Contrainte : 4 à 50 caractères parmis a-z A-Z 0-9 _ - #
  "x" : coordonnée en abscisse. Number (double). 
  "y" : coordonnée en ordonnée. Number (double). 
}
```

#### Suppression d'un élément

Suppression d'un élément de la composition.

```
{
  "orderType": "elementDeleted",
  "elementId": identifiant de l'élément. String. Contrainte : 4 à 50 caractères parmis a-z A-Z 0-9 _ - #
}
```

### Notifications du système sur la file d'informations générales

Lorsque l'utilisateur est authentifié, il est attendu que ce dernier se soit connecter en websocket et ait souscrit, entre autre, au à la file __/user/queue/compositions__. À travers ce dernier peuvent lui être communiqués différentes informations relative aux compositions pour lesquelles il est invités ou encore aux utilisateurs éditant les mêmes compositions collaboratives que lui. Cette communique est unidirectionnelle, l'utilisateur ne pouvant que recevoir des message.
Les différents messages possibles sont décrits ci-dessous

#### Changement d'état.
Lorsqu'une composition passe de l'état personnel à l'état collaboratif, tous les invités actuellement connecté en websocket son avertis à travers ce canal du changement.

```
{
  "orderType": "compositionCollaborativeChanged",
  "compositionId": id de la composition
  "collaborative": indicateur de collaboration. booléen. sera true dans ce cas.
  "authorEmail": mail du propriétaire de la composition ici. String.
  "orderDatetime". Date de l'action. String (représentation au format UTC)
}
```

#### Suppression d'une composition

Lorsqu'une composition est supprimée par son propriétaire, tous les invités actuellement connecté en websocket sont avertis à travers ce canal de cette suppression.

```
{
  "orderType": "compositionDeleted",
  "compositionId": id de la composition
  "authorEmail": mail du propriétaire de la composition ici. String.
  "orderDatetime". Date de l'action. String (représentation au format UTC)
}
```

### Editeurs de compositions

Lorsqu'un utilisateur souscrit à un topic d'une composition collaborative (__/topic/compositions.{compoId}__), typiquement dans le cas où il ouvre l'éditeur de cette dernière, il reçoit du serveur un message sur sa file personnelle (__/user/queue/compositions__) lui mentionant quels sont les membres actuellement connectés à ce topic (i.e. : les éditeurs actuels de la composition). Ce message n'est envoyé qu'une fois par souscription à un topic d'une composition. Le message reçu est de cette forme :

```
{
  "compositionId": l'identifiant de la composition
  "orderType": "CONNECTED_MEMBERS",
  "users": [
    {
      "email": email de l'utilisateur,
      "id": identifiant de l'utilisateur
    },...
  ]
}
```

## Notification du système sur le topic d'une composition

Outre les ordres d'édition de composition collaborative et de la gestion de ses éléments, deux autres types de messages sont transmis sur le topic d'une composition collaborative par le système : l'arrivée et le départ d'éditeurs (i.e. : de membres invités à la composition ou du propriétaire). Le deux messages sont les suivant :

```
{
  "orderType": "MEMBER_JOINED",
  "email": email de l'utilisateur,
  "id": identifiant de l'utilisateur
}
```

```
{
  "orderType": "MEMBER_LEFT",
  "email": email de l'utilisateur,
  "id": identifiant de l'utilisateur
}
```

### Messages d'erreurs

Lorsqu'un utilisateur envoie un ordre invalide d'édition de composition collaborative et de la gestion de ses éléments (ex.: données invalide), le système lui renvoie un message d'erreur à travers sa file de réception des erreurs (__/user/queue/errors__). Ces messages ont le format suivant :

```
{
  "timestamp": temps de l'erreur, au format UTC. String.
  "error": nom d'erreur. String.
  "message": détails de l'erreur manifestement en anglais. String. Possiblement null.
}
```

