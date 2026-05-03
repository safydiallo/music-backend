# Test des Endpoints API - Music App

Ce fichier décrit comment tester les endpoints d'authentification de l'application Music Backend.

## Prérequis
- L'application doit tourner sur `http://localhost:8080`.
- Utilisez un outil comme Postman, curl, ou un navigateur pour les requêtes.

## Endpoints à Tester

### 1. Enregistrer un utilisateur (Register)
- **Méthode** : `POST`
- **URL** : `http://localhost:8080/api/auth/register`
- **Headers** :
  - `Content-Type: application/json`
- **Corps JSON** :
  ```json
  {
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User",
    "role": "USER"
  }
  ```
- **Réponse attendue** : Message de succès (ex. : "Utilisateur enregistré avec succès. Vérifiez votre email.").

### 2. Se connecter (Login)
- **Méthode** : `POST`
- **URL** : `http://localhost:8080/api/auth/login`
- **Headers** :
  - `Content-Type: application/json`
- **Corps JSON** :
  ```json
  {
    "email": "test@example.com",
    "password": "password123"
  }
  ```
- **Réponse attendue** : Objet JSON avec `token` et `refreshToken`.

### 3. Vérifier l'email (Verify Email)
- **Méthode** : `GET`
- **URL** : `http://localhost:8080/api/auth/verify?token=<votre_token_jwt>`
  - Remplacez `<votre_token_jwt>` par le token du login.
- **Headers** : Aucun.
- **Corps** : Aucun.
- **Réponse attendue** : Message de confirmation.

## Exemple avec curl
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","fullName":"Test User","role":"USER"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Verify (remplacez TOKEN par le vrai token)
curl "http://localhost:8080/api/auth/verify?token=TOKEN"
```